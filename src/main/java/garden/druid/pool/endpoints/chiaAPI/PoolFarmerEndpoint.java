package garden.druid.pool.endpoints.chiaAPI;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.servlet.http.*;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Body;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.FormVariable;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.POST;
import garden.druid.base.http.rest.annotation.PUT;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.logging.Logger;
import garden.druid.chia.crypt.bech32.Bech32;
import garden.druid.chia.crypt.bls_blst_bindings.BLS;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes96;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.concurrent.FarmerUpdater;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.AuthenticationPayload;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPostPayload;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPostRequest;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPutPayload;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPutRequest;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolError;

@WebServlet(value = { "/farmer" })
@RestPattern( uri = "/farmer")
public class PoolFarmerEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;
	private final static String GET_METHOD_NAME = "get_farmer";
	private static ConcurrentHashMap<Bytes32, Instant> farmerUpdateBlocked = new ConcurrentHashMap<Bytes32, Instant>();
	
	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object loadFarmer(
			@Request HttpServletRequest request, 
			@FormVariable(name="launcher_id") Bytes32 launcherID,
			@FormVariable(name="authentication_token") NativeUInt64 authenticationToken,
			@FormVariable(name="signature") Bytes96 signature) {
		try {
			if (!Pool.getInstance().validateAuthenticationToken(authenticationToken)) {
				Logger.getInstance().log(Level.WARNING, "Invalid Auth Token in get farmer: "  + authenticationToken);
				return PoolError.INVALID_AUTHENTICATION_TOKEN;
			}
			FarmerRecord record = PoolDAO.getFarmerRecord(launcherID);
			if (record != null) {
				AuthenticationPayload payload = new AuthenticationPayload(GET_METHOD_NAME, record.getLauncherId(), Bech32.decodePuzzleHash(Pool.getInstance().getPoolInfo().getPool_contract_puzzle_hash()), authenticationToken);
				boolean bResponse = BLS.verifySignature(record.getAuthenticationPublicKey(), payload.hash().getBytes(), signature);
				if (bResponse == false) {
					Logger.getInstance().log(Level.WARNING, "Failed to load Farmer: invalid sig");
					return PoolError.INVALID_SIGNATURE;
				}
				HashMap<String, Object> resp = new HashMap<String, Object>();
				resp.put("authentication_public_key", record.getAuthenticationPublicKey());
				resp.put("payout_instructions", record.getPayoutInstructions());
				resp.put("current_difficulty", record.getDifficulty());
				resp.put("current_points", record.getPoints());
				return resp;
			} else {
				return PoolError.FARMER_NOT_KNOWN;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFarmerEndpoint.doGet", e);
			return PoolError.REQUEST_FAILED;
		}
	}

	@POST
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.BODY)
	public Object doPost(@Request HttpServletRequest request, @Body String body) {
		try {
			FarmerPostRequest updateRequest = gson.fromJson(body, FarmerPostRequest.class);
			if (updateRequest != null && updateRequest.getPayload() != null) {
				FarmerPostPayload payload = updateRequest.getPayload();
				// check if existing farmer
				FarmerRecord record = PoolDAO.getFarmerRecord(payload.getLauncherId());
				if (record != null) {
					Logger.getInstance().log(Level.WARNING, "Failed to Add Farmer: Already Known");
					return PoolError.FARMER_ALREADY_KNOWN;
				}
				Future<FarmerRecord> fRecord = Pool.getInstance().addFarmerRecord(updateRequest);
				record = fRecord.get();
				if (record == null) {
					Logger.getInstance().log(Level.WARNING, "Failed to Add Farmer, failed to add to garden.druid.pool.database");
					return PoolError.REQUEST_FAILED;
				} else {
					HashMap<String, String> resp = new HashMap<String, String>();
					resp.put("welcome_message", "Welcome to the DruidGarden garden.druid.pool.Pool.");
					Logger.getInstance().log(Level.INFO, "Updated Farmer: " + payload.getLauncherId());
					return resp;
				}
			} else {
				Logger.getInstance().log(Level.WARNING, "Failed to Add Farmer, Invalid request");
				return PoolError.REQUEST_FAILED;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFarmerEndpoint.doPost", e);
			return PoolError.REQUEST_FAILED;
		}
	}
	
	@PUT
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.BODY)
	public Object doPut(@Request HttpServletRequest request, @Body String body) {
		try {
			FarmerPutRequest updateRequest = gson.fromJson(body, FarmerPutRequest.class);
			if (updateRequest != null && updateRequest.getPayload() != null) {
				FarmerPutPayload payload = updateRequest.getPayload();
				// First check if this launcher_id is currently blocked for farmer updates
				Instant lastUpdate = farmerUpdateBlocked.get(payload.getLauncherId());
				if (lastUpdate != null && Instant.now().compareTo(lastUpdate.plus(Pool.getInstance().getPoolSettings().getFarmer_update_cooldown_seconds(), ChronoUnit.SECONDS)) < 0) {
					Logger.getInstance().log(Level.WARNING, "Failed to update Farmer: too soon");
					PoolError error = PoolError.REQUEST_FAILED;
					error.setErrorMessage("Cannot update farmer yet.");
					return error;
				} else {
					FarmerRecord pRecord = PoolDAO.getFarmerRecord(payload.getLauncherId());
					if (pRecord == null) {
						Logger.getInstance().log(Level.WARNING, "Failed to update Farmer: not known");
						return PoolError.FARMER_NOT_KNOWN;
					}
					FarmerUpdater callable = new FarmerUpdater(Pool.getInstance(), updateRequest);
					Future<FarmerRecord> fRecord = Pool.getInstance().updateFarmerRecord(callable);
					FarmerRecord record = fRecord.get();
					if (record == null) {
						Logger.getInstance().log(Level.WARNING, "Failed to update Farmer: " + callable);
						return callable.getError();
					} else {
						HashMap<String, Boolean> resp = new HashMap<String, Boolean>();
						if (!pRecord.getAuthenticationPublicKey().equals(record.getAuthenticationPublicKey())) {
							resp.put("authentication_public_key", true);
						} else {
							resp.put("authentication_public_key", false);
						}
						if (!pRecord.getPayoutInstructions().equals(record.getPayoutInstructions())) {
							resp.put("payout_instructions", true);
						} else {
							resp.put("payout_instructions", false);
						}
						//We do not want to let the farmer self adjust difficulty. Disabled for now 
//						if (pRecord.getDifficulty().compareTo(record.getDifficulty()) != 0) {
//							resp.put("suggested_difficulty", true);
//						} else {
//							resp.put("suggested_difficulty", false);
//						}
						farmerUpdateBlocked.put(record.getLauncherId(), Instant.now());
						return resp;
					}
				}
			} else {
				Logger.getInstance().log(Level.WARNING, "Failed to Update Farmer, Invalid request");
				return PoolError.REQUEST_FAILED;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFarmerEndpoint.doPut", e);
			return PoolError.REQUEST_FAILED;
		}
	}
}
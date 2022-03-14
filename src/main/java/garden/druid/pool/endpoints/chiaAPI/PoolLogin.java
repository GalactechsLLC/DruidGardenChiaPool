package garden.druid.pool.endpoints.chiaAPI;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import garden.druid.base.http.auth.api.Authenticator;
import garden.druid.base.http.auth.api.User;
import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.auth.unified.UnifiedLogin;
import garden.druid.base.http.auth.unified.UnifiedUser;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.AuthenticatorCls;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.FormVariable;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.Response;
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
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.AuthenticationPayload;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolError;

@WebServlet(value = { "/login" })
@RestPattern( uri = "/login")
@RequireAuthenticator( authenticator = "garden.druid.base.http.auth.unified.UnifiedAuthenticator")
@RequireUserLevel( level = UserLevel.NONE)
public class PoolLogin extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.VOID)
	@Consumes(consumer=ConsumerTypes.URL_PARAMS)
	public void login(
			@Request HttpServletRequest request, 
			@Response HttpServletResponse response, 
			@AuthenticatorCls Authenticator authenticator,
			@FormVariable(name="launcher_id") Bytes32 launcherID,
			@FormVariable(name="authentication_token") NativeUInt64 authenticationToken,
			@FormVariable(name="signature") Bytes96 signature
	) throws IOException {
		if (authenticationToken == null || !Pool.getInstance().validateAuthenticationToken(authenticationToken)) {
			Logger.getInstance().log(Level.WARNING, "Invalid Auth Token in login: " + authenticationToken);
			sendError(response, PoolError.INVALID_AUTHENTICATION_TOKEN);
		} else {
			FarmerRecord record = PoolDAO.getFarmerRecord(launcherID);
			if (record != null) {
				// Validate provided signature
				AuthenticationPayload payload = new AuthenticationPayload("get_login", record.getLauncherId(), Bech32.decodePuzzleHash(Pool.getInstance().getPoolInfo().getPool_contract_puzzle_hash()), authenticationToken);
				boolean bResponse = BLS.verifySignature(record.getAuthenticationPublicKey(), payload.hash().getBytes(), signature);
				if (bResponse == false) {
					Logger.getInstance().log(Level.WARNING, "Failed to login: invalid sig");
					sendError(response, PoolError.INVALID_SIGNATURE);
					return;
				} else {
					User user = authenticator.getUser(request);
					if(user == null) {
						user = UnifiedLogin.loginWithChia(launcherID.toString());
					} else {
						if(user instanceof UnifiedUser) {
							user = UnifiedLogin.linkWithChia((UnifiedUser)user, launcherID.toString());
						}
					}
					if(user != null) {
						request.getSession().setAttribute("user", user);
					}
					response.sendRedirect("/myaccount.html");
				}
			} else {
				sendError(response, PoolError.FARMER_NOT_KNOWN);
			}
		}
	}
	
	private void sendError(HttpServletResponse response, PoolError error) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(200);
		response.getWriter().write(gson.toJson(error));
	}
	
}

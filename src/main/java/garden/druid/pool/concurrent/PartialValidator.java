package garden.druid.pool.concurrent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.annotation.WebListener;

import garden.druid.base.cache.Cache;
import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedRunnable;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.SignagePointOrEOS;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.database.PoolPartialsDAO;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PartialStatus;
import garden.druid.pool.types.PendingPartial;
import garden.druid.pool.types.ValidatedSingletonState;

@WebListener
public class PartialValidator extends ManagedRunnable {

	private static final long serialVersionUID = -172089676501383826L;
	private transient boolean run = true;
	
	private transient static Cache<Bytes32, NativeUInt64> recentPointsAdded = new Cache<Bytes32, NativeUInt64>(4096);
	private transient static Cache<Bytes32, SignagePointOrEOS> spCache = new Cache<Bytes32, SignagePointOrEOS>(128);
	
	public PartialValidator() {
		this.uuid = UUID.randomUUID().toString();
		this.name = "PartialValidator";
	}

	public void run() {
		this.started = true;
		this.run = true;
		FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
		Connection con = null;
		PreparedStatement stmt = null;
		PreparedStatement ustmt = null;
		PreparedStatement u2stmt = null;
		PreparedStatement u3stmt = null;
		ResultSet rs = null;
		while(this.run) {
			this.status = "Running - Loading From DB";
			try {
				PendingPartial pendingPartial = PoolPartialsDAO.getPendingPartial();
				if(pendingPartial != null) {
					Bytes32 posHash = pendingPartial.getRequest().getPayload().getProofOfSpace().hash();
					this.status = "Running - Validating Partial";
					SignagePointOrEOS signageOrEOSResponse = spCache.get(pendingPartial.getRequest().getPayload().getSpHash());
					if (signageOrEOSResponse == null) {
						if (pendingPartial.getRequest().getPayload().isEndOfSubSlot()) {
							signageOrEOSResponse = client.get_recent_signage_point_or_eos(null, pendingPartial.getRequest().getPayload().getSpHash());
						} else {
							signageOrEOSResponse = client.get_recent_signage_point_or_eos(pendingPartial.getRequest().getPayload().getSpHash(), null);
						}
						if (signageOrEOSResponse == null || signageOrEOSResponse.isReverted() == true) {
							Logger.getInstance().log(Level.WARNING, "Partial EOS reverted: " + pendingPartial.getRequest().getPayload().getSpHash());
							PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.FAILED);
							continue;
						}
						spCache.put(pendingPartial.getRequest().getPayload().getSpHash(), signageOrEOSResponse);
					}
					// Now we know that the partial came on time, but also that the signage point/EOS is still in the blockchain. We need to check for double submissions.
					if (recentPointsAdded.get(posHash) != null) {
						Logger.getInstance().log(Level.WARNING, "Double signage point submitted for proof: " + posHash);
						PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.FAILED);
						continue;
					}
					recentPointsAdded.put(posHash, NativeUInt64.ONE);
					
					// Now we need to check to see that the singleton in the blockchain is still assigned to this pool
					FarmerRecord farmerRecord = PoolDAO.getFarmerRecord(pendingPartial.getRequest().getPayload().getLauncherId());
					ValidatedSingletonState vSingletonState = Pool.getInstance().getValidateSingletonState(pendingPartial.getRequest().getPayload().getLauncherId(), farmerRecord);
					if (vSingletonState == null) {
						Logger.getInstance().log(Level.WARNING, "Invalid singleton " + pendingPartial.getRequest().getPayload().getLauncherId());
						PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.FAILED);
						continue;
					}
		
					if (vSingletonState.isPoolMember() == false) {
						Logger.getInstance().log(Level.WARNING, "Singleton is not assigned to this pool: " + pendingPartial.getRequest().getPayload().getLauncherId());
						continue;
					}
		
					if (pendingPartial.getRequest().getPayload().getProofOfSpace().getPoolContractPuzzleHash() == null || farmerRecord.getP2SingletonPuzzleHash() == null || !pendingPartial.getRequest().getPayload().getProofOfSpace().getPoolContractPuzzleHash().equals(farmerRecord.getP2SingletonPuzzleHash())) {
						Logger.getInstance().log(Level.WARNING, "Error Validating partial: " + posHash + " , Mismatched Puzzle Hash: " + farmerRecord.getP2SingletonPuzzleHash().toString());
						PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.FAILED);
						continue;
					}
					if (farmerRecord.isPoolMember() == true) {
						this.status = "Running - Updating Farmer";
						PoolDAO.addFarmerPoints(farmerRecord.getLauncherId(), pendingPartial.getCurrentDifficulty());
						PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.VALID);
						PoolPartialsDAO.clearPartialJSON(posHash);
						Logger.getInstance().log(Level.INFO, "Farmer " + farmerRecord.getLauncherId() + " updated points to: " + PoolDAO.getFarmerRecord(farmerRecord.getLauncherId()).getPoints().toString());
					} else {
						PoolPartialsDAO.updatePartialStatus(posHash, PartialStatus.FAILED);
						Logger.getInstance().log(Level.WARNING, "Farmer " + farmerRecord.getLauncherId() + " failed to update, pool_member is false");
					}
				}
				this.status = "Sleeping";
				Thread.sleep(1000);
			} catch (SQLException e) {
				Logger.getInstance().log(Level.WARNING, "Error in PartialValidator.run", e);	
			}catch (InterruptedException e1) {
				this.run = false;
			} catch (Exception e) {
				Logger.getInstance().log(Level.WARNING, "Error in PartialValidator.run", e);	
			} finally {
				if (rs != null)	    try {rs.close();} catch (Exception e) {}
				if (stmt != null)	try {stmt.close();} catch (Exception e) {}
				if (ustmt != null)	try {ustmt.close();} catch (Exception e) {}
				if (u2stmt != null)	try {u2stmt.close();} catch (Exception e) {}
				if (u3stmt != null)	try {u3stmt.close();} catch (Exception e) {}
				if (con != null)    try {con.close();} catch (Exception e) {}
			}
		}
		this.done = true;
		this.status = "Exiting";
	}
}

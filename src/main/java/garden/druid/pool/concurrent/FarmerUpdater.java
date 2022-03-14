package garden.druid.pool.concurrent;

import java.util.UUID;
import java.util.logging.Level;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.crypt.bls_blst_bindings.BLS;
import garden.druid.chia.types.bytes.Bytes;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPutRequest;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolError;
import garden.druid.pool.types.ValidatedSingletonState;

public class FarmerUpdater extends ManagedCallable<FarmerRecord> {

	private transient static final long serialVersionUID = -9048935058242258067L;
	private transient final Pool pool;
	private transient final FarmerPutRequest request;
	private transient PoolError error = null;

	public FarmerUpdater(Pool pool, FarmerPutRequest updateRequest) {
		this.pool = pool;
		this.request = updateRequest;
		this.uuid = UUID.randomUUID().toString();
		this.name = "FarmerUpdater";
	}

	public FarmerRecord call() {
		this.started = true;
		try {
			ValidatedSingletonState vSingletonState = pool.getValidateSingletonState(request.getPayload().getLauncherId(), null);
			if (vSingletonState == null) {
				this.error = PoolError.INVALID_SINGLETON;
				this.error.setErrorMessage("Invalid singleton " + request.getPayload().getLauncherId());
				Logger.getInstance().log(Level.WARNING, "Error Updating Farmer: " + this.error.getErrorMessage());
				return null;
			}
	
			if (vSingletonState.isPoolMember() == false) {
				this.error = PoolError.INVALID_SINGLETON;
				this.error.setErrorMessage("Singleton is not assigned to this pool");
				Logger.getInstance().log(Level.WARNING, "Error Updating Farmer: " + this.error.getErrorMessage());
				return null;
			}
			boolean bResponse = BLS.verifySignature(vSingletonState.getBuriedSingletonTipState().getOwnerPubkey(), request.getPayload().hash().getBytes(), request.getSignature());
			if (bResponse == false) {
				this.error = PoolError.INVALID_SIGNATURE;
				Logger.getInstance().log(Level.WARNING, "Error Updating Farmer: " + this.error.getErrorMessage());
				return null;
			}
	
			FarmerRecord pRecord = PoolDAO.getFarmerRecord(request.getPayload().getLauncherId());
			boolean newValue = false;
			if (request.getPayload().getAuthenticationPublicKey() != null) {
				if (!pRecord.getAuthenticationPublicKey().equals(request.getPayload().getAuthenticationPublicKey())) {
					pRecord.setAuthenticationPublicKey(request.getPayload().getAuthenticationPublicKey());
					newValue = true;
				}
			}
			if (request.getPayload().getPayoutInstructions() != null) {
				if (!pRecord.getPayoutInstructions().equals(request.getPayload().getPayoutInstructions()) && Bytes.parseHexBinary(request.getPayload().getPayoutInstructions()).length == 32) {
					pRecord.setPayoutInstructions(request.getPayload().getPayoutInstructions());
					newValue = true;
				}
			}
			//For Now we are now allowing farmers to manually change.
//			if (request.getPayload().getSuggested_difficulty() != null) {
//				if (pRecord.getDifficulty().compareTo(request.getPayload().getSuggested_difficulty()) != 0 && request.getPayload().getSuggested_difficulty().compareTo(garden.druid.pool.Pool.getInstance().getPoolInfo().getMin_difficulty()) >= 0) {
//					pRecord.setDifficulty(request.getPayload().getSuggested_difficulty());
//					newValue = true;
//				}
//			}
			if (newValue) {
				PoolDAO.updateFarmerRecord(pRecord);
				Logger.getInstance().log(Level.INFO, "Updated Farmer: " + pRecord.getLauncherId());
			}
			return pRecord;
		} finally {
			this.done = true;
		}
	}
	
	public PoolError getError() {
		return this.error;
	}
}
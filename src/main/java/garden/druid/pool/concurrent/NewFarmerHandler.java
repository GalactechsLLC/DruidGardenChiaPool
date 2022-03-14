package garden.druid.pool.concurrent;

import java.util.UUID;
import java.util.logging.Level;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.clvmwrapper.Puzzles;
import garden.druid.chia.crypt.bls_blst_bindings.BLS;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.CoinRecord;
import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.blockchain.DelayedPuzInfo;
import garden.druid.chia.types.bytes.Bytes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPostRequest;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolError;
import garden.druid.pool.types.ValidatedSingletonState;

public class NewFarmerHandler extends ManagedCallable<FarmerRecord> {

	private static final long serialVersionUID = 6102807421340782088L;
	private transient final Pool pool;
	private transient final FarmerPostRequest request;
	private transient PoolError error = null;

	public NewFarmerHandler(Pool pool, FarmerPostRequest request) {
		this.pool = pool;
		this.request = request;
		this.uuid = UUID.randomUUID().toString();
		this.name = "NewFarmerHandler";
	}

	public FarmerRecord call() {
		this.started = true;
		try {
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			ValidatedSingletonState state = pool.getValidateSingletonState(request.getPayload().getLauncherId(), null);
			if (state == null) {
				this.error = PoolError.INVALID_SINGLETON;
				this.error.setErrorMessage("Invalid singleton " + request.getPayload().getLauncherId());
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			if (state.isPoolMember() == false) {
				this.error = PoolError.INVALID_SINGLETON;
				this.error.setErrorMessage("Singleton is not assigned to this pool");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			NativeUInt64 difficulty = pool.getPoolInfo().getDefault_difficulty();
			if (Bytes.parseHexBinary(request.getPayload().getPayoutInstructions()).length != 32) {
				this.error = PoolError.INVALID_SINGLETON;
				this.error.setErrorMessage("Payout instructions must be an xch address for this pool.");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			boolean isSigValid = BLS.verifySignature(state.getBuriedSingletonTipState().getOwnerPubkey(), request.getPayload().hash().getBytes(), request.getSignature());
			if (isSigValid == false) {
				this.error = PoolError.INVALID_SIGNATURE;
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			CoinRecord launcherCoin = client.get_coin_record_by_name(request.getPayload().getLauncherId().toString());
			if (launcherCoin == null) {
				this.error = PoolError.REQUEST_FAILED;
				this.error.setErrorMessage("Unable to find coin record");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			if (!launcherCoin.isSpent()) {
				this.error = PoolError.REQUEST_FAILED;
				this.error.setErrorMessage("Coin not spent");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			CoinSpend launcherSolution = client.get_coin_spend(launcherCoin);
			if (launcherSolution == null) {
				this.error = PoolError.REQUEST_FAILED;
				this.error.setErrorMessage("Unable to find coin record");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			DelayedPuzInfo delayedPuzInfo = Puzzles.getDelayedPuzInfoFromLauncherSpend(launcherSolution);
			if (delayedPuzInfo.getDelayTime().compareTo(new NativeUInt64(3600)) < 0) {
				this.error = PoolError.DELAY_TOO_SHORT;
				this.error.setErrorMessage("Delay time too short, must be at least 1 hour");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			Bytes32 p2SingletonPuzzleHash = Puzzles.launcherIdToP2PuzzleHash(request.getPayload().getLauncherId(), delayedPuzInfo.getDelayTime(), delayedPuzInfo.getDelayPuzzleHash());
			if (p2SingletonPuzzleHash == null) {
				this.error = PoolError.REQUEST_FAILED;
				this.error.setErrorMessage("Unable get DelayedPuzInfo");
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			FarmerRecord farmerRecord = new FarmerRecord(
				request.getPayload().getLauncherId(), 
				p2SingletonPuzzleHash, 
				delayedPuzInfo.getDelayTime(), 
				delayedPuzInfo.getDelayPuzzleHash(), 
				request.getPayload().getAuthenticationPublicKey(), 
				state.getBuriedSingletonTip(), 
				state.getBuriedSingletonTipState(),
				NativeUInt64.ZERO, 
				NativeUInt64.ZERO, 
				difficulty, 
				request.getPayload().getPayoutInstructions(), 
				true
			);
			Pool.getInstance().getScan_p2_singleton_puzzle_hashes().add(p2SingletonPuzzleHash);
			if (PoolDAO.addFarmerRecord(farmerRecord)) {
				Logger.getInstance().log(Level.INFO, "New Farmer: " +request.getPayload().getLauncherId());
				return farmerRecord;
			} else {
				this.error = PoolError.REQUEST_FAILED;
				this.error.setErrorMessage("Failed to Save Farmer Record");
				Logger.getInstance().log(Level.SEVERE, this.error.getErrorMessage());
				return null;
			}
		} catch(Exception ex){
			Logger.getInstance().log(Level.SEVERE, "Error in call.addFarmerRecord", ex);
			return null;
		} finally {
			this.done = true;
		}
	}
	
	public PoolError getError() {
		return this.error;
	}
}
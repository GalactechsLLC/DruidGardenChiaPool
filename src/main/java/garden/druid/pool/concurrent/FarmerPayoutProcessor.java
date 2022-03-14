package garden.druid.pool.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.rpc.WalletRpcClient;
import garden.druid.chia.types.blockchain.TransactionRecord;
import garden.druid.chia.types.blockchain.WalletSync;
import garden.druid.chia.types.blockchain.PendingPayment;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.types.FarmerPayoutRecord;

public class FarmerPayoutProcessor extends ManagedCallable<Boolean>  {

	private static final long serialVersionUID = 8452560827740521443L;
	private static final Object payoutLock = new Object();
	
	public FarmerPayoutProcessor(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "FarmerPayoutProcessor";
	}
	
	@Override
	public Boolean call() {
		this.started = true;
		try {
			WalletRpcClient client = Pool.getInstance().getHotWalletClient();
			NativeUInt32 peakHeight = Pool.getInstance().getState().getPeak().getHeight();
			client.log_in_and_skip(Pool.getInstance().getPoolInfo().getHot_wallet_fingerprint());
			WalletSync walletSync = client.get_sync_status();
			if (!Pool.getInstance().getState().getSync().isSynced() || !walletSync.isSynced()) {
				Logger.getInstance().log(Level.WARNING, "Waiting for wallet/pool sync");
				return false;
			}
			synchronized(payoutLock){
				ArrayList<FarmerPayoutRecord> farmerPayouts = Pool.getInstance().getPending_farmer_payments().peek();
				if (farmerPayouts == null || farmerPayouts.size() <= 0) {
					Logger.getInstance().log(Level.INFO, "No Farmner Payments to Process");
					return false;
				}
	
				HashMap<String, NativeUInt64>  paymentMap = new HashMap<>();
				for(FarmerPayoutRecord farmerRecord : farmerPayouts) {
					if(paymentMap.containsKey(farmerRecord.getPayoutInstructions())) {
						paymentMap.put(farmerRecord.getPayoutInstructions(), paymentMap.get(farmerRecord.getPayoutInstructions()).add(farmerRecord.getFarmerRecord().getBalance()));
					} else {
						paymentMap.put(farmerRecord.getPayoutInstructions(),farmerRecord.getFarmerRecord().getBalance());
					}
				}
				ArrayList<PendingPayment> paymentTargets = new ArrayList<>();
				for(Entry<String, NativeUInt64> entry : paymentMap.entrySet()) {
					paymentTargets.add(new PendingPayment(entry.getKey(), entry.getValue()));
				}
				
				Logger.getInstance().log(Level.INFO, "Submitting a farmer payment: " + paymentTargets);
				NativeUInt64 blockchainFee = NativeUInt64.ZERO;
				TransactionRecord transaction;
				try {
					transaction = client.send_transaction_multi(Pool.getInstance().getPoolInfo().getHot_wallet_id(), paymentTargets, blockchainFee);
					if(transaction == null) {
						Logger.getInstance().log(Level.WARNING, "Transaction was null: error for farmer paymentTargets: " + paymentTargets);
						return false;
					}
				} catch (Exception e) {
					Logger.getInstance().log(Level.WARNING, "Error in FarmerPayoutProcessor.call", e);
					Pool.getInstance().getPending_farmer_payments().add(farmerPayouts);
					return false;
				}
				Logger.getInstance().log(Level.INFO, "Waiting for transaction to obtain " + Pool.getInstance().getPoolSettings().getConfirmation_security_threshold() + " confirmations");
				while (!transaction.isConfirmed() || !(peakHeight.longValue() - transaction.getConfirmedAtHeight().longValue() > Pool.getInstance().getPoolSettings().getConfirmation_security_threshold()) ) {
					transaction = client.get_transaction(Pool.getInstance().getPoolInfo().getHot_wallet_fingerprint(), transaction.getName());
					peakHeight = Pool.getInstance().getState().getPeak().getHeight();
					if (!transaction.isConfirmed()) {
						Logger.getInstance().log(Level.INFO, "Not confirmed. In mempool? " + transaction.isInMempool());
						Thread.sleep(5000);
					} else{
						Logger.getInstance().log(Level.INFO, "Confirmations: " + (peakHeight.longValue() - transaction.getConfirmedAtHeight().longValue()) + "/" + Pool.getInstance().getPoolSettings().getConfirmation_security_threshold());

						Thread.sleep(5000);
					}
				}
				Pool.getInstance().getPending_farmer_payments().remove(farmerPayouts);
				for (FarmerPayoutRecord farmerRecord : farmerPayouts) {
					PoolDAO.subFarmerBalance(farmerRecord.getFarmerRecord().getLauncherId(), farmerRecord.getFarmerRecord().getBalance());
				}
				// # TODO(pool): persist in DB
				Logger.getInstance().log(Level.INFO, "Successfully confirmed farmer payments " + paymentTargets);
			}
			return true;
		} catch (Exception e) {
			Logger.getInstance().log(Level.WARNING, "Error in PaymentProcessor.call", e);
			return false;
		} finally {
			this.done = true;
		}
	}
}

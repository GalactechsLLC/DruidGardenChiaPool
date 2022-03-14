package garden.druid.pool.concurrent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.annotation.WebListener;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.rpc.WalletRpcClient;
import garden.druid.chia.types.blockchain.TransactionRecord;
import garden.druid.chia.types.blockchain.WalletSync;
import garden.druid.chia.types.blockchain.PendingPayment;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;

@WebListener
public class CollectionProcessor extends ManagedCallable<Boolean>  {

	private static final long serialVersionUID = 8452560827740521443L;

	private static final Object threadLock = new Object(); //Used to insure only one collector ever runs at once. 
	
	public CollectionProcessor(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "PaymentProcessor";
	}
	
	@Override
	public Boolean call() {
		try {
			synchronized(threadLock) {
				this.started = true;
				WalletRpcClient client = Pool.getInstance().getContractWalletClient();
				NativeUInt32 peakHeight = Pool.getInstance().getState().getPeak().getHeight();
				client.log_in_and_skip(Pool.getInstance().getPoolInfo().getContract_wallet_fingerprint());
				WalletSync walletSync = client.get_sync_status();
				if (!Pool.getInstance().getState().getSync().isSynced() || !walletSync.isSynced()) {
					Logger.getInstance().log(Level.WARNING, "Waiting for wallet/pool sync");
					return false;
				}
				
				ArrayList<PendingPayment> paymentTargets = Pool.getInstance().getPending_Collections().peek();
				do {
					if (paymentTargets == null || paymentTargets.size() <= 0) {
						Logger.getInstance().log(Level.INFO, "No Payments to Process");
						return false;
					}
		
					Logger.getInstance().log(Level.INFO, "Submitting a payment: " + paymentTargets.toString());
					NativeUInt64 blockchainFee = Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee();
					TransactionRecord transaction = null;
					try {
						transaction = client.send_transaction_multi(Pool.getInstance().getPoolInfo().getContract_wallet_id(), paymentTargets, blockchainFee);
						if(transaction == null) {
							Logger.getInstance().log(Level.WARNING, "Transaction was null: error for payment_targets: " + paymentTargets);
							return false;
						} else {
							Logger.getInstance().log(Level.INFO, "Submited Transaction: " + transaction.getName());
						}
					} catch (Exception e) {
						Logger.getInstance().log(Level.WARNING, "Error in PaymentProcessor.call", e);
						Pool.getInstance().getPending_Collections().add(paymentTargets);
						return false;
					}
					Logger.getInstance().log(Level.INFO, "Waiting for transaction to obtain " + Pool.getInstance().getPoolSettings().getConfirmation_security_threshold() + " confirmations");
					while (!transaction.isConfirmed() || !(peakHeight.longValue() - transaction.getConfirmedAtHeight().longValue() > Pool.getInstance().getPoolSettings().getConfirmation_security_threshold()) ) {
						transaction = client.get_transaction(Pool.getInstance().getPoolInfo().getContract_wallet_fingerprint(), transaction.getName());
						peakHeight = Pool.getInstance().getState().getPeak().getHeight();
						if (!transaction.isConfirmed()) {
							Logger.getInstance().log(Level.INFO, "Not confirmed. In mempool? " + transaction.isInMempool());
							Thread.sleep(5000);
						} else{
							Logger.getInstance().log(Level.INFO, "Confirmations: " + (peakHeight.longValue() - transaction.getConfirmedAtHeight().longValue()) + "/" + Pool.getInstance().getPoolSettings().getConfirmation_security_threshold());
							Thread.sleep(5000);
						}
					}
					Pool.getInstance().getPending_Collections().remove(paymentTargets);
					// # TODO(pool): persist in DB
					Logger.getInstance().log(Level.INFO, "Successfully confirmed payments " + paymentTargets.toString());
				} while((paymentTargets = Pool.getInstance().getPending_Collections().peek()) != null);
				Logger.getInstance().log(Level.INFO, "Done Processing Payments");
				return true;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.WARNING, "Error in PaymentProcessor.call", e);
			return false;
		} finally {
			this.done = true;
		}
	}
}

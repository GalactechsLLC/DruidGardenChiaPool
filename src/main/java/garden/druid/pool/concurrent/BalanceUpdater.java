package garden.druid.pool.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.annotation.WebListener;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.crypt.bech32.Bech32;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.CoinRecord;
import garden.druid.chia.types.blockchain.PendingPayment;
import garden.druid.chia.types.ints.NativeUInt64;
import static garden.druid.chia.Constants.MAX_TRANSACTION_AMOUNT;

import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.types.FarmerPayoutIntructions;

@WebListener
public class BalanceUpdater extends ManagedCallable<Boolean> {
	
	private static final long serialVersionUID = 4399525545943171182L;

	public BalanceUpdater(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "BalanceUpdater";
	}
	
	@Override
	public Boolean call() {
		/*
		 * Calculates the points of each farmer, and splits the total funds received
		 * into coins for each farmer. Saves the transactions that we should make, to
		 * `amount_to_distribute`.
		 */
		this.started = true;
		try {
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			if (Pool.getInstance().getState().getSync().isSynced() == false) {
				Logger.getInstance().log(Level.WARNING, "Pending pool sync, waiting");
				return false;
			}

			Logger.getInstance().log(Level.INFO, "Starting to update farmer balances");

			CoinRecord[] coinRecords = client.get_coin_records_by_puzzle_hash(Bech32.decodePuzzleHash(Pool.getInstance().getPoolInfo().getPool_contract_puzzle_hash()), false, null, null);

			if (coinRecords.length == 0) {
				Logger.getInstance().log(Level.INFO, "No funds to distribute.");
				return false;
			} else {
				Logger.getInstance().log(Level.INFO, "Found " + coinRecords.length + " coins to distribute.");
			}

			NativeUInt64 amountToDistribute = NativeUInt64.ZERO;
			for (CoinRecord c : coinRecords) {
				amountToDistribute = amountToDistribute.add(c.getCoin().getAmount());
			}
			Logger.getInstance().log(Level.INFO, "Total amount to distribute pre fees:" + amountToDistribute);
			//Calculate fee for blockchain transaction based on fee set in pool settings 
			NativeUInt64 multiplier = NativeUInt64.ONE;
			if(amountToDistribute.sub(Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee()).compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
				NativeUInt64[] divAndRem = amountToDistribute.divMod(MAX_TRANSACTION_AMOUNT);
				NativeUInt64 divisor = divAndRem[0];
				NativeUInt64 remainder = divAndRem[1];
				multiplier = remainder.compareTo(NativeUInt64.ZERO) > 0 ? divisor.add(NativeUInt64.ONE) : divisor;
			}
			NativeUInt64 poolRewardsFee = multiplier.mul(Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee());
			Logger.getInstance().log(Level.INFO, "Total Transactions: " + multiplier + ", Total Fee: " + poolRewardsFee);
			
			if (amountToDistribute.sub(poolRewardsFee).compareTo(NativeUInt64.ZERO) <= 0) {
				Logger.getInstance().log(Level.SEVERE, "Amount to distribute is less than pool fee!!!");
				return false;
			}
			List<FarmerPayoutIntructions> payouts = PoolDAO.getFarmerPointsAndPayoutInstructions();
			NativeUInt64 totalPoints = NativeUInt64.ZERO;
			for(FarmerPayoutIntructions fpi : payouts) {
				totalPoints = totalPoints.add(fpi.getAmount());
			}
			NativeUInt64 amountAfterFee = amountToDistribute.sub(poolRewardsFee);
			Logger.getInstance().log(Level.INFO, "Total amount to distribute after fees:" + amountAfterFee + " mojo (" + new BigDecimal(amountAfterFee.longValue()).divide(BigDecimal.TEN.pow(12)).toPlainString() + " xch)");
			if (totalPoints.compareTo(NativeUInt64.ZERO) > 0 && amountAfterFee.compareTo(NativeUInt64.ZERO) > 0) {
				NativeUInt64 mojoPerPoint = amountAfterFee.divFloor(totalPoints);
				Logger.getInstance().log(Level.INFO, "Paying out " + mojoPerPoint.toString() + " mojo / point");
				for (FarmerPayoutIntructions fpi : payouts) {
					if (fpi.getAmount().compareTo(NativeUInt64.ZERO) > 0) {
						PoolDAO.saveFarmerPointsHistory(fpi.getLauncherId(), fpi.getAmount(), totalPoints, mojoPerPoint);
						PoolDAO.subFarmerPoints(fpi.getLauncherId(), fpi.getAmount());
						PoolDAO.addFarmerBalance(fpi.getLauncherId(), fpi.getAmount().mul(mojoPerPoint));
					}
				}
				//Move Funds to the Hot Wallet
				String txAddress = Bech32.decodePuzzleHash(Pool.getInstance().getPoolInfo().getPool_wallet_puzzle_hash()).toString();
				NativeUInt64 remaining = amountAfterFee.add(NativeUInt64.ZERO); //CLONE HACK 
				boolean doneProcessing = false;
				ArrayList<ArrayList<PendingPayment>> pendingPaymentWrapperList = new ArrayList<ArrayList<PendingPayment>>();				
				while(!doneProcessing) {
					PendingPayment poolPayment = null;
					ArrayList<PendingPayment> pendingPaymentList = new ArrayList<PendingPayment>();
					if(remaining.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
						poolPayment = new PendingPayment(txAddress , MAX_TRANSACTION_AMOUNT);
						pendingPaymentList.add(poolPayment);
						remaining = remaining.sub(MAX_TRANSACTION_AMOUNT);
					} else {
						if(remaining.compareTo(NativeUInt64.ZERO) > 0) {
							poolPayment = new PendingPayment(txAddress , remaining);
							pendingPaymentList.add(poolPayment);
						}
						doneProcessing = true;
					}
					if(pendingPaymentList.size() > 0) {
						pendingPaymentWrapperList.add(pendingPaymentList);
					}
				}
				Pool.getInstance().getPending_Collections().addAll(pendingPaymentWrapperList);
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			this.done = true;
		}
	}
}

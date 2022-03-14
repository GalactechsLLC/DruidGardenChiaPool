package garden.druid.pool.concurrent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.annotation.WebListener;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.types.FarmerPayoutRecord;
import garden.druid.pool.types.FarmerRecord;

@WebListener
public class FarmerPayoutCreator extends ManagedCallable<Boolean> {
	
	private static final long serialVersionUID = 4399525545943171182L;
	private static final Object payoutLock = new Object();
	
	public FarmerPayoutCreator(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "FarmerPayoutCreator";
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
			synchronized(payoutLock){
				if(Pool.getInstance().getPending_farmer_payments().size() > 0) {
					Logger.getInstance().log(Level.WARNING, "garden.druid.pool.Pool has pending payments to process, skipping payment creation");
					return false;
				}
				if (Pool.getInstance().getState().getSync().isSynced() == false) {
					Logger.getInstance().log(Level.WARNING, "Pending pool sync, farmer payout creator waiting");
					return false;
				}
				ArrayList<FarmerRecord> farmerRecords = PoolDAO.getFarmersForPayout(Pool.getInstance().getPoolSettings().getPool_payout_threshold());
				ArrayList<FarmerPayoutRecord> farmerPayouts = new ArrayList<>();
				
				for (FarmerRecord farmerRecord : farmerRecords) {
					FarmerPayoutRecord farmerPayment = new FarmerPayoutRecord(farmerRecord.getPayoutInstructions(), farmerRecord);
					farmerPayouts.add(farmerPayment);
				}
				Pool.getInstance().getPending_farmer_payments().add(farmerPayouts);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			this.done = true;
		}
	}
}
package garden.druid.pool.concurrent;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedRunnable;
import garden.druid.chia.Constants;
import garden.druid.chia.clvmwrapper.Puzzles;
import garden.druid.chia.crypt.bech32.Bech32;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.Coin;
import garden.druid.chia.types.blockchain.CoinRecord;
import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.blockchain.MempoolItem;
import garden.druid.chia.types.blockchain.SpendBundle;
import garden.druid.chia.types.blockchain.TXStatus;
import garden.druid.chia.types.blockchain.TransactionRecord;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.puzzles.PoolPuzzles;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolSingletonState;
import garden.druid.pool.types.SingletonState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.UUID;

import javax.servlet.annotation.WebListener;

import java.math.BigDecimal;

@WebListener
public class PoolRewardsCollector extends ManagedRunnable {

	private static final long serialVersionUID = 4644633507317334158L;

	public PoolRewardsCollector(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "PoolRewardsCollector";
	}
	
	@Override
	public void run() {
		this.started = true;
		try {
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			if (Pool.getInstance().getState().getSync().isSynced() == false) {
				return;
			}
			ArrayList<Bytes32> puzzleHashes = new ArrayList<Bytes32>(PoolDAO.getPayToSingletonPHS());
			if (puzzleHashes.size() == 0) {
				return;
			}
			NativeUInt32 peakHeight = Pool.getInstance().getState().getPeak().getHeight();

			// Only get puzzle hashes with a certain number of confirmations or more, to
			// avoid reorg issues
			CoinRecord[] coinRecords = client.get_coin_records_by_puzzle_hashes(puzzleHashes, false, Pool.getInstance().getPoolSettings().getScan_start_height(), peakHeight);
			Logger.getInstance().log(Level.INFO, "Scanning for block rewards from " + Pool.getInstance().getPoolSettings().getScan_start_height() + " to " + peakHeight.toString() + ". " + "Found: " + coinRecords.length);
			HashMap<Bytes32, NativeUInt64> phToAmounts = new HashMap<Bytes32, NativeUInt64>();
			HashMap<Bytes32, ArrayList<CoinRecord>> phToCoins = new HashMap<Bytes32, ArrayList<CoinRecord>>();
			NativeUInt64 notBuriedAmounts = NativeUInt64.ZERO;
			for (CoinRecord cr : coinRecords) {
				if (!cr.isCoinbase()) {
					Logger.getInstance().log(Level.WARNING, "Non coinbase coin: " + cr.getCoin().name() + ", ignoring");
					continue;
				}
				if (cr.getConfirmedBlockIndex().compareTo(peakHeight.sub(new NativeUInt32(Pool.getInstance().getPoolSettings().getConfirmation_security_threshold()))) > 0) {
					notBuriedAmounts = notBuriedAmounts.add(cr.getCoin().getAmount());
					continue;
				}
				if (phToAmounts.containsKey(cr.getCoin().getPuzzleHash()) == false) {
					phToAmounts.put(cr.getCoin().getPuzzleHash(), NativeUInt64.ZERO);
					phToCoins.put(cr.getCoin().getPuzzleHash(), new ArrayList<CoinRecord>());
				}
				phToAmounts.put(cr.getCoin().getPuzzleHash(), phToAmounts.get(cr.getCoin().getPuzzleHash()).add(cr.getCoin().getAmount()));
				phToCoins.get(cr.getCoin().getPuzzleHash()).add(cr);
			}
			// For each p2sph, get the FarmerRecords
			List<FarmerRecord> farmerRecords = PoolDAO.getFarmerRecordsForPayToSingletonPHS(phToAmounts.keySet());

			// For each singleton, create, submit, and save a claim transaction
			NativeUInt64 claimableAmounts = NativeUInt64.ZERO;
			NativeUInt64 notClaimableAmounts = NativeUInt64.ZERO;
			for (FarmerRecord rec : farmerRecords) {
				if (rec.isPoolMember()) {
					claimableAmounts = claimableAmounts.add(phToAmounts.get(rec.getP2SingletonPuzzleHash()));
				} else {
					notClaimableAmounts = notClaimableAmounts.add(phToAmounts.get(rec.getP2SingletonPuzzleHash()));
				}
			}
			if (coinRecords.length > 0) {
				Logger.getInstance().log(Level.INFO, "Claimable amount: " + claimableAmounts + " mojo (" + new BigDecimal(claimableAmounts.toBigInteger()).divide(BigDecimal.TEN.pow(12)).toPlainString() + " xch)");
				Logger.getInstance().log(Level.INFO, "Not claimable amount: " + notClaimableAmounts + " mojo (" + new BigDecimal(notClaimableAmounts.toBigInteger()).divide(BigDecimal.TEN.pow(12)).toPlainString() + " xch)");
				Logger.getInstance().log(Level.INFO, "Not buried amounts: " + notBuriedAmounts + " mojo (" + new BigDecimal(notBuriedAmounts.toBigInteger()).divide(BigDecimal.TEN.pow(12)).toPlainString() + " xch)");
			}
			for (FarmerRecord rec : farmerRecords) {
				if (rec.isPoolMember()) {
					Coin singletonTip = Puzzles.getMostRecentSingletonCoinFromCoinSolution(rec.getSingletonTip());
					if (singletonTip == null) {
						continue;
					}
					CoinRecord singletonCoinRecord = client.get_coin_record_by_name(singletonTip.name());
					if (singletonCoinRecord == null) {
						continue;
					}
					if (singletonCoinRecord.isSpent()) {
						Pool.getInstance().getValidateSingletonState(rec.getLauncherId(), rec);
						Logger.getInstance().log(Level.WARNING, "Singleton coin " + singletonCoinRecord.getCoin().name() + " is spent, will not claim rewards: Launcher: " + rec.getLauncherId().toString());
						continue;
					}
					//Create Fee Transaction
					SpendBundle feeSpendBundle = null;
					if(Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee().compareTo(NativeUInt64.ZERO) > 0) {
                        //address can be anything
						ArrayList<Coin> additions = new ArrayList<Coin>();
						Coin addition = new Coin();
						addition.setAmount(Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee()); // 0.00005 chia is min
						addition.setPuzzleHash(Bech32.decodePuzzleHash(Pool.getInstance().getPoolInfo().getPool_contract_puzzle_hash()));
						additions.add(addition);          
						TransactionRecord signed_transaction = Pool.getInstance().getContractWalletClient().create_signed_transaction(additions, null, Pool.getInstance().getPoolSettings().getCollect_pool_rewards_fee());
						feeSpendBundle = signed_transaction.getSpendBundle();
					} else {
                        feeSpendBundle = null;
					}
					SpendBundle spendBundle = createAbsorbTransaction(
							rec, 
							Pool.getInstance().getState().getPeak().getHeight(), 
							phToCoins.get(rec.getP2SingletonPuzzleHash()), 
							Pool.getInstance().getPoolSettings().isIs_testnet() ? Constants.TESTNET_GENESIS_CHALLENGE : Constants.GENESIS_CHALLENGE,
							feeSpendBundle
					);
					if (spendBundle == null) {
						continue;
					}
					HashMap<String, MempoolItem> items = client.get_all_mempool_items();
					boolean inMemPool = false;
					for(Entry<String,MempoolItem> item : items.entrySet()) {
						if(item.getValue().getSpendBundle().name().equals(spendBundle.name())) {
							Logger.getInstance().log(Level.INFO, "SpendBumdle Already Submitted(skipping): " + item.getKey());
							inMemPool = true;
							break;
						}
					}
					if(inMemPool){
						continue;
					}
					TXStatus pushTxResponse = client.push_tx(spendBundle);
					if (pushTxResponse != null && pushTxResponse.toString().equals("SUCCESS")) {
						Logger.getInstance().log(Level.INFO, "Submitted transaction successfully: " + spendBundle.name().toString());
					} else {
						Logger.getInstance().log(Level.WARNING, "Error submitting transaction: " + (pushTxResponse != null ? pushTxResponse.toString() : "null") + ", Spend bundle: " + spendBundle.name().toString());
					}
				}
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolRewardsCollector.run", e);
		} finally {
			this.done = true;
		}
	}

	private SpendBundle createAbsorbTransaction(FarmerRecord farmerRecord, NativeUInt32 peakHeight, ArrayList<CoinRecord> rewardCoinRecords, Bytes32 genesisChallenge, SpendBundle additionalSpendBundle) {
		SingletonState singletonStateTuple = Pool.getInstance().getSingletonState(farmerRecord.getLauncherId(), farmerRecord, peakHeight, 0);
		if (singletonStateTuple == null) {
			Logger.getInstance().log(Level.WARNING, "Invalid singleton " + farmerRecord.getLauncherId() + ".");
			return null;
		}
		// Here the buried state is equivalent to the latest state, because we use 0 as the security_threshold
		if (!singletonStateTuple.getSavedState().equals(singletonStateTuple.getLastNotNullState())) {
			return null;
		}

		if (singletonStateTuple.getSavedState().getState().intValue() == PoolSingletonState.SELF_POOLING.getValue()) {
			Logger.getInstance().log(Level.WARNING, "Don't try to absorb from former farmer " + farmerRecord.getLauncherId() + ".");
			return null;
		}

		CoinRecord launcherCoinRecord = Pool.getInstance().getFullNodeClient().get_coin_record_by_name(farmerRecord.getLauncherId().toString());
		if (launcherCoinRecord == null) {
			return null;
		}
		ArrayList<CoinSpend> allSpends = new ArrayList<CoinSpend>();
		CoinSpend lastSpend = singletonStateTuple.getSavedSolution();
		for (CoinRecord rewardCoinRecord : rewardCoinRecords) {
			NativeUInt32 foundBlockIndex = Pool.get_farmed_height(rewardCoinRecord);
			if (foundBlockIndex == null) {
				// # The puzzle does not allow spending coins that are not a coinbase reward
				Logger.getInstance().log(Level.WARNING, "Received reward " + rewardCoinRecord.getCoin().getPuzzleHash() + " that is not a pool reward.");
				continue;
			}
			if (rewardCoinRecord.isSpent()) {
				Logger.getInstance().log(Level.WARNING, "Reward is spent: " + rewardCoinRecord.getCoin().getPuzzleHash());
				continue;
			}
			// try{
			CoinSpend[] absorbSpend = PoolPuzzles.create_absorb_spend(lastSpend, singletonStateTuple.getSavedState(), launcherCoinRecord.getCoin(), foundBlockIndex, genesisChallenge, farmerRecord.getDelayTime(), farmerRecord.getDelayPuzzleHash());
			lastSpend = absorbSpend[0];
			allSpends.addAll(Arrays.asList(absorbSpend));
			if (allSpends.size() >= 190) {
				break;
			}

		}
		if (allSpends.size() == 0) {
			return null;
		}
		SpendBundle spendBundle = new SpendBundle(allSpends);
		if(additionalSpendBundle != null) {
			spendBundle = SpendBundle.aggregate(spendBundle, additionalSpendBundle);
		}
		return new SpendBundle(allSpends);
	}
}

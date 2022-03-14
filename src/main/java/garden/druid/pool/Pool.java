package garden.druid.pool;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedExecutor;
import garden.druid.base.threads.threadpools.ManagedScheduler;
import garden.druid.base.threads.threadpools.ThreadPoolManager;
import garden.druid.chia.Constants;
import garden.druid.chia.clvmwrapper.Puzzles;
import garden.druid.chia.crypt.bech32.Bech32;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.rpc.WalletRpcClient;
import garden.druid.chia.types.blockchain.Coin;
import garden.druid.chia.types.blockchain.CoinRecord;
import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.blockchain.DelayedPuzInfo;
import garden.druid.chia.types.blockchain.State;
import garden.druid.chia.types.blockchain.PendingPayment;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt128;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.concurrent.FarmerUpdater;
import garden.druid.pool.concurrent.NewFarmerHandler;
import garden.druid.pool.concurrent.PartialHandler;
import garden.druid.pool.concurrent.PartialValidator;
import garden.druid.pool.concurrent.BalanceUpdater;
import garden.druid.pool.concurrent.BlockHistoryUpdater;
import garden.druid.pool.concurrent.CollectionProcessor;
import garden.druid.pool.concurrent.FarmerPayoutCreator;
import garden.druid.pool.concurrent.FarmerPayoutProcessor;
import garden.druid.pool.concurrent.PoolRewardsCollector;
import garden.druid.pool.concurrent.StateThread;
import garden.druid.pool.database.PoolAdminDAO;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.endpoints.chiaAPI.types.FarmerPostRequest;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialRequest;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialResponse;
import garden.druid.pool.puzzles.PoolPuzzles;
import garden.druid.pool.types.FarmerPayoutRecord;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolInfo;
import garden.druid.pool.types.PoolSettings;
import garden.druid.pool.types.PoolSingletonState;
import garden.druid.pool.types.PoolState;
import garden.druid.pool.types.SingletonState;
import garden.druid.pool.types.ValidatedSingletonState;

public class Pool implements ServletContextListener{

	public static final NativeUInt128 DIFFICULTY_CONSTANT_FACTOR = new NativeUInt128(BigInteger.valueOf(2l).pow(67));// MAINNET
	public static final NativeUInt128 TESTNET_DIFFICULTY_CONSTANT_FACTOR = new NativeUInt128(10052721566054l); //TESTNET
	public static final NativeUInt64 POOL_SUB_SLOT_ITERS = new NativeUInt64(37600000000l);
	public static final int POOL_PROTOCOL_VERSION = 1;
	// This number should be held constant and be consistent for every pool in the network. DO NOT CHANGE
	public static final NativeUInt64 iters_limit = new NativeUInt64(POOL_SUB_SLOT_ITERS.longValue() / 64);
	
	public static final int NUMBER_ZERO_BITS_PLOT_FILTER = 9;
	
	//The global pool instance that will be shared
	private static final Pool instance;
	
	//The thread pools to execute the various pool functions and keep the explorer and pool in sync with the blockchain
	private static final ManagedExecutor addThreadPool = ThreadPoolManager.newThreadPool("addFarmerPool", 1);
	private static final ManagedExecutor updateThreadPool = ThreadPoolManager.newThreadPool("updateFarmerPool", 1);
	private static final ManagedScheduler stateThreadPool = ThreadPoolManager.newScheduler("statePool", 1);
	//private static final ManagedScheduler explorerSyncThreadPool = ThreadPoolManager.newScheduler("explorerSyncPool", 1);
	private static final ManagedScheduler collectThreadPool = ThreadPoolManager.newScheduler("collectPool", 1); 
	private static final ManagedScheduler blockHistoryThreadPool = ThreadPoolManager.newScheduler("blockHistory", 1); 
	private static final ManagedExecutor balanceUpdaterThreadPool = ThreadPoolManager.newThreadPool("paymentsPool", 1);
	private static final ManagedExecutor collectionProcessorThreadPool = ThreadPoolManager.newThreadPool("paymentsPool", 1);
	private static final ManagedExecutor farmerPayoutThreadPool = ThreadPoolManager.newThreadPool("paymentsPool", 1);
	private static final ManagedExecutor partialConfirmThreadPool = ThreadPoolManager.newThreadPool("partialThreadPool", 1, 16, 400, TimeUnit.SECONDS);
	private static final ManagedExecutor partialThreadPool = ThreadPoolManager.newThreadPool("partialSubmitPool", 1, 24, 60, TimeUnit.SECONDS); 
	
	private PoolInfo poolInfo;
	private PoolSettings poolSettings;
	private HashSet<Bytes32> scan_p2_singleton_puzzle_hashes;
	private ConcurrentLinkedQueue<ArrayList<PendingPayment>> pending_collections = new ConcurrentLinkedQueue<ArrayList<PendingPayment>>();
	private ConcurrentLinkedQueue<ArrayList<FarmerPayoutRecord>> pending_farmer_payments = new ConcurrentLinkedQueue<ArrayList<FarmerPayoutRecord>>();
	private State state = null;
	private WalletRpcClient contractWalletClient = null;
	private WalletRpcClient hotWalletClient = null;
	private FullNodeRpcClient fullNodeClient = null;
	
	static {
		instance = new Pool();
	}

	public static Pool getInstance() {
		return instance;
	}

	private Pool() {
		//Load the pool info from the garden.druid.pool.database
		//TODO maybe add default values?
		this.poolInfo = PoolAdminDAO.loadPoolInfo();
		this.poolSettings = PoolAdminDAO.loadPoolSettings();
		//Init the RPCClients we will use to communicate with the fullnode. 
		this.fullNodeClient = new FullNodeRpcClient(this.poolInfo.getFullNodeHost(), this.poolInfo.getFull_node_port());
		this.hotWalletClient = new WalletRpcClient(this.poolInfo.getHotWalletHost(), this.poolInfo.getHot_wallet_port());
		this.contractWalletClient = new WalletRpcClient(this.poolInfo.getContractWalletHost(), this.poolInfo.getContract_wallet_port());
		//Load all current farmer puzzle hashes 
		scan_p2_singleton_puzzle_hashes = new HashSet<Bytes32>(PoolDAO.getPayToSingletonPHS());
		//Load the blockchain state and ensure we can connect to the full node
		State cState = fullNodeClient.get_blockchain_state();
		if (cState == null) {
			Logger.getInstance().log(Level.SEVERE, "Null State from Full Node");
		} else {
			this.state = cState;
			Logger.getInstance().log(Level.INFO, "Loaded Blockchain State Sucessfully");
		}
		//Log into the wallet and ensure the wallet finger print is correct
		String result = this.contractWalletClient.log_in_and_skip(this.poolInfo.getContract_wallet_fingerprint());
		if (result == null) {
			Logger.getInstance().log(Level.SEVERE, "Error logging in to contract wallet. Make sure your config fingerprint is correct.");
		} else {
			Logger.getInstance().log(Level.INFO, "Logged into garden.druid.pool.Pool Contract Wallet Successfully");
		}	
		result = this.hotWalletClient.log_in_and_skip(this.poolInfo.getHot_wallet_fingerprint());
		if (result == null) {
			Logger.getInstance().log(Level.SEVERE, "Error logging in to hot wallet. Make sure your config fingerprint is correct.");
		} else {
			Logger.getInstance().log(Level.INFO, "Logged into garden.druid.pool.Pool Hot Wallet Successfully");
		}	
		//Start the thread that will collect the pool rewards from the blockchain
		collectThreadPool.scheduleAtInterval(new PoolRewardsCollector(), 0, 60, TimeUnit.SECONDS);
		blockHistoryThreadPool.scheduleAtInterval(new BlockHistoryUpdater(), 0, 300, TimeUnit.SECONDS);
		//Start some partial confirm threads, the number of threads can be adjusted 
		for(int i = 0 ; i < 16; i ++) {
			partialConfirmThreadPool.submit(new PartialValidator());
		}
		//Start the thread that will keep the pools state synced with the fullnode
		stateThreadPool.scheduleAtInterval(new StateThread(), 5, 3, TimeUnit.SECONDS);
		//Start the thread that will keep the explorer synced with the blockchain state and update DB
		//explorerSyncThreadPool.scheduleAtInterval(new ExplorerSyncThread(), 10, 30, TimeUnit.SECONDS);
	}
	
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
	    Driver d = null;
	    while (drivers.hasMoreElements()) {
	        try {
	            d = drivers.nextElement();
	            DriverManager.deregisterDriver(d);
	            Logger.getInstance().log(Level.WARNING, String.format("Driver %s deregistered", d));
	        }
	        catch (SQLException ex) {
	        	Logger.getInstance().log(Level.WARNING, String.format("Error deregistering driver %s", d), ex);
	        }
	    }
	    addThreadPool.shutdown();
		updateThreadPool.shutdown();
		stateThreadPool.shutdown();
		//explorerSyncThreadPool.shutdown();
		collectThreadPool.shutdown();
		balanceUpdaterThreadPool.shutdown();
		collectionProcessorThreadPool.shutdown();
		farmerPayoutThreadPool.shutdown();
		partialConfirmThreadPool.shutdown();
		partialThreadPool.shutdown();
		try { Thread.sleep(2000L); } catch (Exception e) {}
		
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Pool.getInstance();
	}

	public Future<Boolean> runBalanceUpdater(){
		if(this.getPending_Collections().size() > 0) return new FutureTask<Boolean>(null, false){{set(false);done();}};
		return balanceUpdaterThreadPool.submit(new BalanceUpdater());
	}
	
	public Future<Boolean> runProcessCollections(){
		if(this.getPending_Collections().size() <= 0) return new FutureTask<Boolean>(null, false){{set(false);done();}};
		return collectionProcessorThreadPool.submit(new CollectionProcessor());
	}
	
	public Future<Boolean> runFarmerPayoutProcessor(){
		if(this.getPending_farmer_payments().size() <= 0) return new FutureTask<Boolean>(null, false){{set(false);done();}};
		return farmerPayoutThreadPool.submit(new FarmerPayoutProcessor());
	}
	
	public Future<Boolean> runFarmerPayoutCreator(){
		if(this.getPending_farmer_payments().size() > 0) return new FutureTask<Boolean>(null, false){{set(false);done();}};
		return farmerPayoutThreadPool.submit(new FarmerPayoutCreator());
	}
	
	public Future<FarmerRecord> addFarmerRecord(FarmerPostRequest request) {
		return addThreadPool.submit(new NewFarmerHandler(instance, request));
	}

	public Future<FarmerRecord> updateFarmerRecord(FarmerUpdater callable) {
		return updateThreadPool.submit(callable);
	}

	public Future<PostPartialResponse> submitPartial(PostPartialRequest request, FarmerRecord farmer_record, Instant start_time) {
		return partialThreadPool.submit(new PartialHandler(request, farmer_record, start_time));
	}

	public State getState() {
		return this.state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public PoolInfo getPoolInfo() {
		return poolInfo;
	}

	public PoolSettings getPoolSettings() {
		return poolSettings;
	}

	public void reloadPoolSettings() {
		PoolSettings tmp = PoolAdminDAO.loadPoolSettings();
		if(tmp != null) {
			this.poolSettings = tmp;
		}
	}

	public WalletRpcClient getHotWalletClient() {
		return hotWalletClient;
	}
	
	public WalletRpcClient getContractWalletClient() {
		return contractWalletClient;
	}

	public FullNodeRpcClient getFullNodeClient() {
		return fullNodeClient;
	}

	public HashSet<Bytes32> getScan_p2_singleton_puzzle_hashes() {
		return scan_p2_singleton_puzzle_hashes;
	}
	
	public ConcurrentLinkedQueue<ArrayList<PendingPayment>> getPending_Collections() {
		return pending_collections;
	}
	
	public ConcurrentLinkedQueue<ArrayList<FarmerPayoutRecord>> getPending_farmer_payments() {
		return pending_farmer_payments;
	}

	public boolean validateAuthenticationToken(NativeUInt64 authentication_token) {
		return authentication_token.toBigInteger().subtract(getCurrentAuthenticationToken().toBigInteger()).abs().compareTo(this.poolInfo.getAuthentication_token_timeout().toBigInteger()) <= 0;
	}

	public NativeUInt32 getCurrentAuthenticationToken() {
		return new NativeUInt32(Instant.now().atZone(ZoneOffset.UTC).toEpochSecond()).div(60l).div(this.poolInfo.getAuthentication_token_timeout());
	}
	
	public ValidatedSingletonState getValidateSingletonState(Bytes32 launcher_id, FarmerRecord record) {
		SingletonState singletonState = getSingletonState(launcher_id, record, this.state.getPeak().getHeight(), this.poolSettings.getConfirmation_security_threshold());
		if (singletonState == null) {
			Logger.getInstance().log(Level.WARNING, "Null Singleton state: (Launcher_ID) " + launcher_id.toString());
			return null;
		}
		// Validate state of the singleton
		boolean is_pool_member = true;
		Bytes32 singleton_hash = singletonState.getSavedState().getTargetPuzzleHash();
		if (!singleton_hash.equals(Bech32.decodePuzzleHash(this.poolInfo.getPool_contract_puzzle_hash()))) {
			Logger.getInstance().log(Level.WARNING, "Wrong target puzzle hash: " + singletonState.getSavedState().getTargetPuzzleHash() + " for launcher_id " + launcher_id);
			is_pool_member = false;
		} else if (singletonState.getSavedState().getRelativeLockHeight().compareTo(this.poolInfo.getRelative_lock_height()) != 0) {
			Logger.getInstance().log(Level.WARNING, "Wrong relative lock height: " + singletonState.getSavedState().getRelativeLockHeight() + " for launcher_id " + launcher_id);
			is_pool_member = false;
		} else if (singletonState.getSavedState().getVersion().intValue() != POOL_PROTOCOL_VERSION) {
			Logger.getInstance().log(Level.WARNING, "Wrong version " + singletonState.getSavedState().getVersion() + " for launcher_id " + launcher_id);
			is_pool_member = false;
		} else if (singletonState.getSavedState().getState().intValue() == PoolSingletonState.SELF_POOLING.getValue()) {
			Logger.getInstance().log(Level.WARNING, "Invalid singleton state " + singletonState.getSavedState().getState() + " for launcher_id " + launcher_id);
			is_pool_member = false;
		} else if (singletonState.getSavedState().getState().intValue() == PoolSingletonState.LEAVING_POOL.getValue()) {
			CoinRecord coin_record = fullNodeClient.get_coin_record_by_name(singletonState.getSavedSolution().getCoin().name());
			// assert coin_record is not None
			if (coin_record == null) {
				Logger.getInstance().log(Level.WARNING, "Error No Coin Data");
				return null;
			}
			if ((this.state.getPeak().getHeight().sub(coin_record.getConfirmedBlockIndex())).compareTo(this.poolInfo.getRelative_lock_height()) > 0) {
				Logger.getInstance().log(Level.WARNING, "launcher_id " + launcher_id + " got enough confirmations to leave the pool");
				is_pool_member = false;
			} else {
				Logger.getInstance().log(Level.WARNING, "launcher_id " + launcher_id + " started leaving pool, but hasnt been confirmed.");
			}
		}

		if (record != null && (!record.getSingletonTip().equals(singletonState.getSavedSolution()) || !record.getSingletonTipState().equals(singletonState.getSavedState()))) {
			// This means the singleton has been changed in the blockchain (either by us or someone else). 
			// We still keep track of this singleton if the farmer has changed to a different pool, in case they switch back.
			Logger.getInstance().log(Level.INFO, "Updating singleton state for " + launcher_id);
			PoolDAO.updateSingleton(launcher_id, singletonState.getSavedSolution(), singletonState.getSavedState(), is_pool_member);
		}
		ValidatedSingletonState toReturn = new ValidatedSingletonState();
		toReturn.setBuriedSingletonTip(singletonState.getSavedSolution());
		toReturn.setBuriedSingletonTipState(singletonState.getSavedState());
		toReturn.setPoolMember(is_pool_member);
		return toReturn; // singleton_tip, singleton_tip_state, is_pool_member
	}

	public SingletonState getSingletonState(Bytes32 launcher_id, FarmerRecord farmer_record, NativeUInt32 peak_height, int confirmation_security_threshold) {
		try {
			CoinSpend last_solution;
			NativeUInt64 delay_time;
			Bytes32 delay_puzzle_hash;
			PoolState saved_state, pool_state;
			if (farmer_record == null) {
				CoinRecord launcher_coin = fullNodeClient.get_coin_record_by_name(launcher_id.toString());
				if (launcher_coin == null) {
					Logger.getInstance().log(Level.WARNING, "Can not find genesis coin " + launcher_id);
					return null;
				}
				if (launcher_coin.isSpent() == false) {
					Logger.getInstance().log(Level.WARNING, "Genesis coin " + launcher_id + " not spent");
					return null;
				}

				last_solution = fullNodeClient.get_coin_spend(launcher_coin);
				DelayedPuzInfo delaypuzHash = Puzzles.getDelayedPuzInfoFromLauncherSpend(last_solution);
				delay_time = delaypuzHash.getDelayTime();
				delay_puzzle_hash = delaypuzHash.getDelayPuzzleHash();
				saved_state = PoolPuzzles.solutionToPoolState(last_solution);
				if (last_solution == null || saved_state == null) {
					return null;
				}
			} else {
				last_solution = farmer_record.getSingletonTip();
				saved_state = farmer_record.getSingletonTipState();
				delay_time = farmer_record.getDelayTime();
				delay_puzzle_hash = farmer_record.getDelayPuzzleHash();
			}
			CoinSpend saved_solution = last_solution;
			PoolState last_not_none_state = saved_state;
			if (last_solution == null) {
				return null;
			}

			CoinRecord last_coin_record = fullNodeClient.get_coin_record_by_name(last_solution.getCoin().name());
			if (last_coin_record == null) {
				return null;
			}

			while (true) {
				// # Get next coin solution
				Coin next_coin = Puzzles.getMostRecentSingletonCoinFromCoinSolution(last_solution);
				if (next_coin == null) {
					// This means the singleton is invalid
					return null;
				}
				CoinRecord next_coin_record = fullNodeClient.get_coin_record_by_name(next_coin.name());
				if (next_coin_record == null) {
					return null;
				}

				if (next_coin_record.isSpent() == false) {
					boolean valid_puzzle_hash = PoolPuzzles.validate_puzzle_hash(
							last_not_none_state, 
							launcher_id, 
							delay_puzzle_hash, 
							delay_time, 
							next_coin_record.getCoin().getPuzzleHash(), 
							Pool.getInstance().getPoolSettings().isIs_testnet() ? Constants.TESTNET_GENESIS_CHALLENGE : Constants.GENESIS_CHALLENGE
					);
					if (valid_puzzle_hash == false) {
						Logger.getInstance().log(Level.SEVERE, "Invalid singleton puzzle_hash for " + launcher_id);
						return null;
					}
					break;
				}
				last_solution = fullNodeClient.get_coin_spend(next_coin_record);
				if (last_solution == null) {
					return null;
				}

				pool_state = PoolPuzzles.solutionToPoolState(last_solution);

				if (pool_state != null) {
					last_not_none_state = pool_state;
				}
				if (peak_height.sub(confirmation_security_threshold).compareTo(next_coin_record.getSpentBlockIndex()) >= 0) {
					// There is a state transition, and it is sufficiently buried
					saved_solution = last_solution;
					saved_state = last_not_none_state;
				} else {
					Logger.getInstance().log(Level.FINE, "Singleton state not burried for farmer: " + launcher_id);
				}
			}
			SingletonState toRtn = new SingletonState();
			toRtn.setSaved_solution(saved_solution);
			toRtn.setSavedState(saved_state);
			toRtn.setLastNotNullState(last_not_none_state);
			return toRtn;
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in garden.druid.pool.Pool.getSingletonState", e);
			return null;
		}
	}
	
	public static NativeUInt32 get_farmed_height(CoinRecord reward_coin_record) {
		Bytes32 genesis_challenge = Pool.getInstance().getPoolSettings().isIs_testnet() ? Constants.TESTNET_GENESIS_CHALLENGE : Constants.GENESIS_CHALLENGE;
		// # Returns the height farmed if it's a coinbase reward, otherwise None
		for (NativeUInt32 block_index : range(reward_coin_record.getConfirmedBlockIndex().toBigInteger(), reward_coin_record.getConfirmedBlockIndex().sub(128).toBigInteger(), BigInteger.valueOf(-1))) {
			if (block_index.compareTo(NativeUInt64.ZERO) < 0) {
				break;
			}
			Bytes32 pool_parent = pool_parent_id(block_index, genesis_challenge);
			if (pool_parent.equals(reward_coin_record.getCoin().getParentCoinInfo())) {
				return block_index;
			}
		}
		return null;
	}
	
	private static Bytes32 pool_parent_id(NativeUInt32 block_index, Bytes32 genesis_challenge) {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.put(Arrays.copyOfRange(genesis_challenge.getBytes(), 0, 16));
		buf.put(new NativeUInt128(block_index.toBigInteger()).toByteArray());
		byte[] result = new byte[32];
		buf.flip();
		buf.get(result, 0, 32);
		return new Bytes32(result);
	}

	private static ArrayList<NativeUInt32> range(BigInteger start, BigInteger stop, BigInteger step) {
		ArrayList<NativeUInt32> range = new ArrayList<NativeUInt32>();
		BigInteger cur = start;
		// range.add(cur);
		while (cur.compareTo(stop) != 0) {
			cur = cur.add(step);
			range.add(new NativeUInt32(cur));
		}
		return range;
	}
}
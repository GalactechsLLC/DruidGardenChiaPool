package garden.druid.pool.concurrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import garden.druid.base.threads.threadpools.ManagedRunnable;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.BlockRecord;
import garden.druid.chia.types.blockchain.CoinRecord;
import garden.druid.chia.types.blockchain.FullBlock;
import garden.druid.chia.types.bytes.Bytes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolDAO;
import garden.druid.pool.database.PoolFeaturesDAO;
import garden.druid.pool.types.FarmerRecord;

public class BlockHistoryUpdater extends ManagedRunnable {

	private static final long serialVersionUID = 1L;

	public BlockHistoryUpdater(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "BlockHistoryUpdater";
	}
	
	@Override
	public void run() {
		this.started = true;
		try {
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			if (Pool.getInstance().getState().getSync().isSynced() == false) {
				return;
			}
			Set<Bytes32> puzzleHashes = new HashSet<Bytes32>(PoolDAO.getPayToSingletonPHS());
			if (puzzleHashes.size() == 0) {
				return;
			}
			NativeUInt32 lastHeight = PoolFeaturesDAO.getLastSyncedBlockHistoryHeight();
			Map<Bytes32, FarmerRecord> farmerMap = PoolDAO.getFarmerRecordsForPayToSingletonPHS(puzzleHashes).stream().collect(Collectors.toMap(entry -> entry.getP2SingletonPuzzleHash(), entry -> entry));
			CoinRecord[] coinRecords = client.get_coin_records_by_puzzle_hashes(
				new ArrayList<Bytes32>(puzzleHashes), 
				true, 
				Pool.getInstance().getPoolSettings().getScan_start_height().max(lastHeight),
				Pool.getInstance().getState().getPeak().getHeight()
			);
			for(CoinRecord coinRecord : coinRecords) {
				NativeUInt32 farmedHeight = NativeUInt32.ZERO;
				Bytes32 launcherId = new Bytes32(Bytes.multiply(new byte[]{0}, 32));
				if(coinRecord.getConfirmedBlockIndex() != null) {
					farmedHeight = Pool.get_farmed_height(coinRecord);
					BlockRecord block = client.get_block_record_by_height(farmedHeight);
					FullBlock fBlock = client.get_block(block.getHeaderHash());
					launcherId = farmerMap.get(fBlock.getRewardChainBlock().getProofOfSpace().getPoolContractPuzzleHash()).getLauncherId();
				}
				PoolFeaturesDAO.saveBlockHistory(launcherId, farmedHeight, new Bytes32(coinRecord.getCoin().name()), coinRecord.getCoin().getAmount(), coinRecord.getTimestamp());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.done = true;
		}
	}
}

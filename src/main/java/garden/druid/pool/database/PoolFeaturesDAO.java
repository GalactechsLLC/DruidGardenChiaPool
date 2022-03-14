package garden.druid.pool.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;

import garden.druid.base.database.BaseDAO;
import garden.druid.base.logging.Logger;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.types.Dataset;
import garden.druid.pool.types.LeaderBoardRecord;
import garden.druid.pool.types.PublicFarmerData;
import garden.druid.pool.types.RecentBlock;

public class PoolFeaturesDAO extends BaseDAO {
	
	public static ArrayList<LeaderBoardRecord> getLeaderBoard() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<LeaderBoardRecord> leaderBoard = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT a.launcher_id, b.difficulty, b.points, count(*) as partialCount, min(`timestamp`) as minTimestamp, max(`timestamp`) as maxTimestamp,"
					+ " b.points + (SELECT sum(points) FROM `ChiaPool`.`farmer_points_history` c WHERE c.launcher_id = a.launcher_id) as totalPoints"
					+ " FROM `ChiaPool`.`partials` a LEFT JOIN `ChiaPool`.`farmer` b on a.launcher_id=b.launcher_id"
					+ " WHERE `status`='VALID' GROUP BY a.launcher_id order by points desc limit 50;");
			rs = stmt.executeQuery();
			leaderBoard = new ArrayList<LeaderBoardRecord>();
			while(rs.next()) {
				LeaderBoardRecord lbr = new LeaderBoardRecord();
				lbr.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				lbr.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
				lbr.setPoints(new NativeUInt64(rs.getLong("points")));
				lbr.setMinTimeStamp(new NativeUInt64(rs.getLong("minTimestamp")));
				lbr.setMaxTimeStamp(new NativeUInt64(rs.getLong("maxTimestamp")));
				lbr.setEstimatedSpace(new NativeUInt64(calculateEstimatedSpace(rs.getLong("partialCount"), rs.getLong("minTimestamp"))));
				lbr.setEstimatedPartials(lbr.getEstimatedSpace().divFloor(106364865085l));
				lbr.setTotalPoints(new NativeUInt64(rs.getLong("totalPoints")));
				leaderBoard.add(lbr);
			}
		} catch (Exception e) {
			leaderBoard = null;
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getLeaderBoard", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return leaderBoard;
	}
	
	public static NativeUInt32 getLastSyncedBlockHistoryHeight() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT max(height) as maxHeight FROM ChiaPool.block_history");
			rs = stmt.executeQuery();
			if(rs.next()) {
				return new NativeUInt32(rs.getLong("maxHeight"));
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getLastSyncedBlockHistoryHeight", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return NativeUInt32.ZERO;
	}
	
	public static ArrayList<RecentBlock> getBlockHistory(int offset, int limit) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`block_history` ORDER BY `height` DESC LIMIT ?, ?");
			stmt.setInt(1, offset);
			stmt.setInt(2, limit);
			rs = stmt.executeQuery();
			ArrayList<RecentBlock> rBlocks = new ArrayList<RecentBlock>();
			while(rs.next()) {
				RecentBlock rBlock = new RecentBlock();
				rBlock.setAmount(new NativeUInt64(rs.getLong("amount")));
				rBlock.setCoin(new Bytes32(rs.getBytes("coin")));
				rBlock.setHeight(new NativeUInt32(rs.getLong("height")));
				rBlock.setLauncherID(new Bytes32(rs.getBytes("launcher_id")));
				rBlock.setTimestamp(new NativeUInt64(rs.getLong("timestamp")));
				rBlocks.add(rBlock);
			}
			return rBlocks;
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getBlockHistory", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return null;
	}
	
	public static boolean saveBlockHistory(Bytes32 launcherID, NativeUInt32 height, Bytes32 coin, NativeUInt64 amount, NativeUInt64 timestamp) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("INSERT INTO `ChiaPool`.`block_history` (`launcher_id`,`height`,`coin`,`amount`,`timestamp`) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE launcher_id=?, height=?");
			stmt.setBytes(1, launcherID.getBytes());
			stmt.setLong(2, height.longValue());
			stmt.setBytes(3, coin.getBytes());
			stmt.setLong(4, amount.longValue());
			stmt.setLong(5, timestamp.longValue());
			stmt.setBytes(6, launcherID.getBytes());
			stmt.setLong(7, height.longValue());
			return stmt.executeUpdate() == 1;
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.saveBlockHistory", e);
		} finally {
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return false;
	}
	
	private static long calculateEstimatedSpace(long count, long minTimeStamp) {
		return calculateEstimatedSpace(count, minTimeStamp, Instant.now().getEpochSecond());
	}
	
	private static long calculateEstimatedSpace(long count, long minTimeStamp, long maxTimeStamp) {
		return (long) (count / ((maxTimeStamp - minTimeStamp) * ( 0.0001157/106364865085l)));
	}
	
	public static ArrayList<RecentBlock> getFarmerBlockHistory(Bytes32 launcherID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`block_history` where launcher_id = ? ORDER BY `height`");
			stmt.setBytes(1, launcherID.getBytes());
			rs = stmt.executeQuery();
			ArrayList<RecentBlock> rBlocks = new ArrayList<RecentBlock>();
			while(rs.next()) {
				RecentBlock rBlock = new RecentBlock();
				rBlock.setAmount(new NativeUInt64(rs.getLong("amount")));
				rBlock.setCoin(new Bytes32(rs.getBytes("coin")));
				rBlock.setHeight(new NativeUInt32(rs.getLong("height")));
				rBlock.setLauncherID(new Bytes32(rs.getBytes("launcher_id")));
				rBlock.setTimestamp(new NativeUInt64(rs.getLong("timestamp")));
				rBlocks.add(rBlock);
			}
			return rBlocks;
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getBlockHistory", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return null;
	}

	public static PublicFarmerData getPublicFarmerData(Bytes32 launcherID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT balance, points, difficulty, is_pool_member, UNIX_TIMESTAMP(joined) as joined, UNIX_TIMESTAMP(modified) as modified, (SELECT sum(points) FROM ChiaPool.farmer_points_history WHERE launcher_id = ? group by launcher_id) as totalPoints, (SELECT sum(amount) FROM ChiaPool.farmer_payout_history WHERE launcher_id = ? group by launcher_id) as totalPaid FROM ChiaPool.farmer WHERE launcher_id = ?");
			stmt.setBytes(1, launcherID.getBytes());
			stmt.setBytes(2, launcherID.getBytes());
			stmt.setBytes(3, launcherID.getBytes());
			rs = stmt.executeQuery();
			if(rs.next()) {
				PublicFarmerData fData = new PublicFarmerData();
				fData.setLauncherID(launcherID);
				fData.setPoints(new NativeUInt64(rs.getLong("points")));
				fData.setTotalPoints(new NativeUInt64(rs.getLong("totalPoints")));
				fData.setBalance(new NativeUInt64(rs.getLong("balance")));
				fData.setTotalPaid(new NativeUInt64(rs.getLong("totalPaid")));
				fData.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				fData.setJoined(new NativeUInt64(rs.getLong("joined")));
				fData.setPoolMember(rs.getInt("is_pool_member") > 0);
				return fData;
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getPublicFarmerData", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return null;
	}
	
	public static NativeUInt64 getNetspace(Bytes32 launcherID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT count(*) as partialCount, min(`timestamp`) as minTimestamp FROM `ChiaPool`.`partials` WHERE `status`='VALID' AND launcher_id=? GROUP BY launcher_id");
			stmt.setBytes(1, launcherID.getBytes());
			rs = stmt.executeQuery();
			if(rs.next()) {
				return new NativeUInt64(calculateEstimatedSpace(rs.getLong("partialCount"), rs.getLong("minTimestamp")));
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getLeaderBoard", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return null;
	}
	
	public static ArrayList<Dataset<Long, Long>> getFarmerNetspaceTimeline(Bytes32 launcherID, long start, long end) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<Dataset<Long, Long>> rtn = new ArrayList<Dataset<Long, Long>>();
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement(
					"SELECT (`timestamp` DIV 3600) * 3600 as label, count(pos_hash) as partialCount " +
					"FROM `ChiaPool`.`partials` " +
					"WHERE `timestamp` between ? AND ? AND launcher_id = ? " + 
					"GROUP BY label ORDER BY LABEL ASC"
					);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			stmt.setBytes(3, launcherID.getBytes());
			rs = stmt.executeQuery();
			while(rs.next()) {
				long label = rs.getLong("label");
				rtn.add(new Dataset<Long,Long>(label, calculateEstimatedSpace(rs.getLong("partialCount"), label, label + 3599)));
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getFarmerNetspaceTimeline", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return rtn;
	}
	
	public static ArrayList<Dataset<Long, Long>> getPoolNetspaceTimeline(long start, long end) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<Dataset<Long, Long>> rtn = new ArrayList<Dataset<Long, Long>>();
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement(
					"SELECT (`timestamp` DIV 3600) * 3600 as label, count(pos_hash) as partialCount " +
					"FROM `ChiaPool`.`partials` " +
					"WHERE `timestamp` between ? AND ? " + 
					"GROUP BY label ORDER BY LABEL ASC"
					);
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			rs = stmt.executeQuery();
			while(rs.next()) {
				long label = rs.getLong("label");
				rtn.add(new Dataset<Long,Long>(label, calculateEstimatedSpace(rs.getLong("partialCount"), label, label + 3599)));
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolFeaturesDAO.getNetspaceTimeline", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return rtn;
	}
}

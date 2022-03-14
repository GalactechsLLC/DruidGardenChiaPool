package garden.druid.pool.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import garden.druid.base.database.BaseDAO;
import garden.druid.base.logging.Logger;
import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes48;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.types.FarmerPayoutIntructions;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PoolState;

import java.util.List;
import java.util.logging.Level;
import java.util.Set;

import java.util.ArrayList;

public class PoolDAO extends BaseDAO {

	private transient static final Object farmerUpdatelock = new Object();
	
	public static ArrayList<FarmerRecord> getFarmersForPayout(NativeUInt64 threshold) {
		synchronized(farmerUpdatelock){
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			ArrayList<FarmerRecord> records = new ArrayList<FarmerRecord>();
			;
			try {
				conn = getConnection("ChiaPool");
				stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`farmer` WHERE balance >= ?");
				stmt.setBytes(1, threshold.toByteArray());
				rs = stmt.executeQuery();
				FarmerRecord record = null;
				while (rs.next()) {
					record = new FarmerRecord();
					record.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
					record.setP2SingletonPuzzleHash(new Bytes32(rs.getBytes("p2_singleton_puzzle_hash")));
					record.setDelayTime(new NativeUInt64(rs.getLong("delay_time")));
					record.setDelayPuzzleHash(new Bytes32(rs.getBytes("delay_puzzle_hash")));
					record.setAuthenticationPublicKey(new Bytes48(rs.getBytes("authentication_public_key")));
					record.setSingletonTip(gson.fromJson(rs.getString("singleton_tip"), CoinSpend.class));
					record.setSingletonTipState(gson.fromJson(rs.getString("singleton_tip_state"), PoolState.class));
					record.setPoints(new NativeUInt64(rs.getLong("points")));
					record.setBalance(new NativeUInt64(rs.getLong("balance")));
					record.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
					record.setPayoutInstructions(rs.getString("payout_instructions"));
					record.setPoolMember(rs.getInt("is_pool_member") > 0 ? true : false);
					records.add(record);
				}
			} catch (Exception ex) {
				records = null;
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getFarmerRecords", ex);
			} finally {
				if(rs != null) try {rs.close();}catch(Exception e) {}
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(conn != null) try {conn.close();}catch(Exception e) {}
			}
			return records;
		}
	}
	
	/**
	 * @param launcherID The Farmers Launcher ID
	 * @param points
	 * @param totalPoints
	 * @param mojoPerPoint
	 * @throws SQLException
	 */
	public static void saveFarmerPointsHistory(Bytes32 launcherID, NativeUInt64 points, NativeUInt64 totalPoints, NativeUInt64 mojoPerPoint) throws SQLException {
		synchronized(farmerUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = PoolDAO.getConnection("ChiaPool");
				stmt = con.prepareStatement("INSERT INTO `ChiaPool`.`farmer_points_history` (SELECT null, launcher_id, balance, balance+?, ?, ?, ?, CURRENT_TIMESTAMP FROM `ChiaPool`.`farmer` WHERE launcher_id=?)");
				stmt.setBytes(1, points.mul(mojoPerPoint).toByteArray());
				stmt.setBytes(2, points.toByteArray());
				stmt.setBytes(3, totalPoints.toByteArray());
				stmt.setBytes(4, mojoPerPoint.toByteArray());
				stmt.setBytes(5, launcherID.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in ChiaExplorerDAO.saveFarmerPointsHistory", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	
	public static void addFarmerBalance(Bytes32 launcherID, NativeUInt64 addAmount) throws SQLException {
		synchronized(farmerUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = PoolDAO.getConnection("ChiaPool");
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`farmer` set balance=balance+? where launcher_id=?");
				stmt.setBytes(1, addAmount.toByteArray());
				stmt.setBytes(2, launcherID.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in ChiaExplorerDAO.addFarmerBalance", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	
	public static void subFarmerBalance(Bytes32 launcher_id, NativeUInt64 subAmount) throws SQLException {
		synchronized(farmerUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = PoolDAO.getConnection("ChiaPool");
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`farmer` set balance=balance-? where launcher_id=?");
				stmt.setBytes(1, subAmount.toByteArray());
				stmt.setBytes(2, launcher_id.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in ChiaExplorerDAO.subFarmerBalance", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	
	public static void addFarmerPoints(Bytes32 launcherID, NativeUInt64 addAmount) throws SQLException {
		synchronized(farmerUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = getConnection("ChiaPool");
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`farmer` set points=points+? where launcher_id=?");
				stmt.setBytes(1, addAmount.toByteArray());
				stmt.setBytes(2, launcherID.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in ChiaExplorerDAO.addFarmerPoints", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	
	public static void subFarmerPoints(Bytes32 launcherID, NativeUInt64 subAmount) throws SQLException {
		synchronized(farmerUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = getConnection("ChiaPool");
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`farmer` set points=points-? where launcher_id=?");
				stmt.setBytes(1, subAmount.toByteArray());
				stmt.setBytes(2, launcherID.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in ChiaExplorerDAO.subFarmerPoints", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}

	public static boolean addFarmerRecord(FarmerRecord farmerRecord) {
		synchronized(farmerUpdatelock){
			Connection conn = null;
			PreparedStatement stmt = null;
			try {
				conn = getConnection("ChiaPool");
				stmt = conn.prepareStatement("INSERT INTO `ChiaPool`.`farmer` VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())");
				stmt.setBytes(1, farmerRecord.getLauncherId().getBytes());
				stmt.setBytes(2, farmerRecord.getP2SingletonPuzzleHash().getBytes());
				stmt.setLong(3, farmerRecord.getDelayTime().longValue());
				stmt.setBytes(4, farmerRecord.getDelayPuzzleHash().getBytes());
				stmt.setBytes(5, farmerRecord.getAuthenticationPublicKey().getBytes());
				stmt.setString(6, gson.toJson(farmerRecord.getSingletonTip()));
				stmt.setString(7, gson.toJson(farmerRecord.getSingletonTipState()));
				stmt.setBytes(8, farmerRecord.getBalance().toByteArray());
				stmt.setBytes(9, farmerRecord.getPoints().toByteArray());
				stmt.setBytes(10, farmerRecord.getDifficulty().toByteArray());
				stmt.setString(11, farmerRecord.getPayoutInstructions());
				stmt.setInt(12, farmerRecord.isPoolMember() ? 1 : 0);
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.addFarmerRecord", ex);
				return false;
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(conn != null) try {conn.close();}catch(Exception e) {}
			}
		}
		return true;
	}

	public static boolean updateFarmerRecord(FarmerRecord farmerRecord) {
		synchronized(farmerUpdatelock){
			Connection conn = null;
			PreparedStatement stmt = null;
			try {
				conn = getConnection("ChiaPool");
				stmt = conn.prepareStatement("UPDATE `ChiaPool`.`farmer` SET `p2_singleton_puzzle_hash` = ?, `delay_time` = ?, "
						+ "`delay_puzzle_hash` = ?, `authentication_public_key` = ?, `singleton_tip` = ?, `singleton_tip_state` = ?,"
						+ "`points` = ?, `difficulty` = ?, `payout_instructions` = ?, `is_pool_member` = ?, `modified` = NOW() WHERE `launcher_id` = ?");
				
				stmt.setBytes(1, farmerRecord.getP2SingletonPuzzleHash().getBytes());
				stmt.setLong(2, farmerRecord.getDelayTime().longValue());
				stmt.setBytes(3, farmerRecord.getDelayPuzzleHash().getBytes());
				stmt.setBytes(4, farmerRecord.getAuthenticationPublicKey().getBytes());
				stmt.setString(5, gson.toJson(farmerRecord.getSingletonTip()));
				stmt.setString(6, gson.toJson(farmerRecord.getSingletonTipState()));
				stmt.setBytes(7, farmerRecord.getPoints().toByteArray());
				stmt.setBytes(8, farmerRecord.getDifficulty().toByteArray());
				stmt.setString(9, farmerRecord.getPayoutInstructions());
				stmt.setInt(10, farmerRecord.isPoolMember() ? 1 : 0);
				stmt.setBytes(11, farmerRecord.getLauncherId().getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.updateFarmerRecord", ex);
				return false;
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(conn != null) try {conn.close();}catch(Exception e) {}
			}
		}
		return true;
	}

	public static FarmerRecord getFarmerRecord(Bytes32 launcherID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		FarmerRecord record = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`farmer` WHERE launcher_id=?");
			stmt.setBytes(1, launcherID.getBytes());
			rs = stmt.executeQuery();
			if (rs.next()) {
				record = new FarmerRecord();
				record.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
				record.setP2SingletonPuzzleHash(new Bytes32(rs.getBytes("p2_singleton_puzzle_hash")));
				record.setDelayTime(new NativeUInt64(rs.getLong("delay_time")));
				record.setDelayPuzzleHash(new Bytes32(rs.getBytes("delay_puzzle_hash")));
				record.setAuthenticationPublicKey(new Bytes48(rs.getBytes("authentication_public_key")));
				record.setSingletonTip(gson.fromJson(rs.getString("singleton_tip"), CoinSpend.class));
				record.setSingletonTipState(gson.fromJson(rs.getString("singleton_tip_state"), PoolState.class));
				record.setPoints(new NativeUInt64(rs.getLong("points")));
				record.setBalance(new NativeUInt64(rs.getLong("balance")));
				record.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				record.setPayoutInstructions(rs.getString("payout_instructions"));
				record.setPoolMember(rs.getInt("is_pool_member") > 0 ? true : false);
			}
		} catch (Exception ex) {
			record = null;
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getFarmerRecord", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return record;
	}

	public static ArrayList<FarmerRecord> getFarmerRecords() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<FarmerRecord> records = new ArrayList<FarmerRecord>();
		;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`farmer`");
			rs = stmt.executeQuery();
			FarmerRecord record = null;
			while (rs.next()) {
				record = new FarmerRecord();
				record.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
				record.setP2SingletonPuzzleHash(new Bytes32(rs.getBytes("p2_singleton_puzzle_hash")));
				record.setDelayTime(new NativeUInt64(rs.getLong("delay_time")));
				record.setDelayPuzzleHash(new Bytes32(rs.getBytes("delay_puzzle_hash")));
				record.setAuthenticationPublicKey(new Bytes48(rs.getBytes("authentication_public_key")));
				record.setSingletonTip(gson.fromJson(rs.getString("singleton_tip"), CoinSpend.class));
				record.setSingletonTipState(gson.fromJson(rs.getString("singleton_tip_state"), PoolState.class));
				record.setPoints(new NativeUInt64(rs.getLong("points")));
				record.setBalance(new NativeUInt64(rs.getLong("balance")));
				record.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				record.setPayoutInstructions(rs.getString("payout_instructions"));
				record.setPoolMember(rs.getInt("is_pool_member") > 0 ? true : false);
				records.add(record);
			}
		} catch (Exception ex) {
			records = null;
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getFarmerRecords", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return records;
	}

	public static boolean updateDifficulty(Bytes32 launcherID, NativeUInt64 difficulty) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("UPDATE `ChiaPool`.`farmer` SET difficulty=? WHERE launcher_id=?");
			stmt.setBytes(1, difficulty.toByteArray());
			stmt.setBytes(2, launcherID.getBytes());
			stmt.executeUpdate();
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.updateDifficulty", ex);
			return false;
		} finally {
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return true;
	}

	public static boolean updateSingleton(Bytes32 launcherID, CoinSpend coinSpend, PoolState singletonTipState, boolean isPoolMember) {
		synchronized(farmerUpdatelock){
			Connection conn = null;
			PreparedStatement stmt = null;
			try {
				conn = getConnection("ChiaPool");
				stmt = conn.prepareStatement("UPDATE `ChiaPool`.`farmer` SET singleton_tip=?, singleton_tip_state=?, is_pool_member=? WHERE launcher_id=?");
				stmt.setString(1, gson.toJson(coinSpend));
				stmt.setString(2, gson.toJson(singletonTipState));
				stmt.setInt(3, isPoolMember ? 1 : 0);
				stmt.setBytes(4, launcherID.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.updateSingleton", ex);
				return false;
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(conn != null) try {conn.close();}catch(Exception e) {}
			}
		}
		return true;
	}

	public static List<Bytes32> getPayToSingletonPHS() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<Bytes32> hashes = new ArrayList<Bytes32>();
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT p2_singleton_puzzle_hash FROM `ChiaPool`.`farmer`");
			rs = stmt.executeQuery();
			while (rs.next()) {
				hashes.add(new Bytes32(rs.getBytes("p2_singleton_puzzle_hash")));
			}
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getPayToSingletonPHS", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return hashes;
	}

	public static List<FarmerRecord> getFarmerRecordsForPayToSingletonPHS(Set<Bytes32> puzzleHashes) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<FarmerRecord> records = new ArrayList<FarmerRecord>();
		try {
			if (puzzleHashes == null || puzzleHashes.size() == 0) {
				return new ArrayList<FarmerRecord>();
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < puzzleHashes.size(); i++) {
				sb.append("?,");
			}
			sb.deleteCharAt(sb.length() - 1);
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`farmer` WHERE CONCAT('0x',LOWER(HEX(p2_singleton_puzzle_hash))) IN (" + sb.toString() + ")");
			int i = 1;
			for (Bytes32 puz_hash : puzzleHashes) {
				stmt.setString(i, puz_hash.toString());
				i++;
			}
			rs = stmt.executeQuery();
			while (rs.next()) {
				FarmerRecord record = new FarmerRecord();
				record.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
				record.setP2SingletonPuzzleHash(new Bytes32(rs.getBytes("p2_singleton_puzzle_hash")));
				record.setDelayTime(new NativeUInt64(rs.getLong("delay_time")));
				record.setDelayPuzzleHash(new Bytes32(rs.getBytes("delay_puzzle_hash")));
				record.setAuthenticationPublicKey(new Bytes48(rs.getBytes("authentication_public_key")));
				record.setSingletonTip(gson.fromJson(rs.getString("singleton_tip"), CoinSpend.class));
				record.setSingletonTipState(gson.fromJson(rs.getString("singleton_tip_state"), PoolState.class));
				record.setPoints(new NativeUInt64(rs.getLong("points")));
				record.setBalance(new NativeUInt64(rs.getLong("balance")));
				record.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				record.setPayoutInstructions(rs.getString("payout_instructions"));
				record.setPoolMember(rs.getInt("is_pool_member") > 0 ? true : false);
				records.add(record);
			}
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getFarmerRecordsForPayToSingletonPHS", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return records;
	}

	public static List<FarmerPayoutIntructions> getFarmerPointsAndPayoutInstructions() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<FarmerPayoutIntructions> payouts = new ArrayList<FarmerPayoutIntructions>();
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT points, payout_instructions, launcher_id FROM `ChiaPool`.`farmer`");
			rs = stmt.executeQuery();
			while (rs.next()) {
				FarmerPayoutIntructions fpi = new FarmerPayoutIntructions();
				fpi.setLauncherId(new Bytes32(rs.getBytes("launcher_id")));
				fpi.setPayoutIntrstructions(rs.getString("payout_instructions"));
				fpi.setAmount(new NativeUInt64(rs.getLong("points")));
				payouts.add(fpi);
			}
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getFarmerPointsAndPayoutInstructions", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return payouts;
	}

	public static boolean clearFarmerPoints() {
		synchronized(farmerUpdatelock){
			Connection conn = null;
			PreparedStatement stmt = null;
			try {
				conn = getConnection("ChiaPool");
				stmt = conn.prepareStatement("UPDATE `ChiaPool`.`farmer` set points=?");
				stmt.setBytes(1, NativeUInt64.ZERO.toByteArray());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.clearFarmerPoints", ex);
				return false;
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(conn != null) try {conn.close();}catch(Exception e) {}
			}
		}
		return true;
	}
	
}

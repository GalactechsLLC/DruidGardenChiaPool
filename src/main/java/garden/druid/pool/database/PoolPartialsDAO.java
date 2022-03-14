package garden.druid.pool.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;

import garden.druid.base.database.BaseDAO;
import garden.druid.base.logging.Logger;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.types.Dataset;
import garden.druid.pool.types.PartialStatus;
import garden.druid.pool.types.PendingPartial;
import garden.druid.pool.types.RecentPartial;

public class PoolPartialsDAO extends BaseDAO{
	
	private transient static final Object partialLoadlock = new Object();
	private transient static final Object partialUpdatelock = new Object();

	public static PendingPartial getPendingPartial() throws SQLException {
		synchronized(partialLoadlock){
			Connection con = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			PreparedStatement ustmt = null;
			PendingPartial pendingPartial = null;
			try {
				con = getConnection("ChiaPool");
				// Update Partials
				stmt = con.prepareStatement("SELECT * FROM ChiaPool.partials WHERE (CAST(`timestamp` AS SIGNED) + ? - ? - 5) <= 0 AND status = 'NOT_STARTED' LIMIT 1");
				stmt.setLong(1, Pool.getInstance().getPoolSettings().getPartial_confirmation_delay());
				stmt.setLong(2, Instant.now().getEpochSecond());
				rs = stmt.executeQuery();
				if(rs.next()) {
					try {
						pendingPartial = gson.fromJson(rs.getString("json"), PendingPartial.class);
						ustmt = con.prepareStatement("UPDATE `ChiaPool`.`partials` set status='PENDING' where pos_hash=?");
						ustmt.setBytes(1, pendingPartial.getRequest().getPayload().getProofOfSpace().hash().getBytes());
						ustmt.executeUpdate();
					} catch(Exception e) {
						Logger.getInstance().log(Level.WARNING, "Error in PartialValidator.run", e);							}
					if(pendingPartial == null) {
						Logger.getInstance().log(Level.WARNING, "Failed to load Pending Partial: " + rs.getString("json"));
						return null;
					}
				}
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getPendingPartial", ex);
			} finally {
				if(rs != null) try {rs.close();}catch(Exception e) {}
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(ustmt != null) try {ustmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
			return pendingPartial;
		}
	}
	
	public static ArrayList<Dataset<Long, Integer>> getPartialsOverTime(long start, long end){
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<Dataset<Long, Integer>> rtn = new ArrayList<Dataset<Long, Integer>>();
		try {
			con = getConnection("ChiaPool");
			stmt = con.prepareStatement(
				  "SELECT (`timestamp` DIV 600) * 600 as label, count(pos_hash) as `data` "
				+ "FROM ChiaPool.partials "
				+ "WHERE `timestamp` between ? AND ? "
				+ "GROUP BY label");
			stmt.setLong(1, start);
			stmt.setLong(2, end);
			rs = stmt.executeQuery();
			while(rs.next()) {
				rtn.add(new Dataset<Long,Integer>(rs.getLong("label"), rs.getInt("data")));
			}
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getPartialsOverTime", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(con != null) try {con.close();}catch(Exception e) {}
		}
		return rtn;
	}

	public static void updatePartialStatus(Bytes32 posHash, PartialStatus partialStatus) throws SQLException {
		synchronized(partialUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = getConnection("ChiaPool");
				// Update Partials
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`partials` set status=? where pos_hash=?");
				stmt.setString(1, partialStatus.name());
				stmt.setBytes(2, posHash.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.updatePartialStatus", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	
	public static void clearPartialJSON(Bytes32 posHash) throws SQLException {
		synchronized(partialUpdatelock){
			Connection con = null;
			PreparedStatement stmt = null;
			try {
				con = getConnection("ChiaPool");
				// Update Partials
				stmt = con.prepareStatement("UPDATE `ChiaPool`.`partials` set json='{}' where pos_hash=?");
				stmt.setBytes(1, posHash.getBytes());
				stmt.executeUpdate();
			} catch (Exception ex) {
				Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.clearPartialJSON", ex);
			} finally {
				if(stmt != null) try {stmt.close();}catch(Exception e) {}
				if(con != null) try {con.close();}catch(Exception e) {}
			}
		}
	}
	

	public static boolean addPendingPartial(PendingPartial pendingPartial) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("ChiaPool");
			// Update Partials
			stmt = conn.prepareStatement("INSERT into `ChiaPool`.`partials` VALUES(?, ?, ?, ?, ?, ?, ?, 'NOT_STARTED')");
			stmt.setBytes(1, pendingPartial.getRequest().getPayload().getProofOfSpace().hash().getBytes());
			stmt.setLong(2, pendingPartial.getStartTime().getEpochSecond());
			stmt.setBytes(3, pendingPartial.getCurrentDifficulty().toByteArray());
			stmt.setBytes(4, pendingPartial.getRequest().getPayload().getLauncherId().getBytes());
			stmt.setBytes(5, pendingPartial.getRequest().getPayload().getHarvesterId().getBytes());
			stmt.setBytes(6, pendingPartial.getRequest().getPayload().getSpHash().getBytes());
			stmt.setString(7, gson.toJson(pendingPartial));
			stmt.executeUpdate();
			return true;
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.addPendingPartial", ex);
			return false;
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
	}

	public static ArrayList<RecentPartial> getRecentPartials(Bytes32 launcherID, int limit) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ArrayList<RecentPartial> partials = new ArrayList<RecentPartial>();
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT timestamp, difficulty from `ChiaPool`.`partials` WHERE launcher_id=? AND status = 'VALID' ORDER BY timestamp DESC LIMIT ?");
			stmt.setBytes(1, launcherID.getBytes());
			stmt.setInt(2, limit);
			rs = stmt.executeQuery();
			while (rs.next()) {
				RecentPartial partial = new RecentPartial();
				partial.setLauncherId(launcherID);
				partial.setTimestamp(new NativeUInt64(rs.getLong("timestamp")));
				partial.setDifficulty(new NativeUInt64(rs.getLong("difficulty")));
				partials.add(partial);
			}
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.getRecentPartials", ex);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return partials;
	}
}

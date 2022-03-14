package garden.druid.pool.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import garden.druid.base.database.BaseDAO;
import garden.druid.base.logging.Logger;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.chia.types.ints.NativeUInt8;
import garden.druid.pool.types.PoolInfo;
import garden.druid.pool.types.PoolSettings;

public class PoolAdminDAO extends BaseDAO {
	
	public static PoolInfo loadPoolInfo() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		PoolInfo poolInfo = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`pool_info` ORDER BY id DESC LIMIT 1");
			rs = stmt.executeQuery();
			if (rs.next()) {
				poolInfo = new PoolInfo();
				poolInfo.setDescription(rs.getString("description"));
				poolInfo.setName(rs.getString("name"));
				poolInfo.setFee(rs.getDouble("fee"));
				poolInfo.setLogoURL(rs.getString("logo_url"));
				poolInfo.setMin_difficulty(new NativeUInt64(rs.getLong("minimum_difficulty")));
				poolInfo.setDefault_difficulty(new NativeUInt64(rs.getLong("default_difficulty")));
				poolInfo.setProtocolVersion(rs.getInt("protocol_version"));
				poolInfo.setRelative_lock_height(new NativeUInt32(rs.getLong("relative_lock_height")));
				poolInfo.setPool_contract_puzzle_hash(rs.getString("pool_contract_puzzle_hash"));
				poolInfo.setPool_wallet_puzzle_hash(rs.getString("pool_wallet_puzzle_hash"));
				poolInfo.setAuthentication_token_timeout(new NativeUInt8(rs.getInt("authentication_token_timeout")));
				poolInfo.setFullNodeHost(rs.getString("full_node_host"));
				poolInfo.setFull_node_port(rs.getInt("full_node_port"));
				poolInfo.setContractWalletHost(rs.getString("contract_wallet_host"));
				poolInfo.setHotWalletHost(rs.getString("hot_wallet_host"));
				poolInfo.setContract_wallet_port(rs.getInt("contract_wallet_port"));
				poolInfo.setHot_wallet_port(rs.getInt("hot_wallet_port"));
				poolInfo.setHot_wallet_fingerprint(new NativeUInt32(rs.getLong("hot_wallet_fingerprint")));
				poolInfo.setContract_wallet_fingerprint(new NativeUInt32(rs.getLong("contract_wallet_fingerprint")));
				poolInfo.setHot_wallet_id(new NativeUInt32(rs.getInt("hot_wallet_id")));
				poolInfo.setContract_wallet_id(new NativeUInt32(rs.getInt("contract_wallet_id")));
			}
		} catch (Exception e) {
			poolInfo = null;
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.loadPoolInfo", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return poolInfo;
	}
	
	public static PoolSettings loadPoolSettings() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		PoolSettings poolSettings = null;
		try {
			conn = getConnection("ChiaPool");
			stmt = conn.prepareStatement("SELECT * FROM `ChiaPool`.`pool_settings` WHERE `status`='ACTIVE' ORDER BY id DESC LIMIT 1");
			rs = stmt.executeQuery();
			if (rs.next()) {
				poolSettings = new PoolSettings();
				poolSettings.setId(rs.getInt("id"));
				poolSettings.setCollect_pool_rewards_fee(new NativeUInt64(rs.getLong("collect_pool_rewards_fee")));;
				poolSettings.setCollect_pool_rewards_interval(rs.getInt("collect_pool_rewards_interval"));
				poolSettings.setScan_start_height(new NativeUInt32(rs.getLong("scan_start_height")));
				poolSettings.setPartial_time_limit(rs.getInt("partial_time_limit"));
				poolSettings.setFarmer_update_cooldown_seconds(rs.getInt("farmer_update_cooldown_seconds"));
				poolSettings.setConfirmation_security_threshold(rs.getInt("confirmation_security_threshold"));
				poolSettings.setPayment_interval(rs.getInt("payment_interval"));
				poolSettings.setMax_additions_per_transaction(rs.getInt("max_additions_per_transaction"));
				poolSettings.setNumber_of_partials_target(rs.getInt("number_of_partials_target"));
				poolSettings.setTime_target(rs.getInt("time_target"));
				poolSettings.setPool_payout_threshold(new NativeUInt64(rs.getLong("pool_payout_threshold")));;
				poolSettings.setIs_testnet(rs.getInt("is_testnet") > 0);
				poolSettings.setStatus(rs.getString("status"));
			}
		} catch (Exception e) {
			poolSettings = null;
			Logger.getInstance().log(Level.SEVERE, "Error in PoolDAO.loadPoolInfo", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception e) {}
			if(stmt != null) try {stmt.close();}catch(Exception e) {}
			if(conn != null) try {conn.close();}catch(Exception e) {}
		}
		return poolSettings;
	}
}

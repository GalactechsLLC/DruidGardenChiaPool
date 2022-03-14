package garden.druid.pool.types;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;

public class PoolSettings {

	private int id;

	// Fee used when collecting pool rewards
	@SerializedName(value = "collect_pool_rewards_fee", alternate = "collectPoolRewardsFee")
	private NativeUInt64 collectPoolRewardsFee;
	
	// Interval for scanning and collecting the pool rewards
	@SerializedName(value = "collect_pool_rewards_interval", alternate = "collectPoolRewardsInterval")
	private int collectPoolRewardsInterval;

	// Don't scan anything before this height, for efficiency (for example pool start date)
	@SerializedName(value = "scan_start_height", alternate = "scanStartHeight")
	private NativeUInt32 scanStartHeight;
	
	@SerializedName(value = "partial_time_limit", alternate = "partialTimeLimit")
	private int partialTimeLimit;

	// There is always a risk of a reorg, in which case we cannot reward farmers that submitted partials in that reorg. That is why we have a time delay before changing any account points.
	@SerializedName(value = "partial_confirmation_delay", alternate = "partialConfirmationDelay")
	private int partialConfirmationDelay;

	// Only allow PUT /farmer per launcher_id every n seconds to prevent difficulty change attacks.
	@SerializedName(value = "farmer_update_cooldown_seconds", alternate = "farmerUpdateCooldownSeconds")
	private int farmerUpdateCooldownSeconds;

	// After this many confirmations, a transaction is considered final and irreversible
	@SerializedName(value = "confirmation_security_threshold", alternate = "confirmationSecurityThreshold")
	private int confirmationSecurityThreshold;

	// Interval for making payout transactions to farmers
	@SerializedName(value = "payment_interval", alternate = "paymentInterval")
	private int paymentInterval;

	// We will not make transactions with more targets than this, to ensure our transaction gets into the blockchain faster.
	@SerializedName(value = "max_additions_per_transaction", alternate = "maxAdditionsPerTransaction")
	private int maxAdditionsPerTransaction;

	// We target these many partials for this number of seconds. We adjust after receiving this many partials.
	@SerializedName(value = "number_of_partials_target", alternate = "numberOfPartialsTarget")
	private int numberOfPartialsTarget;
	
	@SerializedName(value = "time_target", alternate = "timeTarget")
	private int timeTarget;

	// Fee used when collecting pool rewards
	@SerializedName(value = "pool_payout_threshold", alternate = "poolPayoutThreshold")
	private NativeUInt64 poolPayoutThreshold;
	
	@SerializedName(value = "is_testnet", alternate = "isTestnet")
	private boolean isTestnet;
	
	private String status;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public NativeUInt64 getCollect_pool_rewards_fee() {
		return collectPoolRewardsFee;
	}

	public void setCollect_pool_rewards_fee(NativeUInt64 collect_pool_rewards_fee) {
		this.collectPoolRewardsFee = collect_pool_rewards_fee;
	}

	public int getCollect_pool_rewards_interval() {
		return collectPoolRewardsInterval;
	}

	public void setCollect_pool_rewards_interval(int collect_pool_rewards_interval) {
		this.collectPoolRewardsInterval = collect_pool_rewards_interval;
	}

	public NativeUInt32 getScan_start_height() {
		return scanStartHeight;
	}

	public void setScan_start_height(NativeUInt32 scan_start_height) {
		this.scanStartHeight = scan_start_height;
	}

	public int getPartial_time_limit() {
		return partialTimeLimit;
	}

	public void setPartial_time_limit(int partial_time_limit) {
		this.partialTimeLimit = partial_time_limit;
	}

	public int getPartial_confirmation_delay() {
		return partialConfirmationDelay;
	}

	public void setPartial_confirmation_delay(int partial_confirmation_delay) {
		this.partialConfirmationDelay = partial_confirmation_delay;
	}

	public int getFarmer_update_cooldown_seconds() {
		return farmerUpdateCooldownSeconds;
	}

	public void setFarmer_update_cooldown_seconds(int farmer_update_cooldown_seconds) {
		this.farmerUpdateCooldownSeconds = farmer_update_cooldown_seconds;
	}

	public int getConfirmation_security_threshold() {
		return confirmationSecurityThreshold;
	}

	public void setConfirmation_security_threshold(int confirmation_security_threshold) {
		this.confirmationSecurityThreshold = confirmation_security_threshold;
	}

	public int getPayment_interval() {
		return paymentInterval;
	}

	public void setPayment_interval(int payment_interval) {
		this.paymentInterval = payment_interval;
	}

	public int getMax_additions_per_transaction() {
		return maxAdditionsPerTransaction;
	}

	public void setMax_additions_per_transaction(int max_additions_per_transaction) {
		this.maxAdditionsPerTransaction = max_additions_per_transaction;
	}

	public int getNumber_of_partials_target() {
		return numberOfPartialsTarget;
	}

	public void setNumber_of_partials_target(int number_of_partials_target) {
		this.numberOfPartialsTarget = number_of_partials_target;
	}

	public int getTime_target() {
		return timeTarget;
	}

	public void setTime_target(int time_target) {
		this.timeTarget = time_target;
	}

	public NativeUInt64 getPool_payout_threshold() {
		return poolPayoutThreshold;
	}

	public void setPool_payout_threshold(NativeUInt64 pool_payout_threshold) {
		this.poolPayoutThreshold = pool_payout_threshold;
	}

	public boolean isIs_testnet() {
		return isTestnet;
	}

	public void setIs_testnet(boolean is_testnet) {
		this.isTestnet = is_testnet;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

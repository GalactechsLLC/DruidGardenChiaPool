package garden.druid.pool.types;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.chia.types.ints.NativeUInt8;

public class PoolInfo {
	@SerializedName(value = "protocol_version", alternate = "protocolVersion")
	private int protocolVersion;
	@SerializedName(value = "full_node_port", alternate = "fullNodePort")
	private int fullNodePort;
	@SerializedName(value = "hot_wallet_port", alternate = "hotWalletPort")
	private int hotWalletPort;
	@SerializedName(value = "contract_wallet_port", alternate = "contractWalletPort")
	private int contractWalletPort;
	private double fee;
	private NativeUInt8 authenticationTokenTimeout;
	@SerializedName(value = "relative_lock_height", alternate = "relativeLockHeight")
	private NativeUInt32 relativeLockHeight;
	@SerializedName(value = "hot_wallet_fingerprint", alternate = "hotWalletFingerprint")
	private NativeUInt32 hotWalletFingerprint;
	@SerializedName(value = "hot_wallet_id", alternate = "hotWalletId")
	private NativeUInt32 hotWalletId; 
	@SerializedName(value = "contract_wallet_fingerprint", alternate = "contractWalletFingerprint")
	private NativeUInt32 contractWalletFingerprint;
	@SerializedName(value = "contract_wallet_id", alternate = "contractWalletId")
	private NativeUInt32 contractWalletId;
	@SerializedName(value = "min_difficulty", alternate = "minDifficulty")
	private NativeUInt64 minDifficulty; // 10 difficulty is about 1 proof a day per k32 plot
	@SerializedName(value = "default_difficulty", alternate = "defaultDifficulty")
	private NativeUInt64 defaultDifficulty; // 10 difficulty is about 1 proof a day per k32 plot
	private String description, name;
	@SerializedName(value = "logo_url", alternate = "logoURL")
	private String logoURL;
	@SerializedName(value = "pool_contract_puzzle_hash", alternate = "poolContractPuzzleHash")
	private String poolContractPuzzleHash;
	@SerializedName(value = "pool_wallet_puzzle_hash", alternate = "poolWalletPuzzleHash")
	private String poolWalletPuzzleHash;
	@SerializedName(value = "full_node_host", alternate = "fullNodeHost")
	private String fullNodeHost;
	@SerializedName(value = "hot_wallet_host", alternate = "hotWalletHost")
	private String hotWalletHost;
	@SerializedName(value = "contract_wallet_host", alternate = "contractWalletHost")
	private String contractWalletHost;

	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLogoURL() {
		return logoURL;
	}

	public void setLogoURL(String logoURL) {
		this.logoURL = logoURL;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public String getFullNodeHost() {
		return fullNodeHost;
	}

	public void setFullNodeHost(String full_node_host) {
		this.fullNodeHost = full_node_host;
	}

	public String getHotWalletHost() {
		return hotWalletHost;
	}

	public void setHotWalletHost(String hotWalletHost) {
		this.hotWalletHost = hotWalletHost;
	}

	public String getContractWalletHost() {
		return contractWalletHost;
	}

	public void setContractWalletHost(String contractWalletHost) {
		this.contractWalletHost = contractWalletHost;
	}

	public int getHot_wallet_port() {
		return hotWalletPort;
	}

	public void setHot_wallet_port(int hot_wallet_port) {
		this.hotWalletPort = hot_wallet_port;
	}

	public int getContract_wallet_port() {
		return contractWalletPort;
	}

	public void setContract_wallet_port(int contract_wallet_port) {
		this.contractWalletPort = contract_wallet_port;
	}

	public String getPool_contract_puzzle_hash() {
		return poolContractPuzzleHash;
	}

	public void setPool_contract_puzzle_hash(String pool_contract_puzzle_hash) {
		this.poolContractPuzzleHash = pool_contract_puzzle_hash;
	}

	public String getPool_wallet_puzzle_hash() {
		return poolWalletPuzzleHash;
	}

	public void setPool_wallet_puzzle_hash(String pool_wallet_puzzle_hash) {
		this.poolWalletPuzzleHash = pool_wallet_puzzle_hash;
	}

	public NativeUInt32 getHot_wallet_fingerprint() {
		return hotWalletFingerprint;
	}

	public void setHot_wallet_fingerprint(NativeUInt32 hot_wallet_fingerprint) {
		this.hotWalletFingerprint = hot_wallet_fingerprint;
	}

	public NativeUInt32 getHot_wallet_id() {
		return hotWalletId;
	}

	public void setHot_wallet_id(NativeUInt32 hot_wallet_id) {
		this.hotWalletId = hot_wallet_id;
	}

	public NativeUInt32 getContract_wallet_fingerprint() {
		return contractWalletFingerprint;
	}

	public void setContract_wallet_fingerprint(NativeUInt32 contract_wallet_fingerprint) {
		this.contractWalletFingerprint = contract_wallet_fingerprint;
	}

	public NativeUInt32 getContract_wallet_id() {
		return contractWalletId;
	}

	public void setContract_wallet_id(NativeUInt32 contract_wallet_id) {
		this.contractWalletId = contract_wallet_id;
	}

	public NativeUInt32 getRelative_lock_height() {
		return relativeLockHeight;
	}

	public void setRelative_lock_height(NativeUInt32 relative_lock_height) {
		this.relativeLockHeight = relative_lock_height;
	}

	public NativeUInt64 getMin_difficulty() {
		return minDifficulty;
	}

	public void setMin_difficulty(NativeUInt64 min_difficulty) {
		this.minDifficulty = min_difficulty;
	}

	public NativeUInt64 getDefault_difficulty() {
		return defaultDifficulty;
	}

	public void setDefault_difficulty(NativeUInt64 default_difficulty) {
		this.defaultDifficulty = default_difficulty;
	}

	public NativeUInt8 getAuthentication_token_timeout() {
		return authenticationTokenTimeout;
	}

	public void setAuthentication_token_timeout(NativeUInt8 authentication_token_timeout) {
		this.authenticationTokenTimeout = authentication_token_timeout;
	}

	public int getFull_node_port() {
		return fullNodePort;
	}

	public void setFull_node_port(int full_node_port) {
		this.fullNodePort = full_node_port;
	}
}

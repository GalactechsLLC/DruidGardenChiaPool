package garden.druid.pool.types;

import garden.druid.chia.types.blockchain.CoinSpend;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes48;
import garden.druid.chia.types.ints.NativeUInt64;

public class FarmerRecord implements Comparable<FarmerRecord> {

	private Bytes32 launcherId, p2SingletonPuzzleHash, delayPuzzleHash;
	private String payoutInstructions;
	private Bytes48 authenticationPublicKey;
	private CoinSpend singletonTip;
	private PoolState singletonTipState;
	private NativeUInt64 delayTime, points, balance, difficulty;
	private boolean isPoolMember;

	public FarmerRecord(
			Bytes32 launcherId, 
			Bytes32 p2SingletonPuzzleHash,
			NativeUInt64 delayTime, 
			Bytes32 delayPuzzleHash, 
			Bytes48 authenticationPublicKey, 
			CoinSpend singletonTip, 
			PoolState singletonTipState, 
			NativeUInt64 points, 
			NativeUInt64 balance, 
			NativeUInt64 difficulty, 
			String payoutInstructions, 
			boolean isPoolMember) {
		this.launcherId = launcherId;
		this.p2SingletonPuzzleHash = p2SingletonPuzzleHash;
		this.delayTime = delayTime;
		this.delayPuzzleHash = delayPuzzleHash;
		this.authenticationPublicKey = authenticationPublicKey;
		this.singletonTip = singletonTip;
		this.singletonTipState = singletonTipState;
		this.balance = points;
		this.points = points;
		this.difficulty = difficulty;
		this.payoutInstructions = payoutInstructions;
		this.isPoolMember = isPoolMember;
	}

	public FarmerRecord() {
		
	}

	public Bytes32 getLauncherId() {
		return launcherId;
	}

	public void setLauncherId(Bytes32 launcherId) {
		this.launcherId = launcherId;
	}

	public Bytes32 getP2SingletonPuzzleHash() {
		return p2SingletonPuzzleHash;
	}

	public void setP2SingletonPuzzleHash(Bytes32 p2SingletonPuzzleHash) {
		this.p2SingletonPuzzleHash = p2SingletonPuzzleHash;
	}

	public Bytes32 getDelayPuzzleHash() {
		return delayPuzzleHash;
	}

	public void setDelayPuzzleHash(Bytes32 delayPuzzleHash) {
		this.delayPuzzleHash = delayPuzzleHash;
	}

	public String getPayoutInstructions() {
		return payoutInstructions;
	}

	public void setPayoutInstructions(String payoutInstructions) {
		this.payoutInstructions = payoutInstructions;
	}

	public Bytes48 getAuthenticationPublicKey() {
		return authenticationPublicKey;
	}

	public void setAuthenticationPublicKey(Bytes48 authenticationPublicKey) {
		this.authenticationPublicKey = authenticationPublicKey;
	}

	public CoinSpend getSingletonTip() {
		return singletonTip;
	}

	public void setSingletonTip(CoinSpend singletonTip) {
		this.singletonTip = singletonTip;
	}

	public PoolState getSingletonTipState() {
		return singletonTipState;
	}

	public void setSingletonTipState(PoolState singletonTipState) {
		this.singletonTipState = singletonTipState;
	}

	public NativeUInt64 getDelayTime() {
		return delayTime;
	}

	public void setDelayTime(NativeUInt64 delayTime) {
		this.delayTime = delayTime;
	}

	public NativeUInt64 getPoints() {
		return points;
	}

	public void setPoints(NativeUInt64 points) {
		this.points = points;
	}

	public NativeUInt64 getBalance() {
		return balance;
	}

	public void setBalance(NativeUInt64 balance) {
		this.balance = balance;
	}

	public NativeUInt64 getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(NativeUInt64 difficulty) {
		this.difficulty = difficulty;
	}

	public boolean isPoolMember() {
		return isPoolMember;
	}

	public void setPoolMember(boolean isPoolMember) {
		this.isPoolMember = isPoolMember;
	}
	
	@Override
	public int compareTo(FarmerRecord o) {
		return this.launcherId.compareTo(o.getLauncherId());
	}

	@Override
	public boolean equals(Object o) {
		return this.toString().compareTo(o.toString()) == 0 && o instanceof FarmerRecord;
	}
}

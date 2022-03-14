package garden.druid.pool.types;

import java.util.ArrayList;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;

public class PublicFarmerData {
	private Bytes32 launcherID;
	private NativeUInt64 points, totalPoints, balance, totalPaid, difficulty, estimatedSpace, joined;
	private ArrayList<RecentBlock> blockHistory;
	private boolean poolMember;
	
	public Bytes32 getLauncherID() {
		return launcherID;
	}
	public void setLauncherID(Bytes32 launcherID) {
		this.launcherID = launcherID;
	}
	public NativeUInt64 getPoints() {
		return points;
	}
	public void setPoints(NativeUInt64 points) {
		this.points = points;
	}
	public NativeUInt64 getTotalPoints() {
		return totalPoints;
	}
	public void setTotalPoints(NativeUInt64 totalPoints) {
		this.totalPoints = totalPoints;
	}
	public NativeUInt64 getBalance() {
		return balance;
	}
	public void setBalance(NativeUInt64 balance) {
		this.balance = balance;
	}
	public NativeUInt64 getTotalPaid() {
		return totalPaid;
	}
	public void setTotalPaid(NativeUInt64 totalPaid) {
		this.totalPaid = totalPaid;
	}
	public NativeUInt64 getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(NativeUInt64 difficulty) {
		this.difficulty = difficulty;
	}
	public NativeUInt64 getEstimatedSpace() {
		return estimatedSpace;
	}
	public void setEstimatedSpace(NativeUInt64 estimatedSpace) {
		this.estimatedSpace = estimatedSpace;
	}
	public NativeUInt64 getJoined() {
		return joined;
	}
	public void setJoined(NativeUInt64 joined) {
		this.joined = joined;
	}
	public ArrayList<RecentBlock> getBlockHistory() {
		return blockHistory;
	}
	public void setBlockHistory(ArrayList<RecentBlock> blockHistory) {
		this.blockHistory = blockHistory;
	}
	public boolean isPoolMember() {
		return poolMember;
	}
	public void setPoolMember(boolean poolMember) {
		this.poolMember = poolMember;
	}
}

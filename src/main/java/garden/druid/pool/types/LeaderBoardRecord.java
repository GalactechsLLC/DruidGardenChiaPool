package garden.druid.pool.types;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;

public class LeaderBoardRecord {
	
	private Bytes32 launcherId;
	private NativeUInt64 minTimeStamp, maxTimeStamp, points, totalPoints, difficulty, estimatedSpace, estimatedPartials, joined;
	
	public Bytes32 getLauncherId() {
		return launcherId;
	}
	public void setLauncherId(Bytes32 launcherId) {
		this.launcherId = launcherId;
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
	public NativeUInt64 getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(NativeUInt64 difficulty) {
		this.difficulty = difficulty;
	}
	public NativeUInt64 getMinTimeStamp() {
		return minTimeStamp;
	}
	public void setMinTimeStamp(NativeUInt64 minTimeStamp) {
		this.minTimeStamp = minTimeStamp;
	}
	public NativeUInt64 getMaxTimeStamp() {
		return maxTimeStamp;
	}
	public void setMaxTimeStamp(NativeUInt64 maxTimeStamp) {
		this.maxTimeStamp = maxTimeStamp;
	}
	public NativeUInt64 getEstimatedSpace() {
		return estimatedSpace;
	}
	public void setEstimatedSpace(NativeUInt64 estimatedSpace) {
		this.estimatedSpace = estimatedSpace;
	}
	public NativeUInt64 getEstimatedPartials() {
		return estimatedPartials;
	}
	public void setEstimatedPartials(NativeUInt64 estimatedPartials) {
		this.estimatedPartials = estimatedPartials;
	}
	public NativeUInt64 getJoined() {
		return joined;
	}
	public void setJoined(NativeUInt64 joined) {
		this.joined = joined;
	}
}

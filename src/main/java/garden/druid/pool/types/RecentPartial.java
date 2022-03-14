package garden.druid.pool.types;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;

public class RecentPartial {
	
	private Bytes32 launcherId;
	private NativeUInt64 timestamp, difficulty;
	
	public Bytes32 getLauncherId() {
		return launcherId;
	}
	public void setLauncherId(Bytes32 launcherId) {
		this.launcherId = launcherId;
	}
	public NativeUInt64 getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(NativeUInt64 timestamp) {
		this.timestamp = timestamp;
	}
	public NativeUInt64 getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(NativeUInt64 difficulty) {
		this.difficulty = difficulty;
	}
}

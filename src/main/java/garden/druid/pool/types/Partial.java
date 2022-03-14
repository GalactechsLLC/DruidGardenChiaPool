package garden.druid.pool.types;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt64;

public class Partial {

	private Bytes32 launcherId, harvesterId;
	private NativeUInt64 timestamp, difficulty;
	private String status;
	
	transient private PendingPartial request;

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

	public Bytes32 getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(Bytes32 harvesterId) {
		this.harvesterId = harvesterId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public PendingPartial getRequest() {
		return request;
	}

	public void setRequest(PendingPartial request) {
		this.request = request;
	}
}

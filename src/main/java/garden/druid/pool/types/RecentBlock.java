package garden.druid.pool.types;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;

public class RecentBlock {
	
	private Bytes32 launcherID;
	private Bytes32 coin;
	private NativeUInt32 height;
	private NativeUInt64 amount;
	private NativeUInt64 timestamp;
	
	public Bytes32 getLauncherID() {
		return launcherID;
	}
	public void setLauncherID(Bytes32 launcherID) {
		this.launcherID = launcherID;
	}
	public Bytes32 getCoin() {
		return coin;
	}
	public void setCoin(Bytes32 coin) {
		this.coin = coin;
	}
	public NativeUInt32 getHeight() {
		return height;
	}
	public void setHeight(NativeUInt32 height) {
		this.height = height;
	}
	public NativeUInt64 getAmount() {
		return amount;
	}
	public void setAmount(NativeUInt64 amount) {
		this.amount = amount;
	}
	public NativeUInt64 getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(NativeUInt64 timestamp) {
		this.timestamp = timestamp;
	}
}

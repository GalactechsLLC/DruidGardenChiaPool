package garden.druid.pool.types;

import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes48;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt8;

public class PoolState {

	private NativeUInt8 version, state;
	private Bytes32 targetPuzzleHash;
	private Bytes48 ownerPubkey;
	private String poolUrl;
	private NativeUInt32 relativeLockHeight;

	public NativeUInt8 getVersion() {
		return version;
	}

	public void setVersion(NativeUInt8 version) {
		this.version = version;
	}

	public NativeUInt8 getState() {
		return state;
	}

	public void setState(NativeUInt8 state) {
		this.state = state;
	}

	public Bytes32 getTargetPuzzleHash() {
		return targetPuzzleHash;
	}

	public void setTargetPuzzleHash(Bytes32 targetPuzzleHash) {
		this.targetPuzzleHash = targetPuzzleHash;
	}

	public Bytes48 getOwnerPubkey() {
		return ownerPubkey;
	}

	public void setOwnerPubkey(Bytes48 ownerPubkey) {
		this.ownerPubkey = ownerPubkey;
	}

	public String getPoolUrl() {
		return poolUrl;
	}

	public void setPoolUrl(String poolUrl) {
		this.poolUrl = poolUrl;
	}

	public NativeUInt32 getRelativeLockHeight() {
		return relativeLockHeight;
	}

	public void setRelativeLockHeight(NativeUInt32 relativeLockHeight) {
		this.relativeLockHeight = relativeLockHeight;
	}

	@Override
	public boolean equals(Object o) {
		PoolState other = null;
		if (o instanceof PoolState) {
			other = (PoolState) o;
		} else {
			return false;
		}
		if (!this.getVersion().toString().equals(other.getVersion().toString())) {
			return false;
		}
		if (!this.getState().toString().equals(other.getState().toString())) {
			return false;
		}
		if (!this.getTargetPuzzleHash().toString().equals(other.getTargetPuzzleHash().toString())) {
			return false;
		}
		if (!this.getOwnerPubkey().toString().equals(other.getOwnerPubkey().toString())) {
			return false;
		}
		if (!this.getPoolUrl().toString().equals(other.getPoolUrl().toString())) {
			return false;
		}
		if (!this.getRelativeLockHeight().toString().equals(other.getRelativeLockHeight().toString())) {
			return false;
		}
		return true;
	}
}

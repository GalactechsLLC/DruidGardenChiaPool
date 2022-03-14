package garden.druid.pool.endpoints.chiaAPI.structs;

import java.nio.ByteBuffer;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.crypt.sha.SHA;
import garden.druid.chia.types.blockchain.ProofOfSpace;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.interfaces.Streamable;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.chia.types.ints.NativeUInt8;

public class PostPartialPayload implements Streamable {
	@SerializedName(value = "launcher_id", alternate = "launcherID")
	private Bytes32 launcherID;
	@SerializedName(value = "sp_hash", alternate = "spHash")
	private Bytes32 spHash;
	@SerializedName(value = "harvesterId", alternate = "harvesterID")
	private Bytes32 harvesterID;
	@SerializedName(value = "authentication_token", alternate = "authenticationToken")
	private NativeUInt64 authenticationToken;
	@SerializedName(value = "proof_of_space", alternate = "proofOfSpace")
	private ProofOfSpace proofOfSpace;
	@SerializedName(value = "end_of_sub_slot", alternate = "endOfSubSlot")
	private boolean endOfSubSlot;

	public Bytes32 getLauncherID() {
		return launcherID;
	}

	public void setLauncherID(Bytes32 launcherID) {
		this.launcherID = launcherID;
	}

	public Bytes32 getSpHash() {
		return spHash;
	}

	public void setSpHash(Bytes32 sp_hash) {
		this.spHash = sp_hash;
	}

	public Bytes32 getHarvesterID() {
		return harvesterID;
	}

	public void setHarvesterID(Bytes32 harvesterID) {
		this.harvesterID = harvesterID;
	}

	public NativeUInt64 getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(NativeUInt64 authenticationToken) {
		this.authenticationToken = authenticationToken;
	}

	public ProofOfSpace getProofOfSpace() {
		return proofOfSpace;
	}

	public void setProofOfSpace(ProofOfSpace proofOfSpace) {
		this.proofOfSpace = proofOfSpace;
	}

	public boolean isEndOfSubSlot() {
		return endOfSubSlot;
	}

	public void setEndOfSubSlot(boolean endOfSubSlot) {
		this.endOfSubSlot = endOfSubSlot;
	}

	@Override
	public Bytes32 hash() {
		ByteBuffer byteBuf = ByteBuffer.allocate(2048);
		byteBuf.put(this.getLauncherID().getBytes());
		byteBuf.put(this.getAuthenticationToken().toByteArray());
		// PROOF OF SPACE
		byteBuf.put(this.getProofOfSpace().getChallenge().getBytes());
		if (this.getProofOfSpace().getPoolPublicKey() != null) {
			byteBuf.put((byte) 1);
			byteBuf.put(this.getProofOfSpace().getPoolPublicKey().getBytes());
		} else {
			byteBuf.put((byte) 0);
		}
		if (this.getProofOfSpace().getPoolContractPuzzleHash() != null) {
			byteBuf.put((byte) 1);
			byteBuf.put(this.getProofOfSpace().getPoolContractPuzzleHash().getBytes());
		} else {
			byteBuf.put((byte) 0);
		}
		byteBuf.put(this.getProofOfSpace().getPlotPublicKey().getBytes());
		byteBuf.put(new NativeUInt8(this.getProofOfSpace().getSize()).toByteArray());
		byteBuf.put(new NativeUInt32(this.getProofOfSpace().getProof().getBytes().length).toByteArray());
		byteBuf.put(this.getProofOfSpace().getProof().getBytes());
		// Back to Partial
		byteBuf.put(this.getSpHash().getBytes());
		if (this.isEndOfSubSlot()) {
			byteBuf.put((byte) 1);
		} else {
			byteBuf.put((byte) 0);
		}
		byteBuf.put(this.getHarvesterID().getBytes());
		byteBuf.flip();
		byte[] objAry = new byte[byteBuf.limit()];
		byteBuf.get(objAry, 0, byteBuf.limit());
		return new Bytes32(SHA.hash256(objAry));
	}
}

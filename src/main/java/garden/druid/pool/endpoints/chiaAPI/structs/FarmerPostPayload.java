package garden.druid.pool.endpoints.chiaAPI.structs;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.crypt.sha.SHA;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes48;
import garden.druid.chia.types.interfaces.Streamable;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;

public class FarmerPostPayload implements Streamable {

	@SerializedName(value = "launcher_id", alternate = "launcherId")
	private Bytes32 launcherId;
	@SerializedName(value = "authentication_public_key", alternate = "authenticationPublicKey")
	private Bytes48 authenticationPublicKey;
	@SerializedName(value = "payout_instructions", alternate = "payoutInstructions")
	private String payoutInstructions;
	@SerializedName(value = "authentication_token", alternate = "authenticationToken")
	private NativeUInt64 authenticationToken;
	@SerializedName(value = "suggested_difficulty", alternate = "suggestedDifficulty")
	private NativeUInt64 suggestedDifficulty;

	public FarmerPostPayload(Bytes32 launcherId, NativeUInt64 authenticationToken, Bytes48 authenticationPublicKey, String payoutInstructions, NativeUInt64 suggestedDifficulty) {
		this.launcherId = launcherId;
		this.authenticationPublicKey = authenticationPublicKey;
		this.payoutInstructions = payoutInstructions;
		this.authenticationToken = authenticationToken;
		this.suggestedDifficulty = suggestedDifficulty;
	}

	public Bytes32 getLauncherId() {
		return launcherId;
	}

	public void setLauncherId(Bytes32 launcherId) {
		this.launcherId = launcherId;
	}

	public Bytes48 getAuthenticationPublicKey() {
		return authenticationPublicKey;
	}

	public void setAuthenticationPublickey(Bytes48 authenticationPublicKey) {
		this.authenticationPublicKey = authenticationPublicKey;
	}

	public String getPayoutInstructions() {
		return payoutInstructions;
	}

	public void setPayoutInstructions(String payoutInstructions) {
		this.payoutInstructions = payoutInstructions;
	}

	public NativeUInt64 getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(NativeUInt64 authenticationToken) {
		this.authenticationToken = authenticationToken;
	}

	public NativeUInt64 getSuggestedDifficulty() {
		return suggestedDifficulty;
	}

	public void setSuggestedDifficulty(NativeUInt64 suggestedDifficulty) {
		this.suggestedDifficulty = suggestedDifficulty;
	}

	@Override
	public Bytes32 hash() {
		ByteBuffer byteBuf = ByteBuffer.allocate(2048);
		byteBuf.put(this.getLauncherId().getBytes());
		byteBuf.put(this.getAuthenticationToken().toByteArray());
		byteBuf.put(this.getAuthenticationPublicKey().getBytes());
		byte[] decoded = this.getPayoutInstructions().getBytes(StandardCharsets.UTF_8);
		byteBuf.put(new NativeUInt32(decoded.length).toByteArray());
		byteBuf.put(decoded);
		if (this.getSuggestedDifficulty() != null) {
			byteBuf.put((byte) 1);
			byteBuf.put(this.getSuggestedDifficulty().toByteArray());
		} else {
			byteBuf.put((byte) 0);
		}
		byteBuf.flip();
		byte[] objAry = new byte[byteBuf.limit()];
		byteBuf.get(objAry, 0, byteBuf.limit());
		return new Bytes32(SHA.hash256(objAry));
	}
}

package garden.druid.pool.endpoints.chiaAPI.structs;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.crypt.sha.SHA;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.interfaces.Streamable;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;

public class AuthenticationPayload implements Streamable {

	private String method;
	@SerializedName(value = "launcher_id", alternate = "launcherID")
	private Bytes32 launcherID;
	@SerializedName(value = "default_target_puzzle_hash", alternate = "defaultTargetPuzzleHash")
	private Bytes32 defaultTargetPuzzleHash;
	@SerializedName(value = "authentication_token", alternate = "authenticationToken")
	private NativeUInt64 authenticationToken;

	public AuthenticationPayload(String method, Bytes32 launcherID, Bytes32 defaultTargetPuzzleHash, NativeUInt64 authenticationToken) {
		super();
		this.method = method;
		this.launcherID = launcherID;
		this.defaultTargetPuzzleHash = defaultTargetPuzzleHash;
		this.authenticationToken = authenticationToken;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Bytes32 getLauncherID() {
		return launcherID;
	}

	public void setLauncherID(Bytes32 launcher_id) {
		this.launcherID = launcher_id;
	}

	public Bytes32 getDefaultTargetPuzzleHash() {
		return defaultTargetPuzzleHash;
	}

	public void setDefaultTargetPuzzleHash(Bytes32 default_target_puzzle_hash) {
		this.defaultTargetPuzzleHash = default_target_puzzle_hash;
	}

	public NativeUInt64 getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(NativeUInt64 authentication_token) {
		this.authenticationToken = authentication_token;
	}

	@Override
	public Bytes32 hash() {
		ByteBuffer byteBuf = ByteBuffer.allocate(2048);
		byteBuf.put(new NativeUInt32(this.getMethod().length()).toByteArray());
		byteBuf.put(this.getMethod().getBytes(StandardCharsets.UTF_8));
		byteBuf.put(this.getLauncherID().getBytes());
		byteBuf.put(this.getDefaultTargetPuzzleHash().getBytes());
		byteBuf.put(this.getAuthenticationToken().toByteArray());
		byteBuf.flip();
		byte[] objAry = new byte[byteBuf.limit()];
		byteBuf.get(objAry, 0, byteBuf.limit());
		return new Bytes32(SHA.hash256(objAry));
	}
}

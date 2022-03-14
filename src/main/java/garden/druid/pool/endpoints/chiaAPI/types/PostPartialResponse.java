package garden.druid.pool.endpoints.chiaAPI.types;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.types.ints.NativeUInt64;

public class PostPartialResponse {
	@SerializedName(value = "new_difficulty", alternate = "newDifficulty")
	private NativeUInt64 newDifficulty;

	public PostPartialResponse(NativeUInt64 new_difficulty) {
		this.newDifficulty = new_difficulty;
	}

	public NativeUInt64 getNew_difficulty() {
		return newDifficulty;
	}

	public void setNew_difficulty(NativeUInt64 new_difficulty) {
		this.newDifficulty = new_difficulty;
	}
}

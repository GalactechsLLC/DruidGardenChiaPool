package garden.druid.pool.endpoints.chiaAPI.structs;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.types.ints.NativeUInt64;

public class PostPartialResponse {
	@SerializedName(value = "new_difficulty", alternate = "newDifficulty")
	private NativeUInt64 newDifficulty;

	public PostPartialResponse(NativeUInt64 newDifficulty) {
		this.newDifficulty = newDifficulty;
	}

	public NativeUInt64 getNewDifficulty() {
		return newDifficulty;
	}

	public void setNewDifficulty(NativeUInt64 newDifficulty) {
		this.newDifficulty = newDifficulty;
	}
}

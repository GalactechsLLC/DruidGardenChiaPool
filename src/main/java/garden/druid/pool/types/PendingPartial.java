package garden.druid.pool.types;

import java.time.Instant;

import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialRequest;

public class PendingPartial {

	private PostPartialRequest request;
	private Instant startTime;
	private NativeUInt64 currentDifficulty;

	public PendingPartial(PostPartialRequest request, Instant startTime, NativeUInt64 currentDifficulty) {
		this.request = request;
		this.startTime = startTime;
		this.currentDifficulty = currentDifficulty;
	}

	public PostPartialRequest getRequest() {
		return request;
	}

	public void setRequest(PostPartialRequest request) {
		this.request = request;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public NativeUInt64 getCurrentDifficulty() {
		return currentDifficulty;
	}

	public void setCurrentDifficulty(NativeUInt64 currentDifficulty) {
		this.currentDifficulty = currentDifficulty;
	}
}

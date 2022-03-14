package garden.druid.pool.endpoints.chiaAPI.types;

import com.google.gson.annotations.SerializedName;

import garden.druid.chia.types.bytes.Bytes96;

public class PostPartialRequest {
	private PostPartialPayload payload;
	@SerializedName(value = "aggregate_signature", alternate = "aggregateSignature")
	private Bytes96 aggregateSignature;

	public PostPartialPayload getPayload() {
		return payload;
	}

	public void setPayload(PostPartialPayload payload) {
		this.payload = payload;
	}

	public Bytes96 getAggregate_signature() {
		return aggregateSignature;
	}

	public void setAggregate_signature(Bytes96 aggregate_signature) {
		this.aggregateSignature = aggregate_signature;
	}
}

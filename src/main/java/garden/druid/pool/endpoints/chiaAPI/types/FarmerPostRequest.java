package garden.druid.pool.endpoints.chiaAPI.types;

import garden.druid.chia.types.bytes.Bytes96;

public class FarmerPostRequest {

	private FarmerPostPayload payload;
	private Bytes96 signature;

	public FarmerPostPayload getPayload() {
		return payload;
	}

	public void setPayload(FarmerPostPayload payload) {
		this.payload = payload;
	}

	public Bytes96 getSignature() {
		return signature;
	}

	public void setSignature(Bytes96 signature) {
		this.signature = signature;
	}
}

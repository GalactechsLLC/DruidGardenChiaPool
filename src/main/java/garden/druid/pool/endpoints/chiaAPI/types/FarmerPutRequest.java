package garden.druid.pool.endpoints.chiaAPI.types;

import garden.druid.chia.types.bytes.Bytes96;

public class FarmerPutRequest {
	private FarmerPutPayload payload;
	private Bytes96 signature;

	public FarmerPutPayload getPayload() {
		return payload;
	}

	public void setPayload(FarmerPutPayload payload) {
		this.payload = payload;
	}

	public Bytes96 getSignature() {
		return signature;
	}

	public void setSignature(Bytes96 signature) {
		this.signature = signature;
	}
}

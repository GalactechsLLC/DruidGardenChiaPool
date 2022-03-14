package garden.druid.pool.types;

import garden.druid.chia.types.ints.NativeUInt64;

import garden.druid.chia.types.bytes.Bytes32;

public class FarmerPayoutIntructions {
	
	private String payoutIntrstructions;
	private NativeUInt64 amount;
	private Bytes32 launcherId;
	
	public String getPayoutIntrstructions() {
		return payoutIntrstructions;
	}
	public void setPayoutIntrstructions(String payoutIntrstructions) {
		this.payoutIntrstructions = payoutIntrstructions;
	}
	public NativeUInt64 getAmount() {
		return amount;
	}
	public void setAmount(NativeUInt64 amount) {
		this.amount = amount;
	}
	public Bytes32 getLauncherId() {
		return launcherId;
	}
	public void setLauncherId(Bytes32 launcherId) {
		this.launcherId = launcherId;
	}
}

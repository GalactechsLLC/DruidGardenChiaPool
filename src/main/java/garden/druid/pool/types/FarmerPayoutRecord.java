package garden.druid.pool.types;

public class FarmerPayoutRecord {

	private FarmerRecord farmerRecord;
	private String payoutInstructions;

	public FarmerPayoutRecord(String payoutInstructions, FarmerRecord farmerRecord) {
		this.payoutInstructions = payoutInstructions;
		this.farmerRecord = farmerRecord;
	}

	public FarmerRecord getFarmerRecord() {
		return farmerRecord;
	}

	public void setFarmerRecord(FarmerRecord farmerRecord) {
		this.farmerRecord = farmerRecord;
	}

	public String getPayoutInstructions() {
		return payoutInstructions;
	}

	public void setPayoutInstructions(String payoutInstructions) {
		this.payoutInstructions = payoutInstructions;
	}

}

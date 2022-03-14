package garden.druid.pool.types;

import garden.druid.chia.types.ints.NativeUInt64;

public class PendingPayment {

	private String puzzleHash;
	private NativeUInt64 amount;

	public PendingPayment(String puzzleHash, NativeUInt64 amount) {
		this.puzzleHash = puzzleHash;
		this.amount = amount;
	}

	public String getPuzzleHash() {
		return puzzleHash;
	}

	public void setPuzzleHash(String puzzleHash) {
		this.puzzleHash = puzzleHash;
	}

	public NativeUInt64 getAmount() {
		return amount;
	}

	public void setAmount(NativeUInt64 amount) {
		this.amount = amount;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("\"puzzle_hash\" : \"");
		builder.append(this.puzzleHash);
		builder.append("\", \"amount\", ");
		builder.append(this.amount);
		builder.append("\"}");
		return builder.toString();
	}

}

package garden.druid.pool.types;

import com.google.gson.annotations.SerializedName;

public class PoolError {

	public static PoolError SIGNAGE_POINT_REVERTED = PoolError.getInstance(0x01, "The provided signage point has been reverted");
	public static PoolError TOO_LATE = PoolError.getInstance(0x02, "Received partial too late");
	public static PoolError NOT_FOUND = PoolError.getInstance(0x03, "Not found");
	public static PoolError INVALID_PROOF_OF_SPACE = PoolError.getInstance(0x04, "Proof of space invalid");
	public static PoolError BAD_PROOF_OF_SPACE = PoolError.getInstance(0x05, "Proof of space not good enough");
	public static PoolError INVALID_DIFFICULTY = PoolError.getInstance(0x06, "Invalid difficulty");
	public static PoolError INVALID_SIGNATURE = PoolError.getInstance(0x07, "Invalid signature");
	public static PoolError SERVER_EXPECTION = PoolError.getInstance(0x08, "Web-Server raised an exception");
	public static PoolError INVALID_PUZZLE_HASH = PoolError.getInstance(0x09, "Invalid puzzle hash");
	public static PoolError FARMER_NOT_KNOWN = PoolError.getInstance(0x0A, "Farmer not known");
	public static PoolError FARMER_ALREADY_KNOWN = PoolError.getInstance(0x0B, "Farmer already known");
	public static PoolError INVALID_AUTHENTICATION_TOKEN = PoolError.getInstance(0x0C, "Invalid authentication token");
	public static PoolError INVALID_PAYOUT_INSTRUCTIONS = PoolError.getInstance(0x0D, "Invalid payout instructions");
	public static PoolError INVALID_SINGLETON = PoolError.getInstance(0x0E, "Invalid singleton");
	public static PoolError DELAY_TOO_SHORT = PoolError.getInstance(0x0F, "Delay time too short");
	public static PoolError REQUEST_FAILED = PoolError.getInstance(0x10, "Request failed");

	@SerializedName(value = "error_code", alternate = "errorCode")
	private int errorCode;
	@SerializedName(value = "error_message", alternate = "errorMessage")
	private String errorMessage;

	private PoolError(int errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	private static PoolError getInstance(int errorCode, String errorMessage) {
		return new PoolError(errorCode, errorMessage);
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}

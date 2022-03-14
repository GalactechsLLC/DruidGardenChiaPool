package garden.druid.pool.concurrent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import garden.druid.base.cache.Cache;
import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedCallable;
import garden.druid.chia.crypt.bls_blst_bindings.BLS;
import garden.druid.chia.crypt.sha.SHA;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.SignagePoint;
import garden.druid.chia.types.blockchain.SignagePointOrEOS;
import garden.druid.chia.types.blockchain.SubSlotBundle;
import garden.druid.chia.types.bytes.Bytes;
import garden.druid.chia.types.bytes.Bytes32;
import garden.druid.chia.types.bytes.Bytes48;
import garden.druid.chia.types.ints.NativeUInt128;
import garden.druid.chia.types.ints.NativeUInt256;
import garden.druid.chia.types.ints.NativeUInt32;
import garden.druid.chia.types.ints.NativeUInt64;
import garden.druid.pool.Pool;
import garden.druid.pool.database.PoolPartialsDAO;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialRequest;
import garden.druid.pool.endpoints.chiaAPI.types.PostPartialResponse;
import garden.druid.pool.types.FarmerRecord;
import garden.druid.pool.types.PendingPartial;
import garden.druid.pool.types.PoolError;
import garden.druid.pool.types.RecentPartial;

public class PartialHandler extends ManagedCallable<PostPartialResponse> {

	private static final long serialVersionUID = -6554137614811560794L;
	private static final NativeUInt256 TWO_POW_256 = new NativeUInt256(BigInteger.valueOf(2l).pow(256));
	private transient final PostPartialRequest request;
	@SerializedName(value = "farmer_record", alternate = "farmerRecord")
	private transient final FarmerRecord farmerRecord;
	@SerializedName(value = "start_time", alternate = "startTime")
	private transient final Instant startTime;
	private transient PoolError error = null;
	private transient static Gson gson = new GsonBuilder().serializeNulls().create();
	private transient static Cache<Bytes32, SignagePointOrEOS> spCache = new Cache<Bytes32, SignagePointOrEOS>(128);

	public PartialHandler(PostPartialRequest request, FarmerRecord farmerRecord, Instant startTime) {
		this.request = request;
		this.farmerRecord = farmerRecord;
		this.startTime = startTime;
		this.uuid = UUID.randomUUID().toString();
		this.name = "PartialHandler";
	}

	@Override
	public PostPartialResponse call() {
		this.started = true;
		try {
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			Bytes48 partialPubKey = request.getPayload().getProofOfSpace().getPlotPublicKey();
			Bytes48 farmerRecordPubKey = farmerRecord.getAuthenticationPublicKey();
			String message = request.getPayload().hash().toString();
			ArrayList<Bytes48> keys = new ArrayList<Bytes48>();
			keys.add(partialPubKey);
			keys.add(farmerRecordPubKey);
			ArrayList<byte[]> messages = new ArrayList<byte[]>();
			messages.add(Bytes.parseHexBinary(message));
			messages.add(Bytes.parseHexBinary(message));
			boolean isSigValid = BLS.aggregateVerifySignature(keys, messages, request.getAggregate_signature());
			if (isSigValid == false) {
				this.error = PoolError.INVALID_SIGNATURE;
				this.error.setErrorMessage("The aggregate signature is invalid " + request.getAggregate_signature());
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			if (request.getPayload().getProofOfSpace().getPoolContractPuzzleHash() == null || !request.getPayload().getProofOfSpace().getPoolContractPuzzleHash().equals(farmerRecord.getP2SingletonPuzzleHash())) {
				this.error = PoolError.INVALID_PUZZLE_HASH;
				this.error.setErrorMessage("Invalid pool contract puzzle hash " + request.getPayload().getProofOfSpace().getPoolContractPuzzleHash());
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			SignagePointOrEOS signagePointOrEOS = spCache.get(request.getPayload().getSpHash());
			if(signagePointOrEOS == null) {
				if (request.getPayload().isEndOfSubSlot()) {
					signagePointOrEOS = client.get_recent_signage_point_or_eos(null, request.getPayload().getSpHash());
				} else {
					signagePointOrEOS = client.get_recent_signage_point_or_eos(request.getPayload().getSpHash(), null);
				}
				if (signagePointOrEOS.getSignagePoint() == null && signagePointOrEOS.getEos() == null) {
					// Try again after 10 seconds in case we just didn't yet receive the signage point
					try {
						Thread.sleep(10000);
					} catch (Exception e) {
					}
					if (request.getPayload().isEndOfSubSlot()) {
						signagePointOrEOS = client.get_recent_signage_point_or_eos(null, request.getPayload().getSpHash());
					} else {
						signagePointOrEOS = client.get_recent_signage_point_or_eos(request.getPayload().getSpHash(), null);
					}
				}
			}
			if (signagePointOrEOS == null || signagePointOrEOS.isReverted() == true || (signagePointOrEOS.getSignagePoint() == null && signagePointOrEOS.getEos() == null)) {
				this.error = PoolError.NOT_FOUND;
				this.error.setErrorMessage("Did not find signage point or EOS " + request.getPayload().getSpHash() + " , " + gson.toJson(signagePointOrEOS));
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			} else {
				spCache.put(request.getPayload().getSpHash(), signagePointOrEOS);
			}
			BigDecimal nodeTimeReceivedSP = signagePointOrEOS.getTimeReceived();
			SignagePoint signagePoint = signagePointOrEOS.getSignagePoint();
			SubSlotBundle endOfSubSlot = signagePointOrEOS.getEos();			
			if (startTime.getEpochSecond() - nodeTimeReceivedSP.longValue() > Pool.getInstance().getPoolSettings().getPartial_time_limit()) {
				this.error = PoolError.TOO_LATE;
				this.error.setErrorMessage("Received partial in " + (startTime.getEpochSecond() - nodeTimeReceivedSP.longValue()) + ". " 
						+ "Make sure your proof of space lookups are fast, and network connectivity is good. " 
						+ "Response must happen in less than " + Pool.getInstance().getPoolSettings().getPartial_time_limit()
						+ " seconds. NAS or network" + " farming can be an issue");
				Logger.getInstance().log(Level.WARNING, "Slow partial for Farmer: " + request.getPayload().getLauncherId());
				return null;
			}
			Bytes32 challengeHash = null;
			if (signagePoint != null) {
				challengeHash = signagePoint.getCcVdf().getChallenge();
			} else if (endOfSubSlot != null) {
				challengeHash = endOfSubSlot.getChallengeChain().hash();
			} else {
				this.error = PoolError.NOT_FOUND;
				this.error.setErrorMessage("Did not find signage point or EOS " + request.getPayload().getSpHash() + " , " + gson.toJson(signagePointOrEOS));
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			Bytes32 quality = request.getPayload().getProofOfSpace().verifyAndGetQualityString(challengeHash, request.getPayload().getSpHash());
			if (quality == null) {
				this.error = PoolError.INVALID_PROOF_OF_SPACE;
				this.error.setErrorMessage("Invalid proof of space " + request.getPayload().getSpHash());
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			NativeUInt64 currentDifficulty = farmerRecord.getDifficulty();
			NativeUInt64 requiredIters = calculateIterationsQuality(
					Pool.getInstance().getPoolSettings().isIs_testnet() ? Pool.TESTNET_DIFFICULTY_CONSTANT_FACTOR : Pool.DIFFICULTY_CONSTANT_FACTOR, 
					quality, 
					request.getPayload().getProofOfSpace().getSize(), 
					currentDifficulty, 
					request.getPayload().getSpHash());
			if (requiredIters.compareTo(Pool.iters_limit) >= 0) {
				this.error = PoolError.BAD_PROOF_OF_SPACE;
				this.error.setErrorMessage("Proof of space has required iters " + requiredIters.toString() + ", too high for difficulty " + currentDifficulty.toString());
				Logger.getInstance().log(Level.WARNING, this.error.getErrorMessage());
				return null;
			}
			PendingPartial partial = new PendingPartial(request, startTime, currentDifficulty);
			PoolPartialsDAO.addPendingPartial(partial);

			//Not Sure I want dynamic difficulty, but uncomment to turn it back on 
//			synchronized (databaseLock) {
//				FarmerRecord farmer_record = PoolDAO.getFarmerRecord(request.getPayload().getLauncher_id());
//				if (farmer_record != null) {
//					current_difficulty = farmer_record.getDifficulty();
//					// # Decide whether to update the difficulty
//					ArrayList<RecentPartial> recent_partials = PoolDAO.getRecentPartials(request.getPayload().getLauncher_id(), garden.druid.pool.Pool.getInstance().getPoolInfo().getNumber_of_partials_target());
//					// # Only update the difficulty if we meet certain conditions
//					NativeUInt64 new_difficulty = Puzzles.getNewDifficulty(recent_partials, garden.druid.pool.Pool.getInstance().getPoolInfo().getNumber_of_partials_target(), garden.druid.pool.Pool.getInstance().getPoolInfo().getTime_target(), current_difficulty, start_time, garden.druid.pool.Pool.getInstance().getPoolInfo().getMin_difficulty());
//
//					if (current_difficulty.compareTo(new_difficulty) != 0) {
//						PoolDAO.updateDifficulty(request.getPayload().getLauncher_id(), new_difficulty);
//						current_difficulty = new_difficulty;
//					}
//				}
//			}
			return new PostPartialResponse(currentDifficulty);
		} catch (Exception e) {
			Logger.getInstance().log(Level.WARNING, "AccessDenied for Partialhandler.call", e);
			return null;
		} finally {
			this.done = true;
		}
	}
	
	private static NativeUInt64 calculateIterationsQuality(NativeUInt128 difficultyConstantFactor, Bytes32 qualityString, int size, NativeUInt64 difficulty, Bytes32 spHash) {
		/*
		 * Calculates the number of iterations from the quality. This is derives as the
		 * difficulty times the constant factor times a random number between 0 and 1
		 * (based on quality string), divided by plot size.
		 */
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put(qualityString.getBytes());
		buf.put(spHash.getBytes());
		buf.flip();
		byte[] output = new byte[buf.remaining()];
		buf.get(output, 0, buf.remaining());
		buf.clear();
		buf = null;
		Bytes32 sp_quality_string = new Bytes32(SHA.hash256(output));
		NativeUInt32 quality_int = new NativeUInt32(new BigInteger(1, sp_quality_string.getBytes()));
		NativeUInt64 step1 = difficulty.mul(difficultyConstantFactor);
		NativeUInt64 step2 = step1.mul(quality_int);
		NativeUInt256 step3 = TWO_POW_256.mul(expectedPlotSize(size));
		NativeUInt64 step4 = step2.div(step3);
		NativeUInt64 step5 = step4.max(BigInteger.ONE);
		return step5;
	}
	
	private static NativeUInt64 expectedPlotSize(int k) {
		return new NativeUInt64((2l * k) + 1l).mul(new NativeUInt64(2l).pow(k - 1));
	}
	
	public static NativeUInt64 getNewDifficulty(ArrayList<RecentPartial> recentPartials, int numberOfPartialsTarget, int timeTarget, NativeUInt64 currentDifficulty, Instant startTime, NativeUInt64 minDifficulty) {
		//If we haven't processed any partials yet, maintain the current (default) difficulty
		if (recentPartials.size() == 0) {
			return currentDifficulty;
		}
		//If we recently updated difficulty, don't update again
		for (RecentPartial partial : recentPartials) {
			if (partial.getDifficulty().compareTo(currentDifficulty) != 0) {
				return currentDifficulty;
			}
		}
		//Lower the difficulty if we are really slow since our last partial
		Instant lastTimestamp = Instant.ofEpochSecond(recentPartials.get(0).getTimestamp().longValue());
		if (startTime.getEpochSecond() - lastTimestamp.getEpochSecond() > 3 * 3600) {
			return minDifficulty.max(currentDifficulty.div(new NativeUInt64(5l)));
		}
		if (startTime.getEpochSecond() - lastTimestamp.getEpochSecond() > 3600) {
			return minDifficulty.max(currentDifficulty.mul(new NativeUInt64(3l).div(new NativeUInt64(2l))));
		}
		//If we don't have enough partials at this difficulty, don't update yet
		if (recentPartials.size() < numberOfPartialsTarget) {
			return currentDifficulty;
		}
		//Finally, this is the standard case of normal farming and slow (or no) growth, adjust to the new difficulty
		NativeUInt64 timeTaken = recentPartials.get(0).getTimestamp().sub(recentPartials.get(recentPartials.size() - 1).getTimestamp());
		NativeUInt64 newDifficulty = currentDifficulty.mul(new NativeUInt64(timeTarget)).div(timeTaken);
		return minDifficulty.max(newDifficulty);
	}
	
	public PoolError getError() {
		return this.error;
	}
}

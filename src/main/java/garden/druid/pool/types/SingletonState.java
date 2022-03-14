package garden.druid.pool.types;

import garden.druid.chia.types.blockchain.CoinSpend;

public class SingletonState {

	private CoinSpend savedSolution;
	private PoolState savedState;
	private PoolState lastNotNullState;

	public CoinSpend getSavedSolution() {
		return savedSolution;
	}

	public void setSaved_solution(CoinSpend savedSolution) {
		this.savedSolution = savedSolution;
	}

	public PoolState getSavedState() {
		return savedState;
	}

	public void setSavedState(PoolState savedState) {
		this.savedState = savedState;
	}

	public PoolState getLastNotNullState() {
		return lastNotNullState;
	}

	public void setLastNotNullState(PoolState lastNotNullState) {
		this.lastNotNullState = lastNotNullState;
	}

}

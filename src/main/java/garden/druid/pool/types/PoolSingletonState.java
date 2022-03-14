package garden.druid.pool.types;

public enum PoolSingletonState {
	SELF_POOLING(1), LEAVING_POOL(2), FARMING_TO_POOL(3);

	private int value;

	PoolSingletonState(int i) {
		this.value = i;
	}

	public int getValue() {
		return this.value;
	}
}

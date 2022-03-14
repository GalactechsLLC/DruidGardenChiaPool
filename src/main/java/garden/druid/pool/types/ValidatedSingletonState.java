package garden.druid.pool.types;

import garden.druid.chia.types.blockchain.CoinSpend;

public class ValidatedSingletonState {

	private boolean isPoolMember;
	private CoinSpend buriedSingletonTip;
	private PoolState buriedSingletonTipState;

	public CoinSpend getBuriedSingletonTip() {
		return buriedSingletonTip;
	}

	public void setBuriedSingletonTip(CoinSpend buriedSingletonTip) {
		this.buriedSingletonTip = buriedSingletonTip;
	}

	public PoolState getBuriedSingletonTipState() {
		return buriedSingletonTipState;
	}

	public void setBuriedSingletonTipState(PoolState buriedSingletonTipState) {
		this.buriedSingletonTipState = buriedSingletonTipState;
	}

	public boolean isPoolMember() {
		return isPoolMember;
	}

	public void setPoolMember(boolean isPoolMember) {
		this.isPoolMember = isPoolMember;
	}
}

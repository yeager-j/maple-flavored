package mapleflavored.trades;

/**
 * A villager told how many times over to reward the trade it is being notified of, since the paid book is already
 * consumed by then. Implemented by a mixin on {@code Villager}.
 */
public interface TradeMultiplierHolder {
	/** Sets the multiplier from {@link BookTrade#multiplier} for the trade being rewarded; 1 between trades. */
	void mapleflavored$setTradeMultiplier(int multiplier);
}

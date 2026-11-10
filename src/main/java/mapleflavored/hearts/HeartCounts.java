package mapleflavored.hearts;

/**
 * A player's hearts at one moment.
 *
 * @param maxHearts their Max Hearts, from 1 to 30
 * @param witheredHearts how many of those are withered, always leaving at least 1 usable
 */
public record HeartCounts(int maxHearts, int witheredHearts) {
	/** The Max Hearts that aren't withered, which set max health. */
	public int usableHearts() {
		return maxHearts - witheredHearts;
	}

	/** Whether the player is at 30 Max Hearts, so another Max Heart would be refused. */
	public boolean atMaxHeartsCap() {
		return maxHearts >= Hearts.MAX_HEARTS_CAP;
	}
}

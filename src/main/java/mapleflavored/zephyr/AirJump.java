package mapleflavored.zephyr;

/** A player's Zephyr jump in their current air time. No state means the jump is still available. */
sealed interface AirJump {
	/**
	 * The server accepted a jump and the player is still rising from it.
	 *
	 * @param jumpY where the jump started
	 * @param carriedFall the fall distance before the jump, after the Clutch
	 */
	record Rising(double jumpY, double carriedFall) implements AirJump {
	}

	/** The jump is used up until the air time ends. */
	record Spent() implements AirJump {
		static final Spent INSTANCE = new Spent();
	}
}

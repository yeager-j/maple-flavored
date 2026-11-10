package mapleflavored.hearts;

import java.util.Optional;
import mapleflavored.gamerules.MapleGameRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;

/**
 * What a death costs a player under the world's rules, decided once from Hardcore, the difficulty, their Milestones
 * and Keep Hearts.
 *
 * @param heartFloor the lowest death can take their Max Hearts: 10, less one per Milestone held on Normal and Hard
 * @param loss the Max Hearts a death takes while they are above the floor
 */
record DeathCost(int heartFloor, int loss) {
	private static final int HEART_FLOOR_WITHOUT_MILESTONES = 10;
	private static final int LOSS = 1;
	private static final int HARD_LOSS = 2;

	/** The cost of the player's next death, or empty in Hardcore, which keeps vanilla's death rule and has no Heart Floor. */
	static Optional<DeathCost> of(ServerPlayer player) {
		var level = player.level();

		return under(
			level.getServer().isHardcore(),
			level.getDifficulty(),
			Milestones.held(player),
			level.getGameRules().get(MapleGameRules.KEEP_HEARTS)
		);
	}

	private static Optional<DeathCost> under(boolean hardcore, Difficulty difficulty, int milestonesHeld, boolean keepHearts) {
		if (hardcore) return Optional.empty();

		boolean milestonesCount = difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;
		int heartFloor = HEART_FLOOR_WITHOUT_MILESTONES - (milestonesCount ? milestonesHeld : 0);
		int lossAboveFloor = difficulty == Difficulty.HARD ? HARD_LOSS : LOSS;

		return Optional.of(new DeathCost(heartFloor, keepHearts ? 0 : lossAboveFloor));
	}

	/** The Max Hearts left after one death. A player already at or below the floor loses none. */
	int maxHeartsAfterDeath(int maxHearts) {
		int stoppedAtFloor = Math.max(heartFloor, maxHearts - loss);

		return Math.min(maxHearts, stoppedAtFloor);
	}
}

package mapleflavored.experience;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** A player's XP counted in points rather than levels, the unit every Maple Flavored XP cost uses. */
public final class ExperiencePoints {
	// Reaching level 21,864 takes more than Integer.MAX_VALUE points.
	private static final int HIGHEST_LEVEL_IN_POINTS = 21_863;

	private ExperiencePoints() {
	}

	/**
	 * All the player's XP in points, from their level and progress. Works on either side.
	 *
	 * <p>Vanilla's {@code totalExperience} isn't used, since adding levels by command doesn't update it.
	 */
	public static int total(Player player) {
		// Commands allow levels whose points don't fit in an int, or whose square doesn't fit in a long.
		if (player.experienceLevel > HIGHEST_LEVEL_IN_POINTS) return Integer.MAX_VALUE;

		long pointsInLevel = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
		long points = pointsToReachLevel(player.experienceLevel) + pointsInLevel;

		return (int) Math.min(points, Integer.MAX_VALUE);
	}

	/** Sets the player's XP to exactly this many points, at least 0, and syncs it to their client. */
	public static void set(ServerPlayer player, int points) {
		int clampedPoints = Math.max(points, 0);
		int level = levelFor(clampedPoints);

		// The level goes first, since the points within it are measured against that level's size.
		player.setExperienceLevels(level);
		player.setExperiencePoints((int) (clampedPoints - pointsToReachLevel(level)));
		player.totalExperience = clampedPoints;
	}

	private static int levelFor(int points) {
		int level = 0;

		while (level < HIGHEST_LEVEL_IN_POINTS && pointsToReachLevel(level + 1) <= points) level++;

		return level;
	}

	// Closed forms of summing Player.getXpNeededForNextLevel over every lower level, halved last to stay in integers.
	private static long pointsToReachLevel(long level) {
		if (level <= 16) return level * level + 6 * level;
		if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;

		return (9 * level * level - 325 * level + 4440) / 2;
	}
}

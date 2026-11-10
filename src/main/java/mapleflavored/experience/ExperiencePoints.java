package mapleflavored.experience;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A player's XP counted in points rather than levels, the unit every Maple Flavored XP cost and transfer uses.
 *
 * <p>Points are exact: a write lands on the level and progress those points make, with none of the rounding vanilla's
 * level-by-level adding collects.
 */
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

	/**
	 * Gives the player exactly this many points on top of what they hold, up to {@link Integer#MAX_VALUE} in all.
	 * Unlike vanilla's {@code giveExperiencePoints}, it leaves their score alone.
	 *
	 * @param points at least 0
	 */
	public static void add(ServerPlayer player, int points) {
		long total = (long) total(player) + points;

		set(player, (int) Math.min(total, Integer.MAX_VALUE));
	}

	/** Takes all the player's XP. */
	public static void clear(ServerPlayer player) {
		set(player, 0);
	}

	/**
	 * Gives {@code to} the same XP as {@code from}, replacing what {@code to} held. The level and progress are carried
	 * over as they are, so levels beyond what points can count survive.
	 */
	public static void copy(Player from, ServerPlayer to) {
		to.setExperienceLevels(from.experienceLevel);
		to.experienceProgress = from.experienceProgress;
		to.totalExperience = total(from);
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

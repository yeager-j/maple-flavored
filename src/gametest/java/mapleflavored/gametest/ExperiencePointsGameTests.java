package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import mapleflavored.experience.ExperiencePoints;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** XP counted in points: writing points lands on the exact level and progress, and reading them gives the points back. */
public class ExperiencePointsGameTests {
	// Level 40 is 2,920 points, so this covers every point through both bends of the curve, at levels 16 and 31.
	private static final int SWEPT_POINTS = 3_000;
	// The first is level 1,000 exactly. The third is level 21,863 exactly, the highest whose points fit in an int.
	private static final int[] LARGE_POINTS = {4_339_720, 1_000_000_000, 2_147_407_943, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};

	@GameTest
	public void everyPointTotalRoundTripsThroughLevelAndProgress(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		int level = 0;
		int pointsIntoLevel = 0;

		for (int points = 0; points <= SWEPT_POINTS; points++) {
			ExperiencePoints.set(player, points);

			helper.assertValueEqual(player.experienceLevel, level, "level at " + points + " points");
			helper.assertValueEqual(progressInPoints(player), pointsIntoLevel, "points into the level at " + points + " points");
			helper.assertValueEqual(ExperiencePoints.total(player), points, "total read back at " + points + " points");

			// Counted level by level from vanilla's own level sizes, not from the closed forms under test.
			pointsIntoLevel++;

			if (pointsIntoLevel == player.getXpNeededForNextLevel()) {
				level++;
				pointsIntoLevel = 0;
			}
		}

		helper.succeed();
	}

	@GameTest
	public void largePointTotalsRoundTrip(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		for (int points : LARGE_POINTS) {
			ExperiencePoints.set(player, points);

			helper.assertValueEqual(ExperiencePoints.total(player), points, "total read back at " + points + " points");
		}

		helper.succeed();
	}

	@GameTest
	public void addingPointsLandsExactlyOnTheNextLevel(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		// Level 30 is 1,395 points.
		ExperiencePoints.set(player, 1_394);
		helper.assertValueEqual(player.experienceLevel, 29, "level one point short");

		ExperiencePoints.add(player, 1);

		helper.assertValueEqual(player.experienceLevel, 30, "level");
		helper.assertValueEqual(player.experienceProgress, 0.0F, "progress");
		helper.succeed();
	}

	@GameTest
	public void addingPointsStopsAtTheMostAnIntHolds(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		ExperiencePoints.set(player, Integer.MAX_VALUE - 5);
		ExperiencePoints.add(player, 100);

		helper.assertValueEqual(ExperiencePoints.total(player), Integer.MAX_VALUE, "total");
		helper.succeed();
	}

	@GameTest
	public void clearingTakesEveryPointAndCopyingCarriesLevelsSetByCommand(GameTestHelper helper) {
		ServerPlayer from = survivalPlayer(helper);
		ServerPlayer to = survivalPlayer(helper);

		MockPlayers.runCommand(helper, from, "xp set @s 40 levels");
		MockPlayers.runCommand(helper, from, "xp add @s 10 points");
		ExperiencePoints.copy(from, to);

		helper.assertValueEqual(ExperiencePoints.total(to), 2_930, "points copied");
		helper.assertValueEqual(to.experienceLevel, 40, "level copied");

		ExperiencePoints.clear(from);

		helper.assertValueEqual(ExperiencePoints.total(from), 0, "points after clearing");
		helper.assertValueEqual(from.experienceLevel, 0, "level after clearing");
		helper.assertValueEqual(from.experienceProgress, 0.0F, "progress after clearing");
		helper.succeed();
	}

	private static int progressInPoints(ServerPlayer player) {
		return Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
	}
}

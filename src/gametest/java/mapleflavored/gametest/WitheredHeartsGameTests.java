package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.dieAndRespawn;
import static mapleflavored.gametest.MockPlayers.playerWithMaxHearts;
import static mapleflavored.gametest.MockPlayers.relog;
import static mapleflavored.gametest.MockPlayers.runCommand;

import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Withered Hearts: withering under the Wither effect, the timer, the 1 usable heart minimum, gradual recovery in each
 * dimension and its timer, death and relog.
 *
 * <p>Mock players aren't ticked by a connection, so their effects never run out and tests end Wither by removing it.
 */
public class WitheredHeartsGameTests {
	private static final int WITHERING_INTERVAL = 100;
	private static final int RECOVERY_INTERVAL = 200;
	// Waits stay clear of the exact interval, where the order of the test's tick and the withering tick decides.
	private static final int MARGIN = 10;
	private static final String RECOVERY_ADVANCEMENT = "maple_flavored:nether/recover_withered_heart";

	@GameTest(maxTicks = 300)
	public void aHeartWithersEveryFiveSecondsWithoutChangingMaxHearts(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL - MARGIN, () -> assertWitheredHearts(helper, player, 0))
			.thenExecuteAfter(2 * MARGIN, () -> {
				assertWitheredHearts(helper, player, 1);
				helper.assertValueEqual(Hearts.maxHearts(player), 10, "Max Hearts");
			})
			.thenExecuteAfter(WITHERING_INTERVAL, () -> assertWitheredHearts(helper, player, 2))
			.thenSucceed();
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 200)
	public void theTimerResetsWhenTheEffectEnds(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);
		int exposure = WITHERING_INTERVAL / 2 + MARGIN;

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(exposure, () -> player.removeEffect(MobEffects.WITHER))
			.thenExecuteAfter(MARGIN, () -> wither(player))
			.thenExecuteAfter(exposure, () -> assertWitheredHearts(helper, player, 0))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void witheringStopsAtOneUsableHeart(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		runCommand(helper, player, "maxhearts set @s 3");
		wither(player);

		helper.startSequence()
			.thenExecuteAfter(3 * WITHERING_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 2);
				helper.assertValueEqual(Hearts.maxHearts(player), 3, "Max Hearts");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void witheredHeartsReturnInTheOverworldOnceTheEffectEnds(GameTestHelper helper) {
		assertRecoversWhereTheEffectEnds(helper);
	}

	@GameTest(dimension = "minecraft:the_end", maxTicks = 400)
	public void witheredHeartsReturnInTheEndOnceTheEffectEnds(GameTestHelper helper) {
		assertRecoversWhereTheEffectEnds(helper);
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 400)
	public void witheredHeartsStayInTheNetherAndReturnOnLeaving(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> player.removeEffect(MobEffects.WITHER))
			.thenExecuteAfter(MARGIN, () -> {
				assertWitheredHearts(helper, player, 1);
				helper.assertFalse(isDone(helper, player, RECOVERY_ADVANCEMENT), "no Withered Heart is back yet");
				moveTo(helper, player, Level.OVERWORLD);
			})
			.thenExecuteAfter(RECOVERY_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 0);
				helper.assertTrue(isDone(helper, player, RECOVERY_ADVANCEMENT), "getting a Withered Heart back is granted");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 700)
	public void witheredHeartsReturnOneEveryTenSeconds(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(2 * WITHERING_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 2);

				// Vanilla cuts health to max health in the player's own tick, which a mock player never runs.
				player.setHealth(player.getMaxHealth());
				player.removeEffect(MobEffects.WITHER);
			})
			.thenExecuteAfter(RECOVERY_INTERVAL - MARGIN, () -> {
				assertWitheredHearts(helper, player, 2);
				helper.assertFalse(isDone(helper, player, RECOVERY_ADVANCEMENT), "no Withered Heart is back yet");
			})
			.thenExecuteAfter(2 * MARGIN, () -> {
				assertWitheredHearts(helper, player, 1);
				helper.assertValueEqual(player.getHealth(), 16.0F, "health, as a returned heart comes back empty");
				helper.assertTrue(isDone(helper, player, RECOVERY_ADVANCEMENT), "getting a Withered Heart back is granted");
			})
			.thenExecuteAfter(RECOVERY_INTERVAL, () -> assertWitheredHearts(helper, player, 0))
			.thenSucceed();
	}

	@GameTest(maxTicks = 500)
	public void theRecoveryTimerResetsWhenWitherReturns(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);
		int partway = RECOVERY_INTERVAL / 2 + MARGIN;

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> player.removeEffect(MobEffects.WITHER))
			.thenExecuteAfter(partway, () -> wither(player))
			.thenExecuteAfter(MARGIN, () -> player.removeEffect(MobEffects.WITHER))
			// Recovery has run longer than one interval in all, but not since Wither returned.
			.thenExecuteAfter(partway, () -> assertWitheredHearts(helper, player, 1))
			.thenExecuteAfter(RECOVERY_INTERVAL / 2, () -> assertWitheredHearts(helper, player, 0))
			.thenSucceed();
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 500)
	public void theRecoveryTimerResetsOnEnteringTheNether(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);
		int partway = RECOVERY_INTERVAL / 2 + MARGIN;

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> {
				player.removeEffect(MobEffects.WITHER);
				moveTo(helper, player, Level.OVERWORLD);
			})
			.thenExecuteAfter(partway, () -> moveTo(helper, player, Level.NETHER))
			.thenExecuteAfter(MARGIN, () -> moveTo(helper, player, Level.OVERWORLD))
			// Recovery has run longer than one interval in all, but not since the last visit.
			.thenExecuteAfter(partway, () -> assertWitheredHearts(helper, player, 1))
			.thenExecuteAfter(RECOVERY_INTERVAL / 2, () -> assertWitheredHearts(helper, player, 0))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void recoveryTimerProgressSurvivesRelog(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);
		int intoRecovery = RECOVERY_INTERVAL / 2;

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> player.removeEffect(MobEffects.WITHER))
			.thenExecuteAfter(intoRecovery, () -> {
				ServerPlayer relogged = relog(helper, player);

				assertWitheredHearts(helper, relogged, 1);

				// A timer that restarted on relog would need a full interval for the heart to return.
				helper.runAfterDelay(intoRecovery + MARGIN, () -> {
					assertWitheredHearts(helper, relogged, 0);
					helper.succeed();
				});
			});
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 300)
	public void witheredHeartsDroppedByLowerMaxHeartsNeverReturnInTheNether(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		runCommand(helper, player, "maxhearts set @s 3");
		wither(player);

		helper.startSequence()
			.thenExecuteAfter(2 * WITHERING_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 2);

				// Still in the Nether and under the Wither effect, where no Withered Heart recovers.
				runCommand(helper, player, "maxhearts set @s 2");
				assertWitheredHearts(helper, player, 1);
				runCommand(helper, player, "maxhearts set @s 3");
				assertWitheredHearts(helper, player, 1);

				player.removeEffect(MobEffects.WITHER);
				runCommand(helper, player, "maxhearts set @s 1");
				assertWitheredHearts(helper, player, 0);
				runCommand(helper, player, "maxhearts set @s 3");
				assertWitheredHearts(helper, player, 0);
			})
			.thenExecuteAfter(MARGIN, () -> {
				assertWitheredHearts(helper, player, 0);
				helper.assertFalse(isDone(helper, player, RECOVERY_ADVANCEMENT), "no Withered Heart was recovered");
			})
			.thenSucceed();
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 300)
	public void deathClearsEveryWitheredHeartAtOnce(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 12);

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(2 * WITHERING_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 2);

				// Checked before any tick, so recovery outside the Nether can't be what cleared them.
				ServerPlayer respawned = dieAndRespawn(helper, player);

				assertWitheredHearts(helper, respawned, 0);
				helper.assertValueEqual(respawned.getHealth(), respawned.getMaxHealth(), "health after respawn");
			})
			.thenSucceed();
	}

	@GameTest(dimension = "minecraft:the_nether", maxTicks = 300)
	public void witheredHeartsAndTimerProgressSurviveRelog(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);
		int intoSecondInterval = WITHERING_INTERVAL / 2;

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + intoSecondInterval, () -> {
				ServerPlayer relogged = relog(helper, player);

				assertWitheredHearts(helper, relogged, 1);
				helper.assertTrue(relogged.hasEffect(MobEffects.WITHER), "Wither survives relog");

				// A timer that restarted on relog would need a full interval for the second heart.
				helper.runAfterDelay(intoSecondInterval + MARGIN, () -> {
					assertWitheredHearts(helper, relogged, 2);
					helper.succeed();
				});
			});
	}

	private static void assertRecoversWhereTheEffectEnds(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 1);

				// Vanilla cuts health to max health in the player's own tick, which a mock player never runs.
				player.setHealth(player.getMaxHealth());
				player.removeEffect(MobEffects.WITHER);
			})
			.thenExecuteAfter(RECOVERY_INTERVAL + MARGIN, () -> {
				assertWitheredHearts(helper, player, 0);
				helper.assertValueEqual(player.getHealth(), 18.0F, "health, as returned hearts come back empty");
				helper.assertTrue(isDone(helper, player, RECOVERY_ADVANCEMENT), "getting a Withered Heart back is granted");
			})
			.thenSucceed();
	}

	// Long enough to outlast any test; mock players never tick it down anyway.
	private static void wither(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60));
	}

	private static void assertWitheredHearts(GameTestHelper helper, ServerPlayer player, int withered) {
		helper.assertValueEqual(Hearts.witheredHearts(player), withered, "Withered Hearts");
		helper.assertValueEqual(Hearts.usableHearts(player), Hearts.maxHearts(player) - withered, "usable hearts");
		helper.assertValueEqual(player.getMaxHealth(), Hearts.usableHearts(player) * 2.0F, "max health");
	}

	private static void moveTo(GameTestHelper helper, ServerPlayer player, ResourceKey<Level> dimension) {
		ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
		var transition = new TeleportTransition(level, new Vec3(0.5, 100.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING);

		player.teleport(transition);
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(advancement));

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}

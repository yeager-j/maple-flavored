package mapleflavored.gametest;

import mapleflavored.grounded.Grounded;
import mapleflavored.zephyr.Zephyr;
import mapleflavored.zephyr.ZephyrJumpPayload;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Zephyr's server side: which jump requests it accepts, and what an accepted jump does to fall damage. A fall counts
 * from its start to the landing as if the jump never happened, less the Clutch: 10%, 15% or 25% of the distance fallen
 * before the jump. Falls are driven by the move packets a client sends, one per tick, and fall damage is read as
 * health lost from 20.
 */
public class ZephyrGameTests {
	// Above the test's enclosure, so falls up to 24 blocks happen in open air.
	private static final BlockPos FLOOR = new BlockPos(1, 20, 1);
	private static final float FULL_HEALTH = 20.0F;

	@GameTest(maxTicks = 200)
	public void aFallWithoutAJumpCountsInFull(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(10.0)
			.moveTo(0.0)
			// 10 blocks, less 3 safe.
			.thenAssertFallDamage(7);
	}

	@GameTest(maxTicks = 200)
	public void theHeightAJumpRegainsAddsNoFallDamage(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(5.0)
			.jump()
			.moveTo(8.0)
			.moveTo(0.0)
			// Counted from the jump's start, 5 above the floor, rather than vanilla's 8 from the top of the rise.
			.thenAssertFallDamage(2);
	}

	@GameTest(maxTicks = 200)
	public void zephyrIClutchesTenPercentOfTheFallSoFar(GameTestHelper helper) {
		// 20 × 0.9 + 4 = 22.
		assertClutchedFall(helper, 1, 1.5, 19);
	}

	@GameTest(maxTicks = 200)
	public void zephyrIiClutchesFifteenPercentOfTheFallSoFar(GameTestHelper helper) {
		// 20 × 0.85 + 4 = 21.
		assertClutchedFall(helper, 2, 2.25, 18);
	}

	@GameTest(maxTicks = 200)
	public void zephyrIiiClutchesAQuarterOfTheFallSoFar(GameTestHelper helper) {
		// 20 × 0.75 + 4 = 19.
		assertClutchedFall(helper, 3, 3.0, 16);
	}

	@GameTest(maxTicks = 200)
	public void aPauseWhileRisingStillCountsTheWholeFall(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(5.0)
			.moveTo(5.0)
			.moveTo(7.0)
			.moveTo(0.0)
			// 12 × 0.75 + 4 = 13, not just the 7 from the top of the rise.
			.thenAssertFallDamage(10);
	}

	@GameTest(maxTicks = 200)
	public void landingAboveTheJumpTakesTheRegainedHeightOff(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(6.0)
			.land()
			// 12 × 0.75 = 9, less the 2 regained.
			.thenAssertFallDamage(4);
	}

	@GameTest(maxTicks = 200)
	public void aJumpWithoutZephyrBootsIsRefused(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 0)
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(0.0)
			// The whole 16 blocks; an accepted jump would have counted 12 × 0.75 + 4 = 13.
			.thenAssertFallDamage(13);
	}

	@GameTest(maxTicks = 200)
	public void aJumpWhileGroundedIsRefused(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.grounded()
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(0.0)
			// The whole 16 blocks; an accepted jump would have counted 12 × 0.75 + 4 = 13.
			.thenAssertFallDamage(13);
	}

	@GameTest(maxTicks = 200)
	public void onlyOneJumpIsAcceptedPerAirTime(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(14.0)
			.moveTo(4.0)
			.jump()
			.moveTo(7.0)
			.moveTo(2.0)
			.jump()
			.moveTo(0.0)
			// 10 × 0.75 + 4 = 11.5; a second Clutch would have cut the 9.5 fallen by then to 7.125, plus 2.
			.thenAssertFallDamage(8);
	}

	@GameTest(maxTicks = 200)
	public void landingGivesTheJumpBack(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			// A first fall within the safe 3 blocks, with its jump, so only the second fall deals damage.
			.startAt(3.0)
			.jump()
			.moveTo(0.0)
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(7.0)
			.moveTo(0.0)
			// 12 × 0.75 + 4 = 13; with the first fall's jump still spent, it would have been 16.
			.thenAssertFallDamage(10);
	}

	@GameTest(maxTicks = 200)
	public void aJumpOnTheGroundIsRefused(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(0.0)
			.jump()
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(0.0)
			// The refused ground request left this air time's jump unspent: 12 × 0.75 + 4 = 13.
			.thenAssertFallDamage(10);
	}

	@GameTest(maxTicks = 200)
	public void aTeleportWhileRisingEndsTheJumpsFallRules(GameTestHelper helper) {
		Faller.wearingZephyr(helper, 3)
			.startAt(16.0)
			.moveTo(4.0)
			.jump()
			.moveTo(6.0)
			.startAt(10.0)
			.moveTo(0.0)
			// Only the 10 blocks after the teleport; the jump's fall rules would have counted 12 × 0.75 + 4 = 13.
			.thenAssertFallDamage(7);
	}

	@GameTest
	public void eachLevelJumpsItsHeight(GameTestHelper helper) {
		double[] heights = {1.5, 2.25, 3.0};

		for (int level = 1; level <= heights.length; level++) {
			double launchSpeed = Zephyr.launchSpeed(survivalPlayerInZephyrBoots(helper, level));

			helper.assertTrue(
				Math.abs(vanillaRiseHeight(launchSpeed) - heights[level - 1]) < 0.01,
				"Zephyr " + level + " rises " + heights[level - 1] + " blocks"
			);
		}

		helper.succeed();
	}

	@GameTest
	public void theLaunchSpeedIsFoundWithoutGravity(GameTestHelper helper) {
		ServerPlayer player = survivalPlayerInZephyrBoots(helper, 3);

		for (double gravity : new double[] {0.0, -0.08}) {
			player.getAttribute(Attributes.GRAVITY).setBaseValue(gravity);

			double launchSpeed = Zephyr.launchSpeed(player);

			helper.assertTrue(launchSpeed > 0.0 && launchSpeed < 1.0, "a gentle launch at gravity " + gravity);
		}

		helper.succeed();
	}

	// Vanilla's airborne rule at its default gravity: move by the speed, then subtract 0.08 and keep 98%.
	private static double vanillaRiseHeight(double launchSpeed) {
		double height = 0.0;

		for (double speed = launchSpeed; speed > 0.0; speed = (speed - 0.08) * 0.98) {
			height += speed;
		}

		return height;
	}

	/** A loaded survival mock player with full health, wearing iron boots with this Zephyr level (none at 0). */
	private static ServerPlayer survivalPlayerInZephyrBoots(GameTestHelper helper, int level) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		ItemStack boots = new ItemStack(Items.IRON_BOOTS);

		if (level > 0) {
			boots.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Zephyr.ENCHANTMENT), level);
		}

		player.setItemSlot(EquipmentSlot.FEET, boots);

		return player;
	}

	private static void assertClutchedFall(GameTestHelper helper, int level, double jumpHeight, int fallDamage) {
		Faller.wearingZephyr(helper, level)
			.startAt(24.0)
			.moveTo(4.0)
			.jump()
			.moveTo(4.0 + jumpHeight)
			.moveTo(0.0)
			.thenAssertFallDamage(fallDamage);
	}

	/**
	 * A survival mock player above a stone floor, moved by the packets a client sends, one client tick per game tick.
	 * Each step is queued to run on its own tick; heights are above the floor.
	 */
	private static final class Faller {
		// Within what the server accepts as one tick's fall.
		private static final double LONGEST_STEP = 1.0;
		// Teleport ids are private to the server, which ignores every id but the one it awaits.
		private static final int TELEPORT_IDS_TRIED = 64;

		private final GameTestHelper helper;
		private final ServerPlayer player;
		private final Vec3 floorTop;
		private final GameTestSequence steps;
		private double height;

		private Faller(GameTestHelper helper, ServerPlayer player) {
			this.helper = helper;
			this.player = player;
			this.floorTop = Vec3.atBottomCenterOf(helper.absolutePos(FLOOR.above()));
			this.steps = helper.startSequence();
		}

		static Faller wearingZephyr(GameTestHelper helper, int level) {
			ServerPlayer player = survivalPlayerInZephyrBoots(helper, level);

			helper.setBlock(FLOOR, Blocks.STONE);

			return new Faller(helper, player);
		}

		/** Makes the player Grounded for the rest of the test. */
		Faller grounded() {
			player.addEffect(new MobEffectInstance(Grounded.EFFECT, MobEffectInstance.INFINITE_DURATION));

			return this;
		}

		/** Teleports the player to this height, at rest, the way a server places a player. */
		Faller startAt(double startHeight) {
			height = startHeight;

			Vec3 position = at(startHeight);

			steps.thenExecuteAfter(1, () -> {
				player.connection.teleport(position.x, position.y, position.z, 0.0F, 0.0F);
				// A real connection's tick resets the movement check's start; a mock's connection never ticks.
				player.connection.resetPosition();

				for (int id = 0; id < TELEPORT_IDS_TRIED; id++) {
					player.connection.handleAcceptTeleportPacket(
						new ServerboundAcceptTeleportationPacket(id, position.x, position.y, position.z, 0.0F, 0.0F)
					);
				}
			});

			return step(startHeight, startHeight == 0.0);
		}

		/** Moves straight up or down to this height, one step per tick, landing if it is the floor. */
		Faller moveTo(double targetHeight) {
			if (height == targetHeight) return step(height, height == 0.0);

			while (height != targetHeight) {
				double next = height < targetHeight
					? Math.min(targetHeight, height + LONGEST_STEP)
					: Math.max(targetHeight, height - LONGEST_STEP);

				step(next, next == 0.0);
			}

			return this;
		}

		/** Lands where the player is, as on a ledge. */
		Faller land() {
			return step(height, true);
		}

		/** Sends the Zephyr jump request, after this tick's move. */
		Faller jump() {
			steps.thenExecute(() -> player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(ZephyrJumpPayload.INSTANCE)));

			return this;
		}

		/** Runs the queued steps, then checks the player lost this much health to falls and ends the test. */
		void thenAssertFallDamage(int damage) {
			steps.thenExecuteAfter(1, () -> helper.assertValueEqual(FULL_HEALTH - player.getHealth(), (float) damage, "fall damage"))
				.thenSucceed();
		}

		private Faller step(double nextHeight, boolean onGround) {
			height = nextHeight;

			Vec3 position = at(nextHeight);

			steps.thenExecuteAfter(1, () -> {
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(position, onGround, false));
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
			});

			return this;
		}

		private Vec3 at(double atHeight) {
			return floorTop.add(0.0, atHeight, 0.0);
		}
	}
}

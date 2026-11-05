package mapleflavored.gametest;

import mapleflavored.grounded.Grounded;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Grounded's server side: Elytra don't work, Ender Pearls thrown while Grounded fall short but still teleport, Riptide
 * doesn't work, and "How Did We Get Here?" needs Grounded too. Zephyr's refusal is in {@link ZephyrGameTests}.
 */
public class GroundedGameTests {
	// High above the test grid, so the pearls fly through open air with nothing else at this height.
	private static final int THROW_HEIGHT = 60;
	private static final int RUNWAY_LENGTH = 24;
	private static final BlockPos POOL_FLOOR = new BlockPos(1, 1, 1);
	// Above the test's enclosure, in open air.
	private static final BlockPos SKY = new BlockPos(1, 30, 1);
	// Past the 10 ticks a trident must be held before it launches.
	private static final int TRIDENT_CHARGE_TICKS = 12;
	private static final float FACING_EAST = -90.0F;
	private static final Identifier HOW_DID_WE_GET_HERE = Identifier.withDefaultNamespace("nether/all_effects");

	@GameTest
	public void aGroundedPlayerCantStartGliding(GameTestHelper helper) {
		ServerPlayer grounded = playerWithElytraInTheAir(helper, 1);
		ServerPlayer free = playerWithElytraInTheAir(helper, 4);

		ground(grounded);

		// What the server does when a client asks to start gliding.
		helper.assertTrue(free.tryToStartFallFlying(), "the other player starts gliding");
		helper.assertFalse(grounded.tryToStartFallFlying(), "the Grounded player can't start gliding");
		helper.assertFalse(grounded.isFallFlying(), "the Grounded player isn't gliding");
		helper.succeed();
	}

	@GameTest
	public void becomingGroundedWhileGlidingEndsTheGlide(GameTestHelper helper) {
		ServerPlayer grounded = playerWithElytraInTheAir(helper, 1);
		ServerPlayer free = playerWithElytraInTheAir(helper, 4);

		grounded.tryToStartFallFlying();
		free.tryToStartFallFlying();
		ground(grounded);
		grounded.doTick();
		free.doTick();

		helper.assertTrue(free.isFallFlying(), "the other player is still gliding");
		helper.assertFalse(grounded.isFallFlying(), "the Grounded player's glide ended");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void aPearlThrownWhileGroundedFallsShortButStillTeleports(GameTestHelper helper) {
		ServerPlayer grounded = playerOnRunway(helper, 1);
		ServerPlayer free = playerOnRunway(helper, 5);
		Vec3 groundedStart = grounded.position();
		Vec3 freeStart = free.position();

		ground(grounded);
		throwPearl(helper, grounded);
		throwPearl(helper, free);

		helper.startSequence()
			.thenWaitUntil(() -> {
				helper.assertTrue(grounded.position().x > groundedStart.x + 1.0, "the Grounded pearl teleported its thrower");
				helper.assertTrue(free.position().x > freeStart.x + 1.0, "the other pearl teleported its thrower");
			})
			.thenExecute(() -> {
				double groundedRange = grounded.position().x - groundedStart.x;
				double freeRange = free.position().x - freeStart.x;

				// A flat throw from standing lands about 4 blocks out while Grounded and about 13 without.
				helper.assertTrue(groundedRange > 3.0 && groundedRange < 6.0, "a Grounded flat throw lands 3 to 6 blocks out, not " + groundedRange);
				helper.assertTrue(freeRange > 11.0, "a flat throw lands over 11 blocks out, not " + freeRange);
			})
			.thenSucceed();
	}

	@GameTest
	public void aGroundedPlayerCantStartRiptideInWater(GameTestHelper helper) {
		ServerPlayer grounded = playerWithRiptideInWater(helper, 1);
		ServerPlayer free = playerWithRiptideInWater(helper, 4);

		ground(grounded);

		helper.assertValueEqual(useTrident(helper, free), InteractionResult.CONSUME, "charging Riptide in water");
		helper.assertValueEqual(useTrident(helper, grounded), InteractionResult.FAIL, "charging Riptide while Grounded");
		helper.assertFalse(grounded.isUsingItem(), "the Grounded player isn't charging");
		helper.succeed();
	}

	@GameTest
	public void becomingGroundedWhileChargingRiptideStopsTheLaunch(GameTestHelper helper) {
		ServerPlayer grounded = playerWithRiptideInWater(helper, 1);
		ServerPlayer free = playerWithRiptideInWater(helper, 4);

		useTrident(helper, grounded);
		useTrident(helper, free);
		ground(grounded);

		for (int tick = 0; tick < TRIDENT_CHARGE_TICKS; tick++) {
			grounded.doTick();
			free.doTick();
		}

		grounded.releaseUsingItem();
		free.releaseUsingItem();

		helper.assertTrue(free.isAutoSpinAttack(), "Riptide launched the other player");
		helper.assertFalse(grounded.isAutoSpinAttack(), "Riptide didn't launch the Grounded player");
		helper.assertValueEqual(grounded.getMainHandItem().getItem(), Items.TRIDENT, "the Grounded player kept their trident");
		helper.succeed();
	}

	@GameTest
	public void howDidWeGetHereNeedsGrounded(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.playerWithMaxHearts(helper, 10);
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(HOW_DID_WE_GET_HERE);

		helper.assertTrue(advancement != null, HOW_DID_WE_GET_HERE + " exists");

		// Every lasting effect but Grounded. Instant effects can't be held, so the advancement never asks for them.
		BuiltInRegistries.MOB_EFFECT.listElements()
			.filter(effect -> !effect.value().isInstantaneous() && !effect.is(Grounded.EFFECT))
			.forEach(effect -> giveForever(player, effect));

		helper.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned without Grounded");

		ground(player);

		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "earned with Grounded");
		helper.succeed();
	}

	private static void giveForever(ServerPlayer player, Holder<MobEffect> effect) {
		player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION));
	}

	private static void ground(ServerPlayer player) {
		giveForever(player, Grounded.EFFECT);
	}

	/** A loaded survival mock player standing at the start of a long stone runway, facing along it with a pearl. */
	private static ServerPlayer playerOnRunway(GameTestHelper helper, int lane) {
		for (int x = 0; x < RUNWAY_LENGTH; x++) {
			helper.setBlock(new BlockPos(x, THROW_HEIGHT - 1, lane), Blocks.STONE);
		}

		ServerPlayer player = survivalPlayerAt(helper, new BlockPos(1, THROW_HEIGHT, lane));

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL));

		return player;
	}

	/**
	 * A loaded survival mock player in open air wearing an Elytra. A mock's connection never ticks its player, so it
	 * stays where it is put.
	 */
	private static ServerPlayer playerWithElytraInTheAir(GameTestHelper helper, int lane) {
		ServerPlayer player = survivalPlayerAt(helper, SKY.offset(0, 0, lane));

		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
		player.setOnGround(false);

		return player;
	}

	/**
	 * A loaded survival mock player standing in water, holding a Riptide III trident. A mock's connection never ticks its
	 * player, so this ticks it once, the way a client's connection does each tick, to notice the water.
	 */
	private static ServerPlayer playerWithRiptideInWater(GameTestHelper helper, int lane) {
		BlockPos floor = POOL_FLOOR.offset(0, 0, lane);

		helper.setBlock(floor, Blocks.STONE);
		helper.setBlock(floor.above(), Blocks.WATER);

		ServerPlayer player = survivalPlayerAt(helper, floor.above());
		ItemStack trident = new ItemStack(Items.TRIDENT);

		trident.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.RIPTIDE), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, trident);
		player.doTick();
		helper.assertTrue(player.isInWater(), "the player is in water");

		return player;
	}

	private static ServerPlayer survivalPlayerAt(GameTestHelper helper, BlockPos feet) {
		ServerPlayer player = MockPlayers.playerWithMaxHearts(helper, 10);
		Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(feet));

		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(position.x, position.y, position.z, FACING_EAST, 0.0F);

		return player;
	}

	private static void throwPearl(GameTestHelper helper, ServerPlayer player) {
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
	}

	private static InteractionResult useTrident(GameTestHelper helper, ServerPlayer player) {
		return player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
	}
}

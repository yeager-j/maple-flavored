package mapleflavored.gametest;

import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.grounded.Grounded;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Wither: its 100-block Subspace, where Withered Hearts can't return, its 50-block leash, passing through bedrock
 * and obsidian, which grants Nice Try, suffocation immunity, its Blight book and the kill credit for every player inside
 * its Subspace.
 *
 * <p>A Subspace reaches far past the few blocks between tests, so each test runs alone, in its own environment's batch,
 * in a {@link SubspaceArena}. Its Withers have no AI, so they never attack or wander.
 */
public class WitherGameTests {
	private static final int WITHERING_INTERVAL = 100;
	// Waits stay clear of the exact interval, where the order of the test's tick and the withering tick decides.
	private static final int MARGIN = 10;
	private static final String RECOVERY_ADVANCEMENT = "maple_flavored:nether/recover_withered_heart";
	private static final String KILL_ADVANCEMENT = "maple_flavored:nether/kill_wither";
	private static final String NICE_TRY_ADVANCEMENT = "maple_flavored:nether/nice_try";
	private static final ResourceKey<Enchantment> BLIGHT = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("blight"));
	private static final double EPSILON = 1.0E-6;

	@GameTest(environment = "maple_flavored_gametest:wither_recovery", maxTicks = 300)
	public void witheredHeartsReturnOnlyOutsideAWithersSubspaceUntilTheWitherDies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		WitherBoss wither = arena.stillWither(Vec3.ZERO);
		ServerPlayer inside = arena.player(new Vec3(100.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer outside = arena.player(new Vec3(0.0, 0.0, 101.0), GameType.SURVIVAL);
		List<Subspace> opened = Subspaces.containing(level, arena.centre());

		helper.assertValueEqual(opened.size(), 1, "the Subspaces at the Wither's feet");
		helper.assertValueEqual(opened.getFirst().kind(), Subspace.Kind.WITHER, "the Subspace's kind");
		helper.assertValueEqual(opened.getFirst().boss(), wither.getUUID(), "the Subspace's boss");
		helper.assertValueEqual(opened.getFirst().radius(), 100.0, "the Subspace's radius");

		for (ServerPlayer player : List.of(inside, outside)) {
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60));
		}

		Vec3 wandered = arena.at(new Vec3(10.0, 0.0, 0.0));
		CompoundTag[] savedWither = new CompoundTag[1];

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> {
				for (ServerPlayer player : List.of(inside, outside)) {
					helper.assertValueEqual(Hearts.witheredHearts(player), 1, "Withered Hearts after one interval");
					player.removeEffect(MobEffects.WITHER);
				}

				wither.snapTo(wandered.x, wandered.y, wandered.z);
				savedWither[0] = SubspaceArena.unload(helper, wither);
			})
			.thenExecuteAfter(2 * MARGIN, () -> {
				helper.assertValueEqual(Hearts.witheredHearts(inside), 1, "Withered Hearts 100 blocks out, with the Wither unloaded");
				helper.assertFalse(isDone(helper, inside, RECOVERY_ADVANCEMENT), "a Withered Heart came back inside");
				helper.assertFalse(inside.hasEffect(MobEffects.DARKNESS), "the Wither's Subspace gives Darkness");
				helper.assertFalse(inside.hasEffect(Grounded.EFFECT), "the Wither's Subspace gives Grounded");
				helper.assertValueEqual(Hearts.witheredHearts(outside), 0, "Withered Hearts 101 blocks out");
				helper.assertValueEqual(WitherSubspaces.lightLevel(level), 3, "the light level clients draw by default");
				helper.assertTrue(isDone(helper, outside, RECOVERY_ADVANCEMENT), "getting a Withered Heart back is granted outside");

				WitherBoss reloaded = (WitherBoss) arena.track(SubspaceArena.load(helper, savedWither[0]));
				List<Subspace> afterReload = Subspaces.inLevel(level);

				helper.assertValueEqual(afterReload.size(), 1, "Subspaces once the Wither loads again");
				helper.assertValueEqual(afterReload.getFirst().centre(), arena.centre(), "the reloaded Subspace's centre");
				reloaded.kill(level);
			})
			.thenExecuteAfter(MARGIN, () -> {
				helper.assertValueEqual(Hearts.witheredHearts(inside), 0, "Withered Hearts once the Wither dies");
				helper.assertTrue(isDone(helper, inside, RECOVERY_ADVANCEMENT), "getting a Withered Heart back is granted");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_rules", maxTicks = 100)
	public void gameRulesSetTheSubspacesRadiusAndLightLevel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.stillWither(Vec3.ZERO);
		helper.assertValueEqual(Subspaces.containing(level, arena.centre()).getFirst().radius(), 60.0, "the Subspace's radius");

		helper.startSequence()
			.thenExecuteAfter(1, () -> helper.assertValueEqual(WitherSubspaces.lightLevel(level), 7, "the light level clients draw"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_passage", maxTicks = 100)
	public void theWitherPassesThroughBedrockAndObsidianButNotStone(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		// Each joins before its Wither moves, so each sees only that Wither's move for Nice Try.
		ServerPlayer nearMissWitness = arena.player(new Vec3(20.0, 0.0, -3.0), GameType.SURVIVAL);

		helper.assertFalse(diagonalNearMissPassesThrough(arena), "a diagonal move past obsidian passes into it");
		helper.assertFalse(isDone(helper, nearMissWitness, NICE_TRY_ADVANCEMENT), "Nice Try is granted for a diagonal near miss");

		ServerPlayer bedrockWitness = arena.player(new Vec3(20.0, 0.0, 0.0), GameType.SURVIVAL);

		helper.assertValueEqual(advanceThroughWall(helper, arena, Blocks.BEDROCK, 0), 2.0, "the Wither's move through bedrock");
		helper.assertTrue(isDone(helper, bedrockWitness, NICE_TRY_ADVANCEMENT), "Nice Try is granted for bedrock");

		ServerPlayer obsidianWitness = arena.player(new Vec3(20.0, 0.0, 3.0), GameType.SURVIVAL);

		helper.assertValueEqual(advanceThroughWall(helper, arena, Blocks.OBSIDIAN, 6), 2.0, "the Wither's move through obsidian");
		helper.assertTrue(isDone(helper, obsidianWitness, NICE_TRY_ADVANCEMENT), "Nice Try is granted for obsidian");

		ServerPlayer stoneWitness = arena.player(new Vec3(20.0, 0.0, 6.0), GameType.SURVIVAL);

		helper.assertTrue(advanceThroughWall(helper, arena, Blocks.STONE, 12) < 1.0, "the Wither moves through stone");
		helper.assertFalse(isDone(helper, stoneWitness, NICE_TRY_ADVANCEMENT), "Nice Try is granted for stone");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_suffocation", maxTicks = 100)
	public void theWitherDoesntSuffocateInBedrock(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		WitherBoss wither = arena.stillWither(new Vec3(0.0, 5.0, 0.0));

		fill(arena, Blocks.BEDROCK, -1, 1, 4, 9, -1, 1);

		helper.startSequence()
			.thenExecuteAfter(40, () -> {
				helper.assertTrue(wither.isInWall(), "the Wither is inside bedrock");
				helper.assertValueEqual(wither.getHealth(), wither.getMaxHealth(), "the Wither's health in bedrock");
				fill(arena, Blocks.AIR, -1, 1, 4, 9, -1, 1);
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_leash", maxTicks = 100)
	public void theWitherNeverMovesFurtherThanFiftyBlocksFromItsSubspacesCentre(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		WitherBoss wither = arena.stillWither(new Vec3(0.0, 5.0, 0.0));
		Vec3 centre = wither.position();

		place(wither, centre.add(48.0, 0.0, 0.0));
		wither.move(MoverType.SELF, new Vec3(5.0, 0.0, 0.0));
		helper.assertTrue(Math.abs(wither.position().distanceTo(centre) - 50.0) < EPSILON, "the Wither stops at its leash");

		wither.move(MoverType.SELF, new Vec3(0.0, 0.0, 5.0));
		helper.assertTrue(wither.position().distanceTo(centre) < 50.0 + EPSILON, "the Wither slides along its leash");
		helper.assertTrue(wither.position().z - centre.z > 4.0, "the Wither slides along its leash");

		// Stone just inside the leash's edge stops the inward part of a slide along it.
		place(wither, centre.add(50.0, 0.0, 0.0));
		fill(arena, Blocks.STONE, 49, 49, 5, 9, -1, 6);
		wither.move(MoverType.SELF, new Vec3(0.0, 0.0, 5.0));
		fill(arena, Blocks.AIR, 49, 49, 5, 9, -1, 6);
		helper.assertTrue(wither.position().distanceTo(centre) < 50.0 + EPSILON, "a wall pushes the Wither past its leash");

		// A piston keeps only the first axis it pushes along, so turning an upward push along the edge would move the Wither
		// sideways instead.
		Vec3 offAxis = centre.add(30.0, 40.0, 0.0);

		place(wither, offAxis);
		wither.move(MoverType.PISTON, new Vec3(0.0, 0.5, 0.0));
		helper.assertTrue(wither.position().distanceTo(offAxis) < EPSILON, "a piston pushes the Wither past its leash");

		wither.move(MoverType.PISTON, new Vec3(0.0, -0.5, 0.0));
		helper.assertTrue(wither.position().distanceTo(offAxis.add(0.0, -0.5, 0.0)) < EPSILON, "a piston pushes the Wither back in along its axis");

		place(wither, centre.add(60.0, 0.0, 0.0));
		wither.move(MoverType.SELF, new Vec3(5.0, 0.0, 0.0));
		helper.assertTrue(Math.abs(wither.position().distanceTo(centre) - 60.0) < EPSILON, "a Wither past its leash moves further out");

		wither.move(MoverType.SELF, new Vec3(-5.0, 0.0, 0.0));
		helper.assertTrue(Math.abs(wither.position().distanceTo(centre) - 55.0) < EPSILON, "a Wither past its leash moves back in");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_drops", maxTicks = 100)
	public void theWitherDropsABlightBookEveryKill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		WitherBoss wither = arena.stillWither(Vec3.ZERO);

		wither.kill(level);

		List<ItemStack> drops = level.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(wither.position(), 8.0, 8.0, 8.0))
			.stream()
			.map(ItemEntity::getItem)
			.toList();
		Holder<Enchantment> blight = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(BLIGHT);
		List<ItemStack> books = drops.stream().filter(drop -> drop.is(Items.ENCHANTED_BOOK)).toList();

		helper.assertValueEqual(books.size(), 1, "enchanted books dropped");

		ItemEnchantments stored = books.getFirst().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		helper.assertValueEqual(stored.size(), 1, "enchantments on the book");
		helper.assertValueEqual(stored.getLevel(blight), 1, "the book's Blight level");
		helper.assertTrue(drops.stream().anyMatch(drop -> drop.is(Items.NETHER_STAR)), "the Nether Star still drops");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:wither_kill", maxTicks = 100)
	public void killingTheWitherCreditsEveryPlayerInsideItsSubspace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		List<ServerPlayer> inside = List.of(
			arena.player(new Vec3(90.0, 0.0, 0.0), GameType.SURVIVAL),
			arena.player(new Vec3(-90.0, 0.0, 0.0), GameType.SURVIVAL)
		);
		ServerPlayer outside = arena.player(new Vec3(0.0, 0.0, 101.0), GameType.SURVIVAL);
		WitherBoss discarded = arena.stillWither(Vec3.ZERO);

		discarded.discard();

		for (ServerPlayer player : inside) {
			helper.assertFalse(isDone(helper, player, KILL_ADVANCEMENT), "a discarded Wither credits a kill");
		}

		WitherBoss killed = arena.stillWither(Vec3.ZERO);

		killed.kill(level);

		for (ServerPlayer player : inside) {
			helper.assertTrue(isDone(helper, player, KILL_ADVANCEMENT), "Kill the Wither is granted inside");
			helper.assertValueEqual(player.getInventory().countItem(MapleItems.CRYSTAL_HEART), 1, "Crystal Hearts rewarded");
		}

		helper.assertFalse(isDone(helper, outside, KILL_ADVANCEMENT), "Kill the Wither is granted outside");
		helper.succeed();
	}

	// Lays a wall one block thick just past a fresh Wither's side, at the offset along z, moves the Wither 2 blocks into
	// it and clears the wall. Returns how far the Wither got, to the nearest thousandth of a block.
	private static double advanceThroughWall(GameTestHelper helper, SubspaceArena arena, Block wall, int z) {
		WitherBoss wither = arena.stillWither(new Vec3(0.0, 5.0, z));
		double startX = wither.getX();

		fill(arena, wall, 1, 1, 5, 9, z - 1, z + 1);
		wither.move(MoverType.SELF, new Vec3(2.0, 0.0, 0.0));
		fill(arena, Blocks.AIR, 1, 1, 5, 9, z - 1, z + 1);

		return Math.round((wither.getX() - startX) * 1000.0) / 1000.0;
	}

	// A Wither's 0.9-block-wide box moving diagonally past an obsidian column's corner, close enough that the box
	// enclosing the whole move overlaps the column, though no point of the move does. Returns whether it ended inside.
	private static boolean diagonalNearMissPassesThrough(SubspaceArena arena) {
		WitherBoss wither = arena.stillWither(new Vec3(0.0, 5.0, 20.0));
		Vec3 corner = Vec3.atLowerCornerOf(arena.blockAt(0, 5, 20));

		place(wither, corner.add(1.3, 0.0, 1.4));
		fill(arena, Blocks.OBSIDIAN, 0, 0, 5, 9, 22, 22);
		wither.move(MoverType.SELF, new Vec3(0.3, 0.0, 0.3));

		boolean inside = arena.level().getBlockStates(wither.getBoundingBox().deflate(1.0E-4)).anyMatch(state -> state.is(Blocks.OBSIDIAN));

		fill(arena, Blocks.AIR, 0, 0, 5, 9, 22, 22);

		return inside;
	}

	private static void fill(SubspaceArena arena, Block block, int fromX, int toX, int fromY, int toY, int fromZ, int toZ) {
		BlockPos.betweenClosedStream(arena.blockAt(fromX, fromY, fromZ), arena.blockAt(toX, toY, toZ))
			.forEach(position -> arena.level().setBlock(position, block.defaultBlockState(), Block.UPDATE_ALL));
	}

	private static void place(WitherBoss wither, Vec3 position) {
		wither.snapTo(position.x, position.y, position.z);
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(advancement));

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}
}

package mapleflavored.gametest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import mapleflavored.spawners.ClearableSpawner;
import mapleflavored.spawners.SpawnerClearing;
import mapleflavored.spawners.Spawners;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Normal spawners ignore light, can't be mined or blown up until Cleared by their own mobs' deaths (12, plus 2 per
 * additional player), and drop 1 Pith when a Cleared one is broken with a pickaxe.
 */
public class SpawnerGameTests {
	private static final BlockPos SPAWNER = new BlockPos(3, 2, 3);
	private static final BlockPos SECOND_SPAWNER = new BlockPos(3, 2, 7);
	private static final int VANILLA_WAIT = 200;
	// Far enough from the spawner that its mobs' loot lands outside the spawner's drop search.
	private static final BlockPos MOB_SPOT = new BlockPos(7, 2, 7);
	// Small, so mock players from neighbouring tests stay out of range.
	private static final int TEST_RANGE = 2;
	private static final short NEVER = Short.MAX_VALUE;
	private static final Identifier BREAK_CLEARED_SPAWNER = Identifier.parse("maple_flavored:adventure/break_cleared_spawner");

	// Fabric events can't be unregistered, so one listener counts every Clear for the tests, by absolute position.
	private static final Map<BlockPos, AtomicInteger> CLEARS = new ConcurrentHashMap<>();

	static {
		Spawners.CLEARED.register((level, pos) -> CLEARS.computeIfAbsent(pos.immutable(), unused -> new AtomicInteger()).incrementAndGet());
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 200)
	public void aSpawnerSpawnsInFullLightAndMarksItsMobs(GameTestHelper helper) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GLOWSTONE);
			}
		}

		CompoundTag spawnSoon = new CompoundTag();

		// Waits a second for the glowstone's light, which the light engine spreads after this tick.
		spawnSoon.putShort("Delay", (short) 20);
		spawnSoon.putShort("MinSpawnDelay", (short) 20);
		spawnSoon.putShort("MaxSpawnDelay", (short) 20);
		spawnSoon.putShort("SpawnRange", (short) 2);

		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, spawnSoon);
		BlockPos spawnerPos = spawner.getBlockPos();
		BlockPos lit = helper.absolutePos(SPAWNER.east());

		playerAt(helper, Vec3.atCenterOf(spawnerPos).add(0.0, 1.0, 0.0));

		helper.succeedWhen(() -> {
			List<Zombie> zombies = helper.getLevel().getEntitiesOfClass(Zombie.class, new AABB(spawnerPos).inflate(4));

			// Block light, so the check doesn't depend on the time of day.
			helper.assertTrue(helper.getLevel().getBrightness(LightLayer.BLOCK, lit) >= 14, "the spawner's surroundings are fully lit");
			helper.assertTrue(!zombies.isEmpty(), "a zombie spawned in full light");

			GlobalPos origin = GlobalPos.of(helper.getLevel().dimension(), spawnerPos);

			helper.assertValueEqual(Spawners.spawnedBy(zombies.getFirst()).orElse(null), origin, "the zombie's spawner");
		});
	}

	@GameTest(maxTicks = 60)
	public void anUnclearedSpawnerWaitsHalfAsLongBetweenWaves(GameTestHelper helper) {
		CompoundTag fixedWait = new CompoundTag();

		fixedWait.putShort("Delay", (short) -1);
		fixedWait.putShort("MinSpawnDelay", (short) VANILLA_WAIT);
		fixedWait.putShort("MaxSpawnDelay", (short) VANILLA_WAIT);

		SpawnerBlockEntity uncleared = placeSpawner(helper, SPAWNER, fixedWait);
		SpawnerBlockEntity cleared = placeSpawner(helper, SECOND_SPAWNER, fixedWait);

		killMarkedMobs(helper, cleared, 12);
		playerAt(helper, Vec3.atCenterOf(uncleared.getBlockPos()).add(1.0, 0.0, 0.0));
		playerAt(helper, Vec3.atCenterOf(cleared.getBlockPos()).add(1.0, 0.0, 0.0));

		// A Delay of -1 makes each spawner choose its wait on its next tick, then both count down together.
		helper.succeedWhen(() -> {
			int unclearedWait = currentWait(helper, uncleared);
			int clearedWait = currentWait(helper, cleared);

			helper.assertTrue(unclearedWait > 0 && unclearedWait <= VANILLA_WAIT / 2, "the uncleared spawner waits " + unclearedWait);
			helper.assertValueEqual(clearedWait - unclearedWait, VANILLA_WAIT / 2, "the Cleared spawner's extra wait");
		});
	}

	@GameTest
	public void aTrialSpawnerCanStillBeMined(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper, Items.DIAMOND_PICKAXE);

		helper.setBlock(SPAWNER, Blocks.TRIAL_SPAWNER);

		helper.assertTrue(destroyProgress(helper, player) > 0.0F, "a trial spawner can be mined");
		helper.succeed();
	}

	@GameTest
	public void aSpawnerCantBeMinedUntilClearedThenDropsPithAndGrantsTheAdvancement(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		ServerPlayer player = survivalPlayer(helper, Items.DIAMOND_PICKAXE);

		helper.assertValueEqual(destroyProgress(helper, player), 0.0F, "mining progress before Cleared");

		killMarkedMobs(helper, spawner, 12);
		helper.assertTrue(destroyProgress(helper, player) > 0.0F, "a Cleared spawner can be mined");

		player.gameMode.destroyBlock(spawner.getBlockPos());

		helper.assertBlockPresent(Blocks.AIR, SPAWNER);
		helper.assertValueEqual(droppedAt(helper, SPAWNER), List.of("1 maple_flavored:pith"), "drops");
		helper.assertTrue(isDone(helper, player, BREAK_CLEARED_SPAWNER), "the breaker gets Break a Cleared spawner");
		helper.succeed();
	}

	@GameTest
	public void aClearedSpawnerBrokenByHandDropsNothing(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		ServerPlayer player = survivalPlayer(helper, Items.AIR);

		killMarkedMobs(helper, spawner, 12);
		player.gameMode.destroyBlock(spawner.getBlockPos());

		helper.assertBlockPresent(Blocks.AIR, SPAWNER);
		helper.assertValueEqual(droppedAt(helper, SPAWNER), List.of(), "drops");
		helper.assertFalse(isDone(helper, player, BREAK_CLEARED_SPAWNER), "no advancement without the Pith");
		helper.succeed();
	}

	@GameTest
	public void twelveDeathsClearASoloSpawnerOnceAndTheForcefieldClosesIn(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());

		playerAt(helper, Vec3.atCenterOf(spawner.getBlockPos()).add(1.0, 0.0, 0.0));
		Spawners.recordPlayersInRange(helper.getLevel(), spawner.getBlockPos(), spawner);

		helper.assertValueEqual(clearing(spawner).requirement(), 12, "a solo requirement");
		helper.assertValueEqual(clearing(spawner).remainingShare(), 1.0F, "share to go before any death");

		killMarkedMobs(helper, spawner, 1);
		helper.assertValueEqual(clearing(spawner).remainingShare(), 11.0F / 12.0F, "share to go after 1 death");

		killMarkedMobs(helper, spawner, 10);
		helper.assertValueEqual(clearing(spawner).remainingShare(), 1.0F / 12.0F, "share to go after 11 deaths");
		helper.assertFalse(clearing(spawner).isCleared(), "11 deaths don't Clear it");
		helper.assertValueEqual(clearsAt(spawner), 0, "Cleared events before");

		killMarkedMobs(helper, spawner, 1);
		helper.assertTrue(clearing(spawner).isCleared(), "the 12th death Clears it");
		helper.assertValueEqual(clearing(spawner).remainingShare(), 0.0F, "a Cleared spawner has no forcefield");
		helper.assertValueEqual(clearsAt(spawner), 1, "Cleared events after the 12th death");

		killMarkedMobs(helper, spawner, 3);
		helper.assertTrue(clearing(spawner).isCleared(), "it stays Cleared");
		helper.assertValueEqual(clearing(spawner).deaths(), 12, "deaths after Cleared aren't counted");
		helper.assertValueEqual(clearsAt(spawner), 1, "Cleared fires only once");
		helper.succeed();
	}

	@GameTest
	public void theRequirementGrowsWithEachPlayerAndNeverDrops(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		ServerLevel level = helper.getLevel();
		Vec3 centre = Vec3.atCenterOf(spawner.getBlockPos());
		List<ServerPlayer> players = List.of(
			playerAt(helper, centre.add(1.0, 0.0, 0.0)),
			playerAt(helper, centre.add(-1.0, 0.0, 0.0)),
			playerAt(helper, centre.add(0.0, 0.0, 1.0))
		);

		Spawners.recordPlayersInRange(level, spawner.getBlockPos(), spawner);
		helper.assertValueEqual(clearing(spawner).requirement(), 16, "the requirement for 3 players");

		killMarkedMobs(helper, spawner, 4);
		helper.assertValueEqual(clearing(spawner).remainingShare(), 0.75F, "share to go after 4 of 16 deaths");

		Vec3 farAway = centre.add(0.0, 50.0, 0.0);

		players.get(1).teleportTo(farAway.x, farAway.y, farAway.z);
		players.get(2).teleportTo(farAway.x, farAway.y, farAway.z);
		Spawners.recordPlayersInRange(level, spawner.getBlockPos(), spawner);
		helper.assertValueEqual(clearing(spawner).requirement(), 16, "the requirement after 2 players leave");

		killMarkedMobs(helper, spawner, 11);
		helper.assertFalse(clearing(spawner).isCleared(), "15 deaths don't Clear it");

		killMarkedMobs(helper, spawner, 1);
		helper.assertTrue(clearing(spawner).isCleared(), "the 16th death Clears it");
		helper.succeed();
	}

	@GameTest
	public void aPlayerArrivingMidCountRaisesTheRequirement(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		Vec3 centre = Vec3.atCenterOf(spawner.getBlockPos());

		playerAt(helper, centre.add(1.0, 0.0, 0.0));
		Spawners.recordPlayersInRange(helper.getLevel(), spawner.getBlockPos(), spawner);
		killMarkedMobs(helper, spawner, 3);

		playerAt(helper, centre.add(-1.0, 0.0, 0.0));
		Spawners.recordPlayersInRange(helper.getLevel(), spawner.getBlockPos(), spawner);

		helper.assertValueEqual(clearing(spawner).playerCount(), 2, "players counted");
		helper.assertValueEqual(clearing(spawner).requirement(), 14, "the requirement for 2 players");
		helper.assertValueEqual(clearing(spawner).deaths(), 3, "deaths so far");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 300)
	public void aMarkedMobDyingInLavaCounts(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());

		helper.setBlock(MOB_SPOT, Blocks.LAVA);

		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_SPOT);

		Spawners.markSpawned(helper.getLevel(), spawner.getBlockPos(), spawner.getSpawner(), zombie);

		helper.succeedWhen(() -> helper.assertValueEqual(clearing(spawner).deaths(), 1, "deaths after the lava"));
	}

	@GameTest
	public void aMarkedZombieThatDrownsStillCountsOnce(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_SPOT);

		Spawners.markSpawned(helper.getLevel(), spawner.getBlockPos(), spawner.getSpawner(), zombie);

		Drowned drowned = zombie.convertTo(EntityTypes.DROWNED, ConversionParams.single(zombie, true, true), converted -> {});

		helper.assertTrue(drowned != null, "the zombie converts");
		helper.assertValueEqual(clearing(spawner).deaths(), 0, "deaths after converting");

		drowned.kill(helper.getLevel());

		helper.assertValueEqual(clearing(spawner).deaths(), 1, "deaths after the drowned dies");
		helper.succeed();
	}

	@GameTest
	public void anUnmarkedMobsDeathDoesntCount(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_SPOT);

		zombie.kill(helper.getLevel());

		helper.assertValueEqual(clearing(spawner).deaths(), 0, "deaths");
		helper.succeed();
	}

	@GameTest
	public void aSpawnerResistsExplosionsUntilCleared(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());

		explodeAt(helper, SPAWNER);
		helper.assertBlockPresent(Blocks.SPAWNER, SPAWNER);

		killMarkedMobs(helper, spawner, 12);
		explodeAt(helper, SPAWNER);

		helper.assertBlockPresent(Blocks.AIR, SPAWNER);
		helper.assertTrue(droppedAt(helper, SPAWNER).contains("1 maple_flavored:pith"), "a blown-up Cleared spawner drops its Pith");
		helper.succeed();
	}

	@GameTest
	public void aSpawnersClearingSurvivesSavingAndReachesTheClient(GameTestHelper helper) {
		SpawnerBlockEntity spawner = placeSpawner(helper, SPAWNER, new CompoundTag());
		ServerLevel level = helper.getLevel();
		Holder<Enchantment> smite = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE);

		playerAt(helper, Vec3.atCenterOf(spawner.getBlockPos()).add(1.0, 0.0, 0.0));
		Spawners.recordPlayersInRange(level, spawner.getBlockPos(), spawner);
		Spawners.holdBook(spawner, smite);
		killMarkedMobs(helper, spawner, 5);

		CompoundTag saved = spawner.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(spawner.getBlockPos(), spawner.getBlockState(), saved, level.registryAccess());
		SpawnerBlockEntity onClient = new SpawnerBlockEntity(spawner.getBlockPos(), spawner.getBlockState());

		onClient.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), spawner.getUpdateTag(level.registryAccess())));

		assertSameClearing(helper, clearing((SpawnerBlockEntity) loaded), clearing(spawner), "after a save");
		assertSameClearing(helper, clearing(onClient), clearing(spawner), "in the update tag");

		killMarkedMobs(helper, spawner, 7);

		SpawnerBlockEntity reloaded = (SpawnerBlockEntity) BlockEntity.loadStatic(
			spawner.getBlockPos(), spawner.getBlockState(), spawner.saveWithFullMetadata(level.registryAccess()), level.registryAccess()
		);

		helper.assertTrue(clearing(reloaded).isCleared(), "Cleared survives a save");
		helper.succeed();
	}

	/** Places a zombie spawner with a small activation range that never spawns, unless {@code changes} says otherwise. */
	static SpawnerBlockEntity placeSpawner(GameTestHelper helper, BlockPos relative, CompoundTag changes) {
		helper.setBlock(relative, Blocks.SPAWNER);

		ServerLevel level = helper.getLevel();
		SpawnerBlockEntity spawner = (SpawnerBlockEntity) level.getBlockEntity(helper.absolutePos(relative));

		spawner.setEntityId(EntityTypes.ZOMBIE, level.getRandom());

		CompoundTag settings = spawner.saveCustomOnly(level.registryAccess());

		settings.putShort("Delay", NEVER);
		settings.putShort("MinSpawnDelay", NEVER);
		settings.putShort("MaxSpawnDelay", NEVER);
		settings.putShort("RequiredPlayerRange", (short) TEST_RANGE);
		settings.merge(changes);
		spawner.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), settings));

		return spawner;
	}

	/** Spawns {@code count} mobs as if the spawner had, and kills each. */
	static void killMarkedMobs(GameTestHelper helper, SpawnerBlockEntity spawner, int count) {
		for (int i = 0; i < count; i++) {
			Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_SPOT);

			Spawners.markSpawned(helper.getLevel(), spawner.getBlockPos(), spawner.getSpawner(), zombie);
			zombie.kill(helper.getLevel());
		}
	}

	static SpawnerClearing clearing(SpawnerBlockEntity spawner) {
		return ((ClearableSpawner) spawner).maple_flavored$clearing();
	}

	/** Each item entity near {@code relative}, as its count and item id, such as {@code "1 maple_flavored:pith"}. */
	static List<String> droppedAt(GameTestHelper helper, BlockPos relative) {
		AABB around = new AABB(helper.absolutePos(relative)).inflate(1.5);

		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
			.map(ItemEntity::getItem)
			.map(stack -> stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()))
			.sorted()
			.toList();
	}

	static ServerPlayer survivalPlayer(GameTestHelper helper, Item tool) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));

		return player;
	}

	static boolean isDone(GameTestHelper helper, ServerPlayer player, Identifier id) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);

		helper.assertTrue(holder != null, id + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static ServerPlayer playerAt(GameTestHelper helper, Vec3 position) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		player.teleportTo(position.x, position.y, position.z);

		return player;
	}

	private static float destroyProgress(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(SPAWNER);

		return helper.getLevel().getBlockState(pos).getDestroyProgress(player, helper.getLevel(), pos);
	}

	private static void explodeAt(GameTestHelper helper, BlockPos relative) {
		Vec3 centre = helper.absoluteVec(Vec3.atCenterOf(relative).add(1.0, 0.0, 0.0));

		helper.getLevel().explode(null, centre.x, centre.y, centre.z, 4.0F, Level.ExplosionInteraction.TNT);
	}

	private static int currentWait(GameTestHelper helper, SpawnerBlockEntity spawner) {
		return spawner.saveCustomOnly(helper.getLevel().registryAccess()).getShortOr("Delay", (short) 0);
	}

	private static int clearsAt(SpawnerBlockEntity spawner) {
		AtomicInteger clears = CLEARS.get(spawner.getBlockPos());

		return clears == null ? 0 : clears.get();
	}

	private static void assertSameClearing(GameTestHelper helper, SpawnerClearing actual, SpawnerClearing expected, String when) {
		helper.assertValueEqual(actual.deaths(), expected.deaths(), "deaths " + when);
		helper.assertValueEqual(actual.playerCount(), expected.playerCount(), "players " + when);
		helper.assertValueEqual(actual.isCleared(), expected.isCleared(), "Cleared " + when);
		helper.assertValueEqual(actual.heldBook(), expected.heldBook(), "held book " + when);
	}
}

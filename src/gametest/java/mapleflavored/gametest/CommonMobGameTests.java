package mapleflavored.gametest;

import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Common mobs take the spec's table whenever they first enter the world, by difficulty and height, and take their new
 * type's row when they convert. In the Overworld, mobs that spawn below Y 63 keep vanilla health, except cave spiders
 * and silverfish. Creepers never spawn naturally at Y 63 or above in the Overworld, while spawners are unaffected.
 *
 * <p>Tests run near Y 4, so a mob checked against the surface column spawns at Y 63 above its test and then moves down
 * into it, where it must keep its stats.
 */
public class CommonMobGameTests {
	private static final BlockPos MOB_POS = new BlockPos(2, 2, 2);
	private static final int LOWEST_SURFACE_Y = 63;
	private static final boolean ADULT = false;
	private static final boolean BABY = true;
	private static final List<EntityType<? extends Zombie>> BABY_ZOMBIES = List.of(
		EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER
	);
	private static final List<EntityType<? extends Mob>> SKELETONS = List.of(
		EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.PARCHED
	);
	private static final List<Holder<Attribute>> ZOMBIE_STATS = List.of(
		Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.STEP_HEIGHT, Attributes.ATTACK_DAMAGE, Attributes.JUMP_STRENGTH
	);
	private static final List<Holder<Attribute>> HUSK_STATS = List.of(
		Attributes.MAX_HEALTH,
		Attributes.MOVEMENT_SPEED,
		Attributes.ATTACK_DAMAGE,
		Attributes.ARMOR,
		Attributes.FOLLOW_RANGE,
		Attributes.KNOCKBACK_RESISTANCE
	);
	private static final List<Holder<Attribute>> ADULT_ONLY_STATS = List.of(
		Attributes.MOVEMENT_SPEED,
		Attributes.STEP_HEIGHT,
		Attributes.ATTACK_DAMAGE,
		Attributes.JUMP_STRENGTH,
		Attributes.ARMOR,
		Attributes.FOLLOW_RANGE,
		Attributes.KNOCKBACK_RESISTANCE
	);
	private static final List<Holder<Attribute>> SPIDER_STATS = List.of(Attributes.JUMP_STRENGTH, Attributes.FALL_DAMAGE_MULTIPLIER);
	private static final int CREEPER_SITE_Y = 58;

	@GameTest(environment = "maple_flavored_gametest:easy")
	public void easyMobsTakeTheEasyColumn(GameTestHelper helper) {
		assertVanilla(helper, EntityTypes.ZOMBIE, ADULT, ZOMBIE_STATS);
		assertBabyZombieHealth(helper, 4.0);
		assertSkeletonHealth(helper, 10.0);
		assertStats(helper, EntityTypes.CREEPER, ADULT, Map.of(Attributes.MAX_HEALTH, 14.0));
		assertStats(helper, EntityTypes.CAVE_SPIDER, ADULT, Map.of(Attributes.MAX_HEALTH, 2.0, Attributes.MOVEMENT_SPEED, 0.35));
		assertStats(helper, EntityTypes.SILVERFISH, ADULT, Map.of(Attributes.MAX_HEALTH, 2.0));
		assertVanilla(helper, EntityTypes.SPIDER, ADULT, SPIDER_STATS);
		assertVanilla(helper, EntityTypes.HUSK, ADULT, HUSK_STATS);
		assertUnlistedAdultsStayVanilla(helper);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void normalMobsTakeTheNormalColumn(GameTestHelper helper) {
		assertStats(
			helper,
			EntityTypes.ZOMBIE,
			ADULT,
			Map.of(Attributes.MAX_HEALTH, 10.0, Attributes.MOVEMENT_SPEED, 0.34, Attributes.STEP_HEIGHT, 1.0)
		);
		assertBabyZombieHealth(helper, 4.0);
		assertSkeletonHealth(helper, 10.0);
		assertStats(helper, EntityTypes.CREEPER, ADULT, Map.of(Attributes.MAX_HEALTH, 16.0));
		assertStats(helper, EntityTypes.CAVE_SPIDER, ADULT, Map.of(Attributes.MAX_HEALTH, 4.0, Attributes.MOVEMENT_SPEED, 0.40));
		assertStats(helper, EntityTypes.SILVERFISH, ADULT, Map.of(Attributes.MAX_HEALTH, 2.0));
		assertStats(helper, EntityTypes.SPIDER, ADULT, Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0));
		assertStats(
			helper,
			EntityTypes.HUSK,
			ADULT,
			Map.of(
				Attributes.MAX_HEALTH, 30.0,
				Attributes.MOVEMENT_SPEED, 0.25,
				Attributes.ATTACK_DAMAGE, 6.0,
				Attributes.ARMOR, 12.0,
				Attributes.FOLLOW_RANGE, 50.0,
				Attributes.KNOCKBACK_RESISTANCE, 0.9
			)
		);
		assertUnlistedAdultsStayVanilla(helper);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void hardMobsTakeTheHardColumn(GameTestHelper helper) {
		assertStats(
			helper,
			EntityTypes.ZOMBIE,
			ADULT,
			Map.of(
				Attributes.MAX_HEALTH, 15.0,
				Attributes.MOVEMENT_SPEED, 0.38,
				Attributes.STEP_HEIGHT, 1.0,
				Attributes.ATTACK_DAMAGE, 5.0,
				Attributes.JUMP_STRENGTH, 0.6
			)
		);
		assertBabyZombieHealth(helper, 7.0);
		assertSkeletonHealth(helper, 12.0);
		assertStats(helper, EntityTypes.CREEPER, ADULT, Map.of(Attributes.MAX_HEALTH, 18.0));
		assertStats(helper, EntityTypes.CAVE_SPIDER, ADULT, Map.of(Attributes.MAX_HEALTH, 6.0, Attributes.MOVEMENT_SPEED, 0.42));
		assertStats(helper, EntityTypes.SILVERFISH, ADULT, Map.of(Attributes.MAX_HEALTH, 4.0));
		assertStats(helper, EntityTypes.SPIDER, ADULT, Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0));
		assertStats(
			helper,
			EntityTypes.HUSK,
			ADULT,
			Map.of(
				Attributes.MAX_HEALTH, 40.0,
				Attributes.MOVEMENT_SPEED, 0.265,
				Attributes.ATTACK_DAMAGE, 8.0,
				Attributes.ARMOR, 14.0,
				Attributes.FOLLOW_RANGE, 60.0,
				Attributes.KNOCKBACK_RESISTANCE, 1.0
			)
		);
		assertUnlistedAdultsStayVanilla(helper);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 200)
	public void aSurfaceBabyZombieThatDrownsUndergroundBecomesABabyDrownedWithVanillaHealth(GameTestHelper helper) {
		Zombie zombie = drowning(helper, spawnOnSurface(helper, EntityTypes.ZOMBIE, BABY));

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(zombie.isUnderWaterConverting(), "the zombie is converting"))
			.thenExecute(() -> zombie.setConversionTime(0))
			.thenWaitUntil(() -> helper.assertEntityPresent(EntityTypes.DROWNED))
			.thenExecute(() -> {
				Mob drowned = helper.findOneEntity(EntityTypes.DROWNED);

				helper.assertTrue(drowned.isBaby(), "the drowned is a baby");
				assertVanillaHealth(helper, drowned);
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 200)
	public void anAdultHuskThatDrownsUndergroundBecomesAFastZombieWithVanillaHealth(GameTestHelper helper) {
		Zombie husk = drowning(helper, spawnOnSurface(helper, EntityTypes.HUSK, ADULT));

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(husk.isUnderWaterConverting(), "the husk is converting"))
			.thenExecute(() -> husk.setConversionTime(0))
			.thenWaitUntil(() -> helper.assertEntityPresent(EntityTypes.ZOMBIE))
			.thenExecute(() -> {
				Mob zombie = helper.findOneEntity(EntityTypes.ZOMBIE);

				assertStats(helper, zombie, Map.of(Attributes.MOVEMENT_SPEED, 0.34, Attributes.STEP_HEIGHT, 1.0));
				assertVanillaHealth(helper, zombie);
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aSilverfishFromAnInfestedBlockIsFrail(GameTestHelper helper) {
		helper.setBlock(MOB_POS, Blocks.INFESTED_STONE);
		helper.getLevel().destroyBlock(helper.absolutePos(MOB_POS), true);

		assertStats(helper, helper.findOneEntity(EntityTypes.SILVERFISH), Map.of(Attributes.MAX_HEALTH, 2.0));
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aBabyFromASpawnEggUsedOnASurfaceZombieIsFrail(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		Zombie parent = spawnAt(helper.getLevel(), helper.absolutePos(MOB_POS).atY(LOWEST_SURFACE_Y), EntityTypes.ZOMBIE, ADULT);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ZOMBIE_SPAWN_EGG));
		parent.interact(player, InteractionHand.MAIN_HAND, parent.position());

		List<Zombie> babies = helper.getLevel().getEntitiesOfClass(Zombie.class, parent.getBoundingBox().inflate(2.0), Zombie::isBaby);

		helper.assertValueEqual(babies.size(), 1, "babies from the spawn egg");
		assertStats(helper, babies.getFirst(), Map.of(Attributes.MAX_HEALTH, 4.0));
		// They're above the test, out of reach of its cleanup.
		babies.forEach(Entity::discard);
		parent.discard();
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void normalMobsBelowY63KeepVanillaHealthButTakeTheirOtherStats(GameTestHelper helper) {
		Mob zombie = spawnBelowSurface(helper, EntityTypes.ZOMBIE, ADULT);

		assertStats(helper, zombie, Map.of(Attributes.MOVEMENT_SPEED, 0.34, Attributes.STEP_HEIGHT, 1.0));
		assertVanillaHealth(helper, zombie);

		for (EntityType<? extends Zombie> type : BABY_ZOMBIES) {
			assertVanillaHealth(helper, spawnBelowSurface(helper, type, BABY));
		}

		for (EntityType<? extends Mob> type : SKELETONS) {
			assertVanillaHealth(helper, spawnBelowSurface(helper, type, ADULT));
		}

		assertVanillaHealth(helper, spawnBelowSurface(helper, EntityTypes.CREEPER, ADULT));

		Mob husk = spawnBelowSurface(helper, EntityTypes.HUSK, ADULT);

		assertStats(
			helper,
			husk,
			Map.of(
				Attributes.MOVEMENT_SPEED, 0.25,
				Attributes.ATTACK_DAMAGE, 6.0,
				Attributes.ARMOR, 12.0,
				Attributes.FOLLOW_RANGE, 50.0,
				Attributes.KNOCKBACK_RESISTANCE, 0.9
			)
		);
		assertVanillaHealth(helper, husk);
		assertStats(
			helper,
			spawnBelowSurface(helper, EntityTypes.SPIDER, ADULT),
			Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
		);
		assertStats(
			helper,
			spawnBelowSurface(helper, EntityTypes.CAVE_SPIDER, ADULT),
			Map.of(Attributes.MAX_HEALTH, 4.0, Attributes.MOVEMENT_SPEED, 0.40)
		);
		assertStats(helper, spawnBelowSurface(helper, EntityTypes.SILVERFISH, ADULT), Map.of(Attributes.MAX_HEALTH, 2.0));
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void hardZombiesBelowY63KeepVanillaHealthButHitAndJumpHarder(GameTestHelper helper) {
		Mob zombie = spawnBelowSurface(helper, EntityTypes.ZOMBIE, ADULT);

		assertStats(
			helper,
			zombie,
			Map.of(
				Attributes.MOVEMENT_SPEED, 0.38,
				Attributes.STEP_HEIGHT, 1.0,
				Attributes.ATTACK_DAMAGE, 5.0,
				Attributes.JUMP_STRENGTH, 0.6
			)
		);
		assertVanillaHealth(helper, zombie);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void netherSkeletonsAreFrailAtAnyHeight(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		BlockPos pos = helper.absolutePos(MOB_POS);
		Mob skeleton = spawnAt(nether, pos, EntityTypes.SKELETON, ADULT);

		helper.assertTrue(pos.getY() < LOWEST_SURFACE_Y, "the Nether skeleton spawns below Y 63");
		assertStats(helper, skeleton, Map.of(Attributes.MAX_HEALTH, 10.0));
		skeleton.discard();
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 200)
	public void creepersSpawnNaturallyOnlyBelowY63InTheOverworldButSpawnersStillWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos site = TestSites.of(helper).chunk(0).getBlockAt(0, CREEPER_SITE_Y, 0);
		BlockPos atY62 = site.offset(1, 4, 1);
		BlockPos atY63 = site.offset(3, 5, 1);
		BlockPos atY64 = site.offset(5, 6, 1);
		List<BlockPos> spots = List.of(atY62, atY63, atY64);
		ServerLevel nether = level.getServer().getLevel(Level.NETHER);

		helper.assertValueEqual(List.of(atY62.getY(), atY63.getY(), atY64.getY()), List.of(62, 63, 64), "spot heights");
		buildDarkSpots(helper, level, site, spots);
		buildDarkSpots(helper, nether, site, List.of(atY63));

		helper.startSequence()
			.thenWaitUntil(() -> {
				spots.forEach(spot -> helper.assertTrue(isPitchDark(level, spot), "the spot at Y " + spot.getY() + " is dark"));
				helper.assertTrue(isPitchDark(nether, atY63), "the Nether spot is dark");
			})
			.thenExecute(() -> {
				helper.assertTrue(spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, atY62), "a creeper spawns naturally at Y 62");
				helper.assertFalse(spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, atY63), "a creeper spawns naturally at Y 63");
				helper.assertFalse(spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, atY64), "a creeper spawns naturally at Y 64");
				helper.assertFalse(
					spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.CHUNK_GENERATION, atY63),
					"a creeper spawns with a new chunk at Y 63"
				);
				helper.assertTrue(spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.SPAWNER, atY63), "a spawner spawns a creeper at Y 63");
				helper.assertTrue(
					spawnAllowed(level, EntityTypes.CREEPER, EntitySpawnReason.TRIAL_SPAWNER, atY63),
					"a trial spawner spawns a creeper at Y 63"
				);
				helper.assertTrue(spawnAllowed(level, EntityTypes.ZOMBIE, EntitySpawnReason.NATURAL, atY63), "a zombie spawns naturally at Y 63");
				helper.assertTrue(
					spawnAllowed(nether, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, atY63),
					"a creeper spawns naturally at Y 63 outside the Overworld"
				);
			})
			.thenSucceed();
	}

	// Babies get only their health; the adult rows' other stats must not leak onto them.
	private static void assertBabyZombieHealth(GameTestHelper helper, double health) {
		for (EntityType<? extends Zombie> type : BABY_ZOMBIES) {
			Mob baby = spawnOnSurface(helper, type, BABY);
			AttributeSupplier defaults = DefaultAttributes.getSupplier(type);

			helper.assertTrue(baby.isBaby(), type.toShortString() + " spawned as a baby");
			assertStats(helper, baby, Map.of(Attributes.MAX_HEALTH, health));

			for (Holder<Attribute> attribute : ADULT_ONLY_STATS) {
				helper.assertValueEqual(baby.getAttributeBaseValue(attribute), defaults.getBaseValue(attribute), describe(baby, attribute));
			}
		}
	}

	private static void assertSkeletonHealth(GameTestHelper helper, double health) {
		for (EntityType<? extends Mob> type : SKELETONS) {
			assertStats(helper, type, ADULT, Map.of(Attributes.MAX_HEALTH, health));
		}
	}

	// Adult zombie villagers keep vanilla stats so curing isn't a chase; adult drowned aren't in the table.
	private static void assertUnlistedAdultsStayVanilla(GameTestHelper helper) {
		assertVanilla(helper, EntityTypes.ZOMBIE_VILLAGER, ADULT, ZOMBIE_STATS);
		assertVanilla(helper, EntityTypes.DROWNED, ADULT, ZOMBIE_STATS);
	}

	private static void assertVanilla(GameTestHelper helper, EntityType<? extends Mob> type, boolean baby, List<Holder<Attribute>> attributes) {
		Mob mob = spawnOnSurface(helper, type, baby);
		AttributeSupplier defaults = DefaultAttributes.getSupplier(type);

		for (Holder<Attribute> attribute : attributes) {
			helper.assertValueEqual(mob.getAttributeBaseValue(attribute), defaults.getBaseValue(attribute), describe(mob, attribute));
		}
	}

	@SuppressWarnings("unchecked")
	private static void assertVanillaHealth(GameTestHelper helper, Mob mob) {
		EntityType<? extends Mob> type = (EntityType<? extends Mob>) mob.getType();
		double vanilla = DefaultAttributes.getSupplier(type).getBaseValue(Attributes.MAX_HEALTH);

		helper.assertValueEqual(mob.getAttributeBaseValue(Attributes.MAX_HEALTH), vanilla, describe(mob, Attributes.MAX_HEALTH));
		helper.assertValueEqual(mob.getHealth(), mob.getMaxHealth(), describe(mob, Attributes.MAX_HEALTH) + " (spawned at full health)");
	}

	private static void assertStats(GameTestHelper helper, EntityType<? extends Mob> type, boolean baby, Map<Holder<Attribute>, Double> expected) {
		assertStats(helper, spawnOnSurface(helper, type, baby), expected);
	}

	private static void assertStats(GameTestHelper helper, Mob mob, Map<Holder<Attribute>, Double> expected) {
		expected.forEach((attribute, value) -> helper.assertValueEqual(mob.getAttributeBaseValue(attribute), value, describe(mob, attribute)));

		if (expected.containsKey(Attributes.MAX_HEALTH)) {
			helper.assertValueEqual(mob.getHealth(), mob.getMaxHealth(), describe(mob, Attributes.MAX_HEALTH) + " (spawned at full health)");
		}
	}

	private static String describe(Mob mob, Holder<Attribute> attribute) {
		String age = mob.isBaby() ? "baby " : "";

		return age + mob.getType().toShortString() + " " + attribute.getRegisteredName();
	}

	private static <T extends Mob> T spawnOnSurface(GameTestHelper helper, EntityType<T> type, boolean baby) {
		return spawnAtHeightThenMoveIn(helper, type, baby, LOWEST_SURFACE_Y);
	}

	private static <T extends Mob> T spawnBelowSurface(GameTestHelper helper, EntityType<T> type, boolean baby) {
		return spawnAtHeightThenMoveIn(helper, type, baby, LOWEST_SURFACE_Y - 1);
	}

	// Moved into the test so its cleanup removes the mob.
	private static <T extends Mob> T spawnAtHeightThenMoveIn(GameTestHelper helper, EntityType<T> type, boolean baby, int y) {
		BlockPos pos = helper.absolutePos(MOB_POS);
		T mob = spawnAt(helper.getLevel(), pos.atY(y), type, baby);

		mob.snapTo(Vec3.atBottomCenterOf(pos));

		return mob;
	}

	// Spawned as natural spawning does. A zombie's group data decides whether it's a baby, as vanilla's own spawns do.
	private static <T extends Mob> T spawnAt(ServerLevel level, BlockPos pos, EntityType<T> type, boolean baby) {
		T mob = type.create(level, EntitySpawnReason.NATURAL);
		SpawnGroupData groupData = mob instanceof Zombie ? new Zombie.ZombieGroupData(baby, false) : null;

		mob.snapTo(Vec3.atBottomCenterOf(pos));
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, groupData);
		// Mock players join far from the test, so a mob that isn't persistent despawns on its first tick.
		mob.setPersistenceRequired();
		level.addFreshEntityWithPassengers(mob);

		return mob;
	}

	// Floods a glass box around the zombie and starts its drowning, so the next tick begins its conversion. A natural husk
	// rides in on a Camel Husk one time in ten, which would hold its eyes above the water, so the zombie is taken off any
	// mount first.
	private static Zombie drowning(GameTestHelper helper, Zombie zombie) {
		Entity mount = zombie.getVehicle();

		if (mount != null) {
			zombie.stopRiding();
			mount.getSelfAndPassengers().toList().forEach(Entity::discard);
			zombie.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(MOB_POS)));
		}

		for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 4, 5, 4)) {
			boolean wall = pos.getX() == 0 || pos.getX() == 4 || pos.getY() == 1 || pos.getY() == 5 || pos.getZ() == 0 || pos.getZ() == 4;

			helper.setBlock(pos, wall ? Blocks.GLASS : Blocks.WATER);
		}

		zombie.setInWaterTime(600);

		return zombie;
	}

	// Sealed air pockets in solid stone, each standing on stone, so only the height differs between them.
	private static void buildDarkSpots(GameTestHelper helper, ServerLevel level, BlockPos site, List<BlockPos> spots) {
		ForcedChunks.keepLoaded(helper, level, List.of(ChunkPos.containing(site)));

		for (BlockPos pos : BlockPos.betweenClosed(site, site.offset(6, 10, 2))) {
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		}

		for (BlockPos spot : spots) {
			level.setBlock(spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private static boolean isPitchDark(ServerLevel level, BlockPos pos) {
		return level.getBrightness(LightLayer.SKY, pos) == 0 && level.getBrightness(LightLayer.BLOCK, pos) == 0;
	}

	private static boolean spawnAllowed(ServerLevel level, EntityType<? extends Mob> type, EntitySpawnReason reason, BlockPos pos) {
		return SpawnPlacements.checkSpawnRules(type, level, reason, pos, level.getRandom());
	}
}

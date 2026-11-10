package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mapleflavored.deepvariants.DeepVariants;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.gametest.mixin.LivingEntityAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import mapleflavored.gametest.mixin.CreeperTestAccessor;
import mapleflavored.mixin.AbstractArrowAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Below Y 0 in the Overworld, natural spawning makes Deep Zombies where it would make adult zombies, while babies,
 * spawners and higher spawns stay zombies. A Deep Zombie takes its health and damage from the Deep variant game rules
 * as it spawns, keeps the adult zombie row's other stats, disables a blocking player's shield as an axe does, counts as
 * a zombie for every tag, and drops a zombie's loot and twice its XP.
 *
 * <p>Natural spawning runs vanilla's own spawner in sealed dark rooms built far from the other tests.
 */
public class DeepVariantGameTests {
	private static final BlockPos MOB_POS = new BlockPos(2, 2, 2);
	// Each at the centre of its own chunk, far from every other test and from each other.
	private static final BlockPos DEEP_ROOM = new BlockPos(54_008, -30, -53_992);
	private static final BlockPos SHALLOW_ROOM = new BlockPos(54_264, 10, -53_992);
	private static final BlockPos DEEP_SPAWNER = new BlockPos(54_520, -30, -53_992);
	private static final BlockPos REINFORCEMENT_ROOM = new BlockPos(54_776, -30, -53_992);
	// Natural spawning needs the nearest player more than 24 blocks away.
	private static final int PLAYER_DISTANCE = 28;
	private static final int ROOM_HALF_WIDTH = 5;
	// Reinforcements arrive 7 to 40 blocks from their caller along each axis.
	private static final int REINFORCEMENT_ROOM_HALF_WIDTH = 12;
	private static final int MAX_REINFORCEMENT_HITS = 40;
	private static final Identifier REINFORCEMENT_CALLEE_CHARGE = Identifier.withDefaultNamespace("reinforcement_callee_charge");
	private static final int ROOM_HEIGHT = 4;
	private static final int WANTED_PER_KIND = 4;
	private static final List<EntityType<? extends Mob>> BASES = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.SPIDER);
	private static final List<EntityType<? extends Mob>> DEEP_TYPES = List.of(
		DeepVariants.DEEP_ZOMBIE,
		DeepVariants.DEEP_SKELETON,
		DeepVariants.DEEP_CREEPER,
		DeepVariants.DEEP_SPIDER
	);
	private static final int MAX_SPAWN_ATTEMPTS = 5_000;
	private static final int AGE_ROLLS = 4_000;
	private static final double VANILLA_BABY_ODDS = 0.05;
	private static final double BABY_ODDS_TOLERANCE = 0.02;
	private static final int SHIELD_COOLDOWN_TICKS = 100;
	private static final float LEVEL_ONE_DAMAGE_BONUS = 2.5F;
	private static final int MONSTER_XP = 5;
	private static final int XP_ROLLS = 50;
	private static final int BLINDNESS_TICKS = 60;
	private static final int MAX_JOCKEY_ATTEMPTS = 3_000;
	private static final BlockPos ARROW_TARGET_POS = new BlockPos(2, 2, 6);
	private static final float ARROW_POWER = 1.0F;
	// The spread of the triangle in AbstractArrow.setBaseDamageFromMob, plus rounding room.
	private static final double ARROW_DAMAGE_SPREAD = 0.57425 + 1.0E-6;
	private static final BlockPos CREEPER_CRATER = new BlockPos(55_032, 10, -53_992);
	private static final BlockPos DEEP_CREEPER_CRATER = new BlockPos(55_288, 10, -53_992);
	private static final int CRATER_HALF_WIDTH = 6;
	private static final int LOOT_ROLLS = 200;

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void naturalSpawnsBelowY0AreDeepVariantsButBabyZombiesStayZombies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		prepareRoom(helper, level, DEEP_ROOM);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isPitchDark(level, DEEP_ROOM), "the room below Y 0 is dark"))
			.thenExecute(() -> {
				List<Mob> mobs = spawnMonstersNaturally(helper, level, DEEP_ROOM);

				for (EntityType<? extends Mob> base : BASES) {
					helper.assertTrue(mobs.stream().anyMatch(mob -> DeepVariants.baseOf(mob.getType()).orElse(null) == base), "natural spawning made a Deep " + base.toShortString() + " below Y 0");
				}

				for (Mob mob : mobs) {
					boolean deep = DeepVariants.baseOf(mob.getType()).isPresent();

					helper.assertTrue(deep != mob.isBaby(), "a natural " + describe(mob) + " spawned below Y 0");
				}
			})
			.thenExecute(() -> unforceRoom(level, DEEP_ROOM))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void naturalSpawnsAtOrAboveY0StayVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		prepareRoom(helper, level, SHALLOW_ROOM);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isPitchDark(level, SHALLOW_ROOM), "the room above Y 0 is dark"))
			.thenExecute(() -> {
				List<Mob> mobs = spawnMonstersNaturally(helper, level, SHALLOW_ROOM);

				helper.assertTrue(mobs.stream().anyMatch(mob -> mob.getType() == EntityTypes.ZOMBIE && !mob.isBaby()), "an adult zombie spawned above Y 0");

				for (Mob mob : mobs) {
					helper.assertTrue(DeepVariants.baseOf(mob.getType()).isEmpty(), "a natural " + describe(mob) + " spawned above Y 0");
				}
			})
			.thenExecute(() -> unforceRoom(level, SHALLOW_ROOM))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void naturalSpawningSwapsOnlyAdultBaseMobsBelowY0InTheOverworld(GameTestHelper helper) {
		ServerLevel overworld = helper.getLevel();
		ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
		BlockPos belowZero = new BlockPos(0, -1, 0);
		Zombie.ZombieGroupData adult = new Zombie.ZombieGroupData(false, true);
		Zombie.ZombieGroupData baby = new Zombie.ZombieGroupData(true, true);

		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, belowZero, adult), DeepVariants.DEEP_ZOMBIE, adult, "an adult at Y -1");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, belowZero, baby), EntityTypes.ZOMBIE, baby, "a baby at Y -1");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, BlockPos.ZERO, adult), EntityTypes.ZOMBIE, adult, "an adult at Y 0");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, nether, belowZero, adult), EntityTypes.ZOMBIE, adult, "an adult in the Nether");

		for (EntityType<? extends Mob> base : List.of(EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.SPIDER)) {
			EntityType<?> deep = deepOf(base);
			Spider.SpiderEffectsGroupData groupData = new Spider.SpiderEffectsGroupData();

			assertNaturalSpawn(helper, DeepVariants.naturalSpawn(base, overworld, belowZero, groupData), deep, groupData, "a " + base.toShortString() + " at Y -1");
			assertNaturalSpawn(helper, DeepVariants.naturalSpawn(base, overworld, belowZero, null), deep, null, "a new " + base.toShortString() + " group at Y -1");
			assertNaturalSpawn(helper, DeepVariants.naturalSpawn(base, overworld, BlockPos.ZERO, null), base, null, "a " + base.toShortString() + " at Y 0");
			assertNaturalSpawn(helper, DeepVariants.naturalSpawn(base, nether, belowZero, null), base, null, "a " + base.toShortString() + " in the Nether");
		}

		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.CAVE_SPIDER, overworld, belowZero, null), EntityTypes.CAVE_SPIDER, null, "a cave spider at Y -1");

		int babies = 0;

		for (int roll = 0; roll < AGE_ROLLS; roll++) {
			DeepVariants.NaturalSpawn spawn = DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, belowZero, null);

			if (!(spawn.groupData() instanceof Zombie.ZombieGroupData rolled)) {
				helper.fail("a new zombie group below Y 0 got no age");
				return;
			}

			helper.assertValueEqual(spawn.type(), rolled.isBaby ? EntityTypes.ZOMBIE : DeepVariants.DEEP_ZOMBIE, "a new group's type");

			if (rolled.isBaby) babies++;
		}

		double babyRate = (double) babies / AGE_ROLLS;

		helper.assertTrue(Math.abs(babyRate - VANILLA_BABY_ODDS) <= BABY_ODDS_TOLERANCE, "the baby rate " + babyRate + " stays vanilla's 5%");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aSpawnerBelowY0StillSpawnsZombies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SpawnerBlockEntity spawner = prepareSpawner(helper, level, DEEP_SPAWNER);
		AABB around = new AABB(DEEP_SPAWNER).inflate(6);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(level.getEntitiesOfClass(Zombie.class, around).isEmpty(), "the spawner spawned a zombie"))
			.thenExecute(() -> {
				List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, around);

				for (Zombie zombie : zombies) {
					helper.assertValueEqual(zombie.getType(), EntityTypes.ZOMBIE, "the spawner's mob below Y 0");
				}

				zombies.forEach(Entity::discard);
				level.removeBlock(spawner.getBlockPos(), false);
				unforceRoom(level, DEEP_SPAWNER);
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aNormalDeepZombieHas30HealthAnd6AttackWithZombieSpeedAndStepUp(GameTestHelper helper) {
		assertStats(
			helper,
			spawnDeepZombie(helper),
			Map.of(Attributes.MAX_HEALTH, 30.0, Attributes.ATTACK_DAMAGE, 6.0, Attributes.MOVEMENT_SPEED, 0.34, Attributes.STEP_HEIGHT, 1.0)
		);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void aHardDeepZombieKeeps30HealthAnd6AttackAndJumpsHigher(GameTestHelper helper) {
		assertStats(
			helper,
			spawnDeepZombie(helper),
			Map.of(
				Attributes.MAX_HEALTH, 30.0,
				Attributes.ATTACK_DAMAGE, 6.0,
				Attributes.MOVEMENT_SPEED, 0.38,
				Attributes.STEP_HEIGHT, 1.0,
				Attributes.JUMP_STRENGTH, 0.6
			)
		);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:deep_zombie_rule_change")
	public void theDeepVariantRulesSetHealthAndDamageForLaterSpawnsOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie first = spawnDeepZombie(helper);

		assertStats(helper, first, Map.of(Attributes.MAX_HEALTH, 40.0, Attributes.ATTACK_DAMAGE, 9.0));
		first.hurtServer(level, level.damageSources().generic(), 5.0F);

		float firstHealth = first.getHealth();

		level.getGameRules().set(MapleGameRules.DEEP_VARIANT_HEALTH_PERCENT, 150, level.getServer());
		level.getGameRules().set(MapleGameRules.DEEP_VARIANT_DAMAGE_PERCENT, 250, level.getServer());

		Zombie second = spawnDeepZombie(helper);

		assertStats(helper, second, Map.of(Attributes.MAX_HEALTH, 30.0, Attributes.ATTACK_DAMAGE, 7.5));
		helper.assertValueEqual(first.getAttributeBaseValue(Attributes.MAX_HEALTH), 40.0, "the first Deep Zombie's max health after the change");
		helper.assertValueEqual(first.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 9.0, "the first Deep Zombie's attack after the change");
		helper.assertValueEqual(first.getHealth(), firstHealth, "the first Deep Zombie's health after the change");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepZombieHitDisablesABlockingShieldFor100Ticks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer blocker = blockingPlayer(helper, new BlockPos(1, 2, 1));
		ServerPlayer plainBlocker = blockingPlayer(helper, new BlockPos(5, 2, 1));
		ItemStack shield = blocker.getOffhandItem();

		helper.spawnWithNoFreeWill(DeepVariants.DEEP_ZOMBIE, new BlockPos(1, 2, 3)).doHurtTarget(level, blocker);
		helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 3)).doHurtTarget(level, plainBlocker);

		helper.assertFalse(blocker.isUsingItem(), "the player still blocks after a Deep Zombie's hit");
		helper.assertTrue(plainBlocker.isBlocking(), "the player still blocks after a zombie's hit");
		helper.assertFalse(plainBlocker.getCooldowns().isOnCooldown(plainBlocker.getOffhandItem()), "a zombie's hit put the shield on cooldown");

		for (int tick = 1; tick < SHIELD_COOLDOWN_TICKS; tick++) {
			blocker.getCooldowns().tick();
		}

		helper.assertTrue(blocker.getCooldowns().isOnCooldown(shield), "the shield is on cooldown 99 ticks after the hit");
		blocker.getCooldowns().tick();
		helper.assertFalse(blocker.getCooldowns().isOnCooldown(shield), "the shield is on cooldown 100 ticks after the hit");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void eachDeepVariantIsInEveryTagItsBaseMobIsIn(GameTestHelper helper) {
		Registry<EntityType<?>> types = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

		for (EntityType<? extends Mob> deep : DEEP_TYPES) {
			EntityType<?> base = DeepVariants.baseOf(deep).orElseThrow();

			types.getTags()
				.filter(tag -> tag.contains(base.builtInRegistryHolder()))
				.forEach(tag -> helper.assertTrue(tag.contains(deep.builtInRegistryHolder()), deep.toShortString() + " is in " + tag.key().location()));
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void smiteHitsDeepZombiesAndSkeletonsAndBaneOfArthropodsHitsDeepSpiders(GameTestHelper helper) {
		assertEnchantmentBonus(helper, Enchantments.SMITE, DeepVariants.DEEP_ZOMBIE);
		assertEnchantmentBonus(helper, Enchantments.SMITE, DeepVariants.DEEP_SKELETON);
		assertEnchantmentBonus(helper, Enchantments.BANE_OF_ARTHROPODS, DeepVariants.DEEP_SPIDER);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void eachDeepVariantDropsItsBaseMobsLootAndTwiceItsXp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer killer = helper.makeMockServerPlayerInLevel();

		for (EntityType<? extends Mob> deepType : DEEP_TYPES) {
			EntityType<? extends Mob> baseType = DeepVariants.baseOf(deepType).orElseThrow();
			Mob deep = gearless(helper.spawnWithNoFreeWill(deepType, MOB_POS));
			Mob base = gearless(helper.spawnWithNoFreeWill(baseType, MOB_POS.east()));
			LootParams params = killedBy(level, deep, killer);
			LootTable deepTable = level.getServer().reloadableRegistries().getLootTable(deep.getLootTable().orElseThrow());
			LootTable baseTable = level.getServer().reloadableRegistries().getLootTable(base.getLootTable().orElseThrow());

			for (long seed = 0; seed < LOOT_ROLLS; seed++) {
				List<String> deepDrops = describe(deepTable.getRandomItems(params, RandomSource.create(seed)));
				List<String> baseDrops = describe(baseTable.getRandomItems(params, RandomSource.create(seed)));

				helper.assertValueEqual(deepDrops, baseDrops, deepType.toShortString() + " drops with seed " + seed);
			}

			helper.assertValueEqual(base.getExperienceReward(level, killer), MONSTER_XP, baseType.toShortString() + "'s XP");
			helper.assertValueEqual(deep.getExperienceReward(level, killer), DeepVariants.XP_MULTIPLIER * MONSTER_XP, deepType.toShortString() + "'s XP");
			deep.discard();
			base.discard();
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepSkeletonWithABowDropsTwiceASkeletonsXp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer killer = helper.makeMockServerPlayerInLevel();
		Mob archer = archer(helper, DeepVariants.DEEP_SKELETON, MOB_POS);

		// A skeleton with a bow gives its 5 XP plus 1 to 3 for the bow.
		for (int roll = 0; roll < XP_ROLLS; roll++) {
			int xp = archer.getExperienceReward(level, killer);

			helper.assertTrue(xp % DeepVariants.XP_MULTIPLIER == 0, "a Deep Skeleton's XP " + xp + " is doubled");
			helper.assertTrue(xp >= 2 * (MONSTER_XP + 1) && xp <= 2 * (MONSTER_XP + 3), "a Deep Skeleton's XP " + xp + " is twice an armed skeleton's");
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void normalDeepSkeletonsAndSpidersTakeTheirHealthAndDamageFromTheRulesAndDeepCreepersKeepVanillaStats(GameTestHelper helper) {
		assertStats(
			helper,
			spawnNaturally(helper, DeepVariants.DEEP_SKELETON),
			Map.of(Attributes.MAX_HEALTH, 30.0, Attributes.ATTACK_DAMAGE, 4.0, Attributes.FOLLOW_RANGE, 32.0)
		);
		assertStats(
			helper,
			spawnNaturally(helper, DeepVariants.DEEP_SPIDER, MOB_POS.east(3)),
			Map.of(Attributes.MAX_HEALTH, 24.0, Attributes.ATTACK_DAMAGE, 4.0, Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
		);
		assertStats(helper, spawnNaturally(helper, DeepVariants.DEEP_CREEPER), Map.of(Attributes.MAX_HEALTH, 20.0, Attributes.ATTACK_DAMAGE, 2.0));
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:deep_variant_rules")
	public void theRulesScaleDeepSkeletonsAndSpidersButNotDeepCreepers(GameTestHelper helper) {
		assertStats(helper, spawnNaturally(helper, DeepVariants.DEEP_SKELETON), Map.of(Attributes.MAX_HEALTH, 40.0, Attributes.ATTACK_DAMAGE, 6.0));
		assertStats(helper, spawnNaturally(helper, DeepVariants.DEEP_SPIDER, MOB_POS.east(3)), Map.of(Attributes.MAX_HEALTH, 32.0, Attributes.ATTACK_DAMAGE, 6.0));
		assertStats(helper, spawnNaturally(helper, DeepVariants.DEEP_CREEPER), Map.of(Attributes.MAX_HEALTH, 20.0, Attributes.ATTACK_DAMAGE, 2.0));
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aNormalDeepSkeletonsArrowsDealDoubleDamage(GameTestHelper helper) {
		assertArrowDamageMultiplier(helper, 2.0);
	}

	@GameTest(environment = "maple_flavored_gametest:deep_variant_rules")
	public void aDeepSkeletonsArrowDamageFollowsTheDamageRule(GameTestHelper helper) {
		assertArrowDamageMultiplier(helper, 3.0);
	}

	@GameTest(environment = "maple_flavored_gametest:deep_skeleton_rule_change")
	public void aDeepSkeletonKeepsItsArrowDamageWhenTheRuleChangesOrItReloads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Skeleton first = spawnNaturally(helper, DeepVariants.DEEP_SKELETON);

		level.getGameRules().set(MapleGameRules.DEEP_VARIANT_DAMAGE_PERCENT, 200, level.getServer());

		Skeleton second = spawnNaturally(helper, DeepVariants.DEEP_SKELETON, MOB_POS.east(3));

		helper.assertValueEqual(DeepVariants.damageMultiplier(first), 3.0, "the first Deep Skeleton's multiplier after the change");
		helper.assertValueEqual(DeepVariants.damageMultiplier(reloaded(helper, first)), 3.0, "the first Deep Skeleton's multiplier after reloading");
		helper.assertValueEqual(DeepVariants.damageMultiplier(second), 2.0, "a later Deep Skeleton's multiplier");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void aDeepSkeletonsArrowGivesAStraysSlowness(GameTestHelper helper) {
		Skeleton deepArcher = archer(helper, DeepVariants.DEEP_SKELETON, new BlockPos(1, 2, 1));
		AbstractSkeleton strayArcher = archer(helper, EntityTypes.STRAY, new BlockPos(5, 2, 1));
		// Two blocks from its archer, where a skeleton's random aim can't miss.
		Mob deepTarget = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 3));
		Mob strayTarget = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 3));

		deepArcher.performRangedAttack(deepTarget, ARROW_POWER);
		strayArcher.performRangedAttack(strayTarget, ARROW_POWER);

		helper.succeedWhen(() -> {
			MobEffectInstance slowness = deepTarget.getEffect(MobEffects.SLOWNESS);
			MobEffectInstance straySlowness = strayTarget.getEffect(MobEffects.SLOWNESS);

			helper.assertTrue(slowness != null, "the Deep Skeleton's arrow slows its target");
			helper.assertTrue(straySlowness != null, "the stray's arrow slows its target");
			helper.assertValueEqual(slowness.getAmplifier(), straySlowness.getAmplifier(), "the Slowness amplifier");
			helper.assertTrue(Math.abs(slowness.getDuration() - straySlowness.getDuration()) <= 1, "the Slowness lasts as long as a stray's");
		});
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void aDeepCreeperBlowsABiggerCraterThanACreeper(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ChunkPos> chunks = List.of(ChunkPos.containing(CREEPER_CRATER), ChunkPos.containing(DEEP_CREEPER_CRATER));

		forceChunks(helper, level, chunks, CREEPER_CRATER.getY());

		helper.startSequence()
			.thenExecute(() -> {
				int creeperCrater = explodeCreeperAndMeasureCrater(level, EntityTypes.CREEPER, CREEPER_CRATER);
				int deepCreeperCrater = explodeCreeperAndMeasureCrater(level, DeepVariants.DEEP_CREEPER, DEEP_CREEPER_CRATER);

				helper.assertTrue(creeperCrater > 0, "the creeper blew a crater");
				helper.assertTrue(deepCreeperCrater > creeperCrater, "the Deep Creeper's crater of " + deepCreeperCrater + " blocks is bigger than the creeper's " + creeperCrater);
				unforceChunks(level, chunks);
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepCreeperHasTntsExplosionPowerHoweverItIsMade(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Creeper spawned = helper.spawnWithNoFreeWill(DeepVariants.DEEP_CREEPER, MOB_POS);
		Creeper creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, MOB_POS.east());
		CompoundTag summonData = new CompoundTag();

		summonData.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(DeepVariants.DEEP_CREEPER).toString());

		Entity summoned = EntityType.loadEntityRecursive(
			TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), summonData),
			level,
			EntitySpawnReason.COMMAND,
			entity -> entity
		);

		helper.assertValueEqual(explosionPower(creeper), 3, "a creeper's explosion power");
		helper.assertValueEqual(explosionPower(spawned), 4, "a spawned Deep Creeper's explosion power");
		helper.assertValueEqual(explosionPower((Creeper) summoned), 4, "a Deep Creeper summoned without data");
		helper.assertValueEqual(explosionPower((Creeper) reloaded(helper, spawned)), 4, "a reloaded Deep Creeper's explosion power");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepSpidersBiteBlindsFor60Ticks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer bitten = MockPlayers.survivalPlayer(helper);
		ServerPlayer plainBitten = MockPlayers.survivalPlayer(helper);

		bitten.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))));
		plainBitten.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(5, 2, 1))));
		helper.spawnWithNoFreeWill(DeepVariants.DEEP_SPIDER, new BlockPos(1, 2, 3)).doHurtTarget(level, bitten);
		helper.spawnWithNoFreeWill(EntityTypes.SPIDER, new BlockPos(5, 2, 3)).doHurtTarget(level, plainBitten);

		MobEffectInstance blindness = bitten.getEffect(MobEffects.BLINDNESS);

		helper.assertTrue(blindness != null, "a Deep Spider's bite blinds");
		helper.assertValueEqual(blindness.getDuration(), BLINDNESS_TICKS, "the Blindness duration");
		helper.assertFalse(plainBitten.hasEffect(MobEffects.BLINDNESS), "a spider's bite blinds");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepSpidersJockeyIsADeepSkeletonAndASpidersStaysASkeleton(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Spider deepJockey = finalizeUntilJockey(helper, DeepVariants.DEEP_SPIDER);
		Spider plainJockey = finalizeUntilJockey(helper, EntityTypes.SPIDER);

		helper.assertValueEqual(plainJockey.getFirstPassenger().getType(), EntityTypes.SKELETON, "a spider's jockey");
		helper.assertValueEqual(deepJockey.getFirstPassenger().getType(), DeepVariants.DEEP_SKELETON, "a Deep Spider's jockey");

		level.addFreshEntityWithPassengers(deepJockey);
		assertStats(helper, (Mob) deepJockey.getFirstPassenger(), Map.of(Attributes.MAX_HEALTH, 30.0, Attributes.FOLLOW_RANGE, 32.0));
		assertStats(helper, deepJockey, Map.of(Attributes.MAX_HEALTH, 24.0));
		deepJockey.getSelfAndPassengers().toList().forEach(Entity::discard);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:deep_zombie_reinforcements", maxTicks = 400)
	public void aDeepZombieCallsDeepZombiesAsReinforcements(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ChunkPos> chunks = chunksAround(REINFORCEMENT_ROOM);

		forceChunks(helper, level, chunks, REINFORCEMENT_ROOM.getY());
		buildRoom(level, REINFORCEMENT_ROOM, REINFORCEMENT_ROOM_HALF_WIDTH);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isPitchDark(level, REINFORCEMENT_ROOM), "the room below Y 0 is dark"))
			.thenExecute(() -> {
				ServerPlayer attacker = MockPlayers.survivalPlayer(helper);
				Zombie caller = spawnDeepZombie(level, REINFORCEMENT_ROOM);
				AABB room = new AABB(REINFORCEMENT_ROOM).inflate(REINFORCEMENT_ROOM_HALF_WIDTH + 2);
				List<Zombie> reinforcements = List.of();

				attacker.snapTo(Vec3.atBottomCenterOf(REINFORCEMENT_ROOM.offset(2, 0, 0)));
				caller.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(1.0);

				// Each hit tries 50 random spots for a reinforcement, most of them inside the walls.
				for (int hit = 0; hit < MAX_REINFORCEMENT_HITS && reinforcements.isEmpty(); hit++) {
					// A mob ignores repeat hits until its damage cooldown, counted down in its own tick, runs out.
					caller.damageCooldownTime = 0;
					caller.setHealth(caller.getMaxHealth());
					caller.hurtServer(level, level.damageSources().playerAttack(attacker), 1.0F);
					reinforcements = level.getEntitiesOfClass(Zombie.class, room, DeepVariantGameTests::isReinforcement);
				}

				helper.assertFalse(reinforcements.isEmpty(), "the Deep Zombie called a reinforcement");

				for (Zombie reinforcement : reinforcements) {
					helper.assertValueEqual(reinforcement.getType(), DeepVariants.DEEP_ZOMBIE, "a Deep Zombie's reinforcement");
				}

				level.getEntitiesOfClass(Zombie.class, room).forEach(Entity::discard);
				unforceChunks(level, chunks);
			})
			.thenSucceed();
	}

	// A sealed dark room in stone, standing on stone, with a survival player far enough away for natural spawning.
	private static void prepareRoom(GameTestHelper helper, ServerLevel level, BlockPos centre) {
		forceChunks(helper, level, roomChunks(centre), centre.getY());
		buildRoom(level, centre, ROOM_HALF_WIDTH);
		MockPlayers.survivalPlayer(helper).snapTo(Vec3.atBottomCenterOf(centre.offset(PLAYER_DISTANCE, 0, 0)));
	}

	// Air ROOM_HEIGHT blocks tall from the centre's height, walled, floored and roofed with stone.
	private static void buildRoom(ServerLevel level, BlockPos centre, int halfWidth) {
		BlockPos from = centre.offset(-halfWidth - 1, -1, -halfWidth - 1);
		BlockPos to = centre.offset(halfWidth + 1, ROOM_HEIGHT, halfWidth + 1);

		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			boolean inside = Math.abs(pos.getX() - centre.getX()) <= halfWidth
				&& Math.abs(pos.getZ() - centre.getZ()) <= halfWidth
				&& pos.getY() >= centre.getY()
				&& pos.getY() < centre.getY() + ROOM_HEIGHT;

			level.setBlock(pos, (inside ? Blocks.AIR : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	// Runs vanilla's natural spawning in the room until it has made enough of each base mob or its Deep variant, clearing
	// the room after each try. Returns those mobs, jockey riders included.
	private static List<Mob> spawnMonstersNaturally(GameTestHelper helper, ServerLevel level, BlockPos centre) {
		AABB room = new AABB(centre).inflate(ROOM_HALF_WIDTH + 2);
		List<Mob> mobs = new ArrayList<>();

		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS && !hasEnoughOfEach(mobs); attempt++) {
			NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, centre);

			List<Entity> spawned = level.getEntitiesOfClass(Entity.class, room, entity -> !(entity instanceof Player));

			for (Entity entity : spawned) {
				if (BASES.contains(baseKind(entity.getType()))) mobs.add((Mob) entity);

				entity.discard();
			}
		}

		helper.assertTrue(hasEnoughOfEach(mobs), "natural spawning made " + mobs.size() + " zombies, skeletons, creepers and spiders");

		return mobs;
	}

	// Counts adults only: a pack of zombies is all babies or all adults, so one pack of babies would otherwise be enough
	// zombies with no adult among them.
	private static boolean hasEnoughOfEach(List<Mob> mobs) {
		return BASES.stream().allMatch(base -> mobs.stream().filter(mob -> baseKind(mob.getType()) == base && !mob.isBaby()).count() >= WANTED_PER_KIND);
	}

	// The base mob a Deep variant stands in for, or the type itself.
	private static EntityType<?> baseKind(EntityType<?> type) {
		return DeepVariants.baseOf(type).<EntityType<?>>map(base -> base).orElse(type);
	}

	private static EntityType<? extends Mob> deepOf(EntityType<? extends Mob> base) {
		return DEEP_TYPES.stream().filter(deep -> DeepVariants.baseOf(deep).orElse(null) == base).findFirst().orElseThrow();
	}

	// An open pocket on a stone floor with a zombie spawner in it that spawns within a second, and a survival player
	// beside it. The test world is solid at this depth.
	private static SpawnerBlockEntity prepareSpawner(GameTestHelper helper, ServerLevel level, BlockPos pos) {
		forceChunks(helper, level, roomChunks(pos), pos.getY());

		for (BlockPos block : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, ROOM_HEIGHT, 4))) {
			boolean floor = block.getY() < pos.getY();

			level.setBlock(block, (floor ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
		}

		level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);

		SpawnerBlockEntity spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);

		spawner.setEntityId(EntityTypes.ZOMBIE, level.getRandom());

		CompoundTag settings = spawner.saveCustomOnly(level.registryAccess());

		settings.putShort("Delay", (short) 20);
		settings.putShort("SpawnRange", (short) 2);
		settings.putShort("RequiredPlayerRange", (short) 16);
		spawner.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), settings));
		MockPlayers.survivalPlayer(helper).snapTo(Vec3.atBottomCenterOf(pos.offset(3, 0, 3)));

		return spawner;
	}

	// Forces the chunks and waits for them to tick entities.
	private static void forceChunks(GameTestHelper helper, ServerLevel level, List<ChunkPos> chunks, int y) {
		chunks.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), true));
		ForcedChunks.awaitEntityTicking(helper, level, chunks, y);
	}

	private static void unforceChunks(ServerLevel level, List<ChunkPos> chunks) {
		chunks.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), false));
	}

	private static void unforceRoom(ServerLevel level, BlockPos centre) {
		unforceChunks(level, roomChunks(centre));
	}

	// The room's chunk and the one its natural-spawning player stands in.
	private static List<ChunkPos> roomChunks(BlockPos centre) {
		return List.of(ChunkPos.containing(centre), ChunkPos.containing(centre.offset(PLAYER_DISTANCE, 0, 0)));
	}

	// The chunk holding the centre and the eight around it.
	private static List<ChunkPos> chunksAround(BlockPos centre) {
		ChunkPos middle = ChunkPos.containing(centre);
		List<ChunkPos> chunks = new ArrayList<>();

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				chunks.add(new ChunkPos(middle.x() + x, middle.z() + z));
			}
		}

		return chunks;
	}

	// Vanilla marks each reinforcement with a charge on its own reinforcement chance.
	private static boolean isReinforcement(Zombie zombie) {
		return zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).getModifier(REINFORCEMENT_CALLEE_CHARGE) != null;
	}

	private static boolean isPitchDark(ServerLevel level, BlockPos pos) {
		return level.getBrightness(LightLayer.SKY, pos) == 0 && level.getBrightness(LightLayer.BLOCK, pos) == 0;
	}

	private static void assertNaturalSpawn(
		GameTestHelper helper, DeepVariants.NaturalSpawn spawn, EntityType<?> type, @Nullable SpawnGroupData groupData, String what
	) {
		helper.assertValueEqual(spawn.type(), type, "the type natural spawning makes for " + what);
		helper.assertTrue(spawn.groupData() == groupData, "the group data is passed on for " + what);
	}

	// Finalizes new spiders of the type at the test's mob spot until one gets a jockey, which vanilla rolls 1 in 100.
	private static Spider finalizeUntilJockey(GameTestHelper helper, EntityType<? extends Spider> type) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(MOB_POS);

		for (int attempt = 0; attempt < MAX_JOCKEY_ATTEMPTS; attempt++) {
			Spider spider = type.create(level, EntitySpawnReason.NATURAL);

			spider.snapTo(Vec3.atBottomCenterOf(pos));
			spider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);

			if (spider.isVehicle()) return spider;
		}

		throw helper.assertionException("no " + type.toShortString() + " spawned as a jockey");
	}

	// Fires an arrow from a Deep Skeleton and a skeleton and checks the Deep one's base damage is the multiplier times
	// one in vanilla's range.
	private static void assertArrowDamageMultiplier(GameTestHelper helper, double multiplier) {
		ServerLevel level = helper.getLevel();
		Skeleton deepArcher = archer(helper, DeepVariants.DEEP_SKELETON, MOB_POS);
		Skeleton plainArcher = archer(helper, EntityTypes.SKELETON, MOB_POS.east(3));
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, ARROW_TARGET_POS);
		// AbstractArrow.setBaseDamageFromMob: power * 2 plus a triangle around 0.11 per difficulty level.
		double mode = ARROW_POWER * 2 + level.getDifficulty().getId() * 0.11;
		double deepDamage = firedArrowDamage(helper, deepArcher, target);
		double plainDamage = firedArrowDamage(helper, plainArcher, target);

		helper.assertTrue(Math.abs(plainDamage - mode) <= ARROW_DAMAGE_SPREAD, "a skeleton's arrow damage " + plainDamage + " is vanilla's");
		helper.assertTrue(Math.abs(deepDamage / multiplier - mode) <= ARROW_DAMAGE_SPREAD, "a Deep Skeleton's arrow damage " + deepDamage + " is " + multiplier + " times vanilla's");
		helper.succeed();
	}

	private static double firedArrowDamage(GameTestHelper helper, Skeleton archer, Mob target) {
		AABB around = archer.getBoundingBox().inflate(2);

		archer.performRangedAttack(target, ARROW_POWER);

		List<Arrow> arrows = helper.getLevel().getEntitiesOfClass(Arrow.class, around, arrow -> arrow.getOwner() == archer);

		helper.assertValueEqual(arrows.size(), 1, archer.getType().toShortString() + "'s arrows");

		Arrow arrow = arrows.getFirst();
		double damage = ((AbstractArrowAccessor) arrow).maple$getBaseDamage();

		arrow.discard();

		return damage;
	}

	// A skeleton of the type at the spot, as natural spawning makes it, holding a bow and standing still.
	private static <T extends AbstractSkeleton> T archer(GameTestHelper helper, EntityType<T> type, BlockPos spot) {
		T archer = spawnNaturally(helper, type, spot);

		archer.setNoAi(true);
		archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));

		return archer;
	}

	// Explodes a creeper of the type in the middle of a dirt cube and counts the dirt blocks it destroyed.
	private static int explodeCreeperAndMeasureCrater(ServerLevel level, EntityType<? extends Creeper> type, BlockPos centre) {
		BlockPos from = centre.offset(-CRATER_HALF_WIDTH, -CRATER_HALF_WIDTH, -CRATER_HALF_WIDTH);
		BlockPos to = centre.offset(CRATER_HALF_WIDTH, CRATER_HALF_WIDTH, CRATER_HALF_WIDTH);
		BlockPos pocketTop = centre.above();

		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			boolean pocket = pos.equals(centre) || pos.equals(pocketTop);

			level.setBlock(pos, (pocket ? Blocks.AIR : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
		}

		Creeper creeper = type.create(level, EntitySpawnReason.COMMAND);

		creeper.snapTo(Vec3.atBottomCenterOf(centre));
		level.addFreshEntity(creeper);
		((CreeperTestAccessor) creeper).maple$explode();

		int notDirt = 0;

		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (!level.getBlockState(pos).is(Blocks.DIRT)) notDirt++;
		}

		level.getEntitiesOfClass(Entity.class, new AABB(centre).inflate(CRATER_HALF_WIDTH + 2), entity -> !(entity instanceof Player))
			.forEach(Entity::discard);

		// The two pocket blocks were never dirt.
		return notDirt - 2;
	}

	private static int explosionPower(Creeper creeper) {
		return ((CreeperTestAccessor) creeper).maple$getExplosionRadius();
	}

	// Saves the mob as its chunk would on unloading and loads a copy, which isn't added to the level.
	private static Mob reloaded(GameTestHelper helper, Mob mob) {
		ServerLevel level = helper.getLevel();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());

		mob.save(output);

		Entity loaded = EntityType.loadEntityRecursive(
			TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()),
			level,
			EntitySpawnReason.LOAD,
			entity -> entity
		);

		helper.assertTrue(loaded instanceof Mob, "the saved mob loads");

		return (Mob) loaded;
	}

	private static void assertEnchantmentBonus(GameTestHelper helper, ResourceKey<Enchantment> enchantment, EntityType<? extends Mob> type) {
		ServerLevel level = helper.getLevel();
		Mob mob = helper.spawnWithNoFreeWill(type, MOB_POS);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), 1);

		float bonus = EnchantmentHelper.modifyDamage(level, sword, mob, level.damageSources().generic(), 0.0F);

		helper.assertValueEqual(bonus, LEVEL_ONE_DAMAGE_BONUS, enchantment.identifier().getPath() + " I's bonus against " + type.toShortString());
		mob.discard();
	}

	// Without gear, whose per-item XP bonus is random.
	private static Mob gearless(Mob mob) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			mob.setItemSlot(slot, ItemStack.EMPTY);
		}

		return mob;
	}

	// Spawned as natural spawning does, with a new group, at the test's mob spot.
	private static <T extends Mob> T spawnNaturally(GameTestHelper helper, EntityType<T> type) {
		return spawnNaturally(helper, type, MOB_POS);
	}

	// Spawned as natural spawning does, with a new group, inside the test so its cleanup removes it.
	private static <T extends Mob> T spawnNaturally(GameTestHelper helper, EntityType<T> type, BlockPos spot) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(spot);
		T mob = type.create(level, EntitySpawnReason.NATURAL);

		mob.snapTo(Vec3.atBottomCenterOf(pos));
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
		mob.setPersistenceRequired();
		level.addFreshEntityWithPassengers(mob);

		return mob;
	}

	// Spawned as natural spawning does, as an adult, inside the test so its cleanup removes it.
	private static Zombie spawnDeepZombie(GameTestHelper helper) {
		return spawnDeepZombie(helper.getLevel(), helper.absolutePos(MOB_POS));
	}

	// Spawned as natural spawning does, as an adult.
	private static Zombie spawnDeepZombie(ServerLevel level, BlockPos pos) {
		Zombie zombie = DeepVariants.DEEP_ZOMBIE.create(level, EntitySpawnReason.NATURAL);

		zombie.snapTo(Vec3.atBottomCenterOf(pos));
		zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, new Zombie.ZombieGroupData(false, false));
		zombie.setPersistenceRequired();
		level.addFreshEntityWithPassengers(zombie);

		return zombie;
	}

	// A survival player at the spot, facing south, holding a raised shield that has finished its block delay.
	private static ServerPlayer blockingPlayer(GameTestHelper helper, BlockPos spot) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);
		ItemStack shield = new ItemStack(Items.SHIELD);

		player.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(spot)), 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.OFF_HAND, shield);
		player.startUsingItem(InteractionHand.OFF_HAND);
		((LivingEntityAccessor) player).maple$setUseItemRemaining(shield.getUseDuration(player) - 20);
		helper.assertTrue(player.isBlocking(), "the player is blocking");

		return player;
	}

	private static void assertStats(GameTestHelper helper, Mob mob, Map<Holder<Attribute>, Double> expected) {
		expected.forEach((attribute, value) -> helper.assertValueEqual(mob.getAttributeBaseValue(attribute), value, describe(mob) + " " + attribute.getRegisteredName()));
		helper.assertValueEqual(mob.getHealth(), mob.getMaxHealth(), describe(mob) + " spawned at full health");
	}

	// The context LivingEntity.dropFromLootTable gives a mob's loot table when a player kills it.
	private static LootParams killedBy(ServerLevel level, Mob mob, ServerPlayer killer) {
		return new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, mob)
			.withParameter(LootContextParams.ORIGIN, mob.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
			.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
			.withParameter(LootContextParams.ATTACKING_ENTITY, killer)
			.create(LootContextParamSets.ENTITY);
	}

	private static List<String> describe(List<ItemStack> stacks) {
		return stacks.stream().map(stack -> stack.getCount() + " " + stack.getItem()).toList();
	}

	private static String describe(Mob mob) {
		return (mob.isBaby() ? "baby " : "adult ") + mob.getType().toShortString();
	}
}

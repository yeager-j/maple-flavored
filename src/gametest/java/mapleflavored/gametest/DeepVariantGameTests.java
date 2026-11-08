package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mapleflavored.deepvariants.DeepVariants;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.gametest.mixin.LivingEntityAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
	private static final int WANTED_ZOMBIES = 12;
	private static final int MAX_SPAWN_ATTEMPTS = 5_000;
	private static final int AGE_ROLLS = 4_000;
	private static final double VANILLA_BABY_ODDS = 0.05;
	private static final double BABY_ODDS_TOLERANCE = 0.02;
	private static final int SHIELD_COOLDOWN_TICKS = 100;
	private static final float SMITE_ONE_BONUS = 2.5F;
	private static final int ZOMBIE_XP = 5;
	private static final int LOOT_ROLLS = 200;

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void naturalAdultZombiesBelowY0AreDeepZombiesButBabiesStayZombies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		prepareRoom(helper, level, DEEP_ROOM);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isPitchDark(level, DEEP_ROOM), "the room below Y 0 is dark"))
			.thenExecute(() -> {
				List<Zombie> zombies = spawnZombiesNaturally(level, DEEP_ROOM);
				long deepZombies = zombies.stream().filter(zombie -> zombie.getType() == DeepVariants.DEEP_ZOMBIE).count();

				helper.assertTrue(zombies.size() >= WANTED_ZOMBIES, "natural spawning made " + zombies.size() + " zombies");
				helper.assertTrue(deepZombies > 0, "natural spawning made a Deep Zombie below Y 0");

				for (Zombie zombie : zombies) {
					boolean deep = zombie.getType() == DeepVariants.DEEP_ZOMBIE;

					helper.assertTrue(deep != zombie.isBaby(), "a natural " + describe(zombie) + " spawned below Y 0");
				}
			})
			.thenExecute(() -> unforceRoom(level, DEEP_ROOM))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 400)
	public void naturalZombiesAtOrAboveY0StayZombies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		prepareRoom(helper, level, SHALLOW_ROOM);

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isPitchDark(level, SHALLOW_ROOM), "the room above Y 0 is dark"))
			.thenExecute(() -> {
				List<Zombie> zombies = spawnZombiesNaturally(level, SHALLOW_ROOM);

				helper.assertTrue(zombies.size() >= WANTED_ZOMBIES, "natural spawning made " + zombies.size() + " zombies");
				helper.assertTrue(zombies.stream().anyMatch(zombie -> !zombie.isBaby()), "an adult zombie spawned above Y 0");

				for (Zombie zombie : zombies) {
					helper.assertTrue(zombie.getType() != DeepVariants.DEEP_ZOMBIE, "a natural Deep Zombie spawned above Y 0");
				}
			})
			.thenExecute(() -> unforceRoom(level, SHALLOW_ROOM))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void naturalSpawningSwapsOnlyAdultZombiesBelowY0InTheOverworld(GameTestHelper helper) {
		ServerLevel overworld = helper.getLevel();
		ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
		BlockPos belowZero = new BlockPos(0, -1, 0);
		Zombie.ZombieGroupData adult = new Zombie.ZombieGroupData(false, true);
		Zombie.ZombieGroupData baby = new Zombie.ZombieGroupData(true, true);

		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, belowZero, adult), DeepVariants.DEEP_ZOMBIE, adult, "an adult at Y -1");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, belowZero, baby), EntityTypes.ZOMBIE, baby, "a baby at Y -1");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, overworld, BlockPos.ZERO, adult), EntityTypes.ZOMBIE, adult, "an adult at Y 0");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.ZOMBIE, nether, belowZero, adult), EntityTypes.ZOMBIE, adult, "an adult in the Nether");
		assertNaturalSpawn(helper, DeepVariants.naturalSpawn(EntityTypes.SKELETON, overworld, belowZero, null), EntityTypes.SKELETON, null, "a skeleton at Y -1");

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

	@GameTest(environment = "maple_flavored_gametest:deep_variant_rules")
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
	public void aDeepZombieIsInEveryZombieTagAndSmiteHitsItHarder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob deepZombie = helper.spawnWithNoFreeWill(DeepVariants.DEEP_ZOMBIE, MOB_POS);
		ItemStack smiteSword = new ItemStack(Items.IRON_SWORD);
		DamageSource hit = level.damageSources().generic();

		level.registryAccess()
			.lookupOrThrow(Registries.ENTITY_TYPE)
			.getTags()
			.filter(tag -> tag.contains(EntityTypes.ZOMBIE.builtInRegistryHolder()))
			.forEach(tag -> helper.assertTrue(tag.contains(DeepVariants.DEEP_ZOMBIE.builtInRegistryHolder()), "the Deep Zombie is in " + tag.key().location()));

		smiteSword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE), 1);

		float bonus = EnchantmentHelper.modifyDamage(level, smiteSword, deepZombie, hit, 0.0F);

		helper.assertValueEqual(bonus, SMITE_ONE_BONUS, "Smite I's bonus against a Deep Zombie");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aDeepZombieDropsAZombiesLootAndTwiceItsXp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer killer = helper.makeMockServerPlayerInLevel();
		Mob deepZombie = helper.spawnWithNoFreeWill(DeepVariants.DEEP_ZOMBIE, MOB_POS);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, MOB_POS.east());
		LootParams params = killedBy(level, deepZombie, killer);
		LootTable deepTable = level.getServer().reloadableRegistries().getLootTable(deepZombie.getLootTable().orElseThrow());
		LootTable zombieTable = level.getServer().reloadableRegistries().getLootTable(zombie.getLootTable().orElseThrow());

		for (long seed = 0; seed < LOOT_ROLLS; seed++) {
			List<String> deepDrops = describe(deepTable.getRandomItems(params, RandomSource.create(seed)));
			List<String> zombieDrops = describe(zombieTable.getRandomItems(params, RandomSource.create(seed)));

			helper.assertValueEqual(deepDrops, zombieDrops, "drops with seed " + seed);
		}

		helper.assertValueEqual(zombie.getExperienceReward(level, killer), ZOMBIE_XP, "a zombie's XP");
		helper.assertValueEqual(deepZombie.getExperienceReward(level, killer), DeepVariants.XP_MULTIPLIER * ZOMBIE_XP, "a Deep Zombie's XP");
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

	// Runs vanilla's natural spawning in the room until it has made enough zombies, clearing the room after each try.
	private static List<Zombie> spawnZombiesNaturally(ServerLevel level, BlockPos centre) {
		AABB room = new AABB(centre).inflate(ROOM_HALF_WIDTH + 2);
		List<Zombie> zombies = new ArrayList<>();

		for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS && zombies.size() < WANTED_ZOMBIES; attempt++) {
			NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, centre);

			List<Entity> spawned = level.getEntitiesOfClass(Entity.class, room, entity -> !(entity instanceof Player));

			for (Entity entity : spawned) {
				boolean zombie = entity.getType() == EntityTypes.ZOMBIE || entity.getType() == DeepVariants.DEEP_ZOMBIE;

				if (zombie) zombies.add((Zombie) entity);

				entity.discard();
			}
		}

		return zombies;
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
		GameTestHelper helper, DeepVariants.NaturalSpawn spawn, EntityType<?> type, Zombie.ZombieGroupData groupData, String what
	) {
		helper.assertValueEqual(spawn.type(), type, "the type natural spawning makes for " + what);
		helper.assertTrue(spawn.groupData() == groupData, "the group data is passed on for " + what);
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

package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.dieAndRespawn;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import mapleflavored.grounded.Grounded;
import mapleflavored.remnants.Remnants;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Warden Subspaces: every Warden opens a fixed 100-block Subspace where it appears, which darkens and Grounds the
 * players inside, outlasts its Warden unloading and ends when its Warden dies or digs down. The Warden is aware of
 * everything inside, but only targets inside vanilla's awareness box keep it from digging down.
 *
 * <p>A Subspace reaches far past the few blocks between tests, so each test runs alone, in its own environment's batch,
 * in an arena high above the test grid that it first clears of whatever earlier batches left there. A mock player's
 * connection never ticks it, so the arena ticks its own players, letting their effects run out as a client's would.
 */
public class SubspaceGameTests {
	private static final double RADIUS = 100.0;
	// The Subspace's effects last 60 ticks once a player stops being topped up.
	private static final int WEAR_OFF_TICKS = 61;
	// Shortened from the 1200 ticks a Warden spawns with, so a test can watch it dig down.
	private static final int DIG_COOLDOWN_TICKS = 200;
	// How long a Warden's digging animation lasts before it's removed.
	private static final int DIGGING_TICKS = 100;
	private static final int STARTING_EXPERIENCE = 100;
	private static final int DARKNESS_FADE_TICKS = 22;

	private static final Set<UUID> TICKED_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final List<UUID> STARTED = new CopyOnWriteArrayList<>();
	private static final List<UUID> ENDED = new CopyOnWriteArrayList<>();

	static {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
				if (TICKED_PLAYERS.contains(player.getUUID())) player.doTick();
			}
		});
		Subspaces.STARTED.register((level, subspace) -> STARTED.add(subspace.boss()));
		Subspaces.ENDED.register((level, subspace) -> ENDED.add(subspace.boss()));
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_entering", maxTicks = 100)
	public void playersInsideAreDarkenedAndGroundedUntilTheyLeave(GameTestHelper helper) {
		Arena arena = Arena.cleared(helper);
		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer inside = arena.player(new Vec3(99.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer creative = arena.player(new Vec3(99.0, 0.0, 2.0), GameType.CREATIVE);
		ServerPlayer outside = arena.player(new Vec3(0.0, 0.0, 101.0), GameType.SURVIVAL);
		Subspace expected = new Subspace(warden.getUUID(), Subspace.Kind.WARDEN, arena.centre(), RADIUS);

		helper.assertValueEqual(Subspaces.containing(helper.getLevel(), arena.centre()), List.of(expected), "the Warden's Subspace");

		helper.startSequence()
			.thenExecuteAfter(1, () -> {
				assertSubspaceEffects(helper, inside, true, "a player 99 blocks out");
				assertSubspaceEffects(helper, creative, false, "a creative player 99 blocks out");
				assertSubspaceEffects(helper, outside, false, "a player 101 blocks out");
				arena.moveTo(inside, new Vec3(101.0, 0.0, 0.0));
			})
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> assertSubspaceEffects(helper, inside, false, "a player who left"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_overlap", maxTicks = 300)
	public void overlappingSubspacesEachEndOnTheirOwnAndNeverStackGrounded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Arena arena = Arena.cleared(helper);
		Warden first = arena.warden(Vec3.ZERO);
		Warden second = arena.warden(new Vec3(60.0, 0.0, 0.0));
		ServerPlayer player = arena.player(new Vec3(30.0, 0.0, 0.0), GameType.SURVIVAL);

		helper.startSequence()
			// The second Warden's Subspace opens once its chunk, outside the test's own, has loaded.
			.thenWaitUntil(() -> helper.assertValueEqual(bossesAround(level, player), List.of(first.getUUID(), second.getUUID()), "the Subspaces around the player"))
			.thenExecuteAfter(1, () -> {
				helper.assertValueEqual(player.getEffect(Grounded.EFFECT).getAmplifier(), 0, "Grounded's amplifier inside both");
				first.kill(level);
				helper.assertValueEqual(bossesAround(level, player), List.of(second.getUUID()), "the Subspaces once the first Warden dies");
			})
			.thenExecuteFor(WEAR_OFF_TICKS, () -> assertEffectsNeverRunningOut(helper, player, "a player still inside the second Subspace"))
			.thenExecute(() -> {
				second.kill(level);
				helper.assertValueEqual(bossesAround(level, player), List.of(), "the Subspaces once both Wardens die");
			})
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> {
				assertSubspaceEffects(helper, player, false, "a player once both Subspaces end");

				for (Warden warden : List.of(first, second)) {
					helper.assertValueEqual(Collections.frequency(STARTED, warden.getUUID()), 1, "starts of a Warden's Subspace");
					helper.assertValueEqual(Collections.frequency(ENDED, warden.getUUID()), 1, "ends of a Warden's Subspace");
				}
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_unloading", maxTicks = 100)
	public void aSubspaceOutlastsItsWardenUnloadingAndEndsWhenItDigsDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Arena arena = Arena.cleared(helper);
		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer player = arena.player(new Vec3(50.0, 0.0, 0.0), GameType.SURVIVAL);
		Subspace opened = new Subspace(warden.getUUID(), Subspace.Kind.WARDEN, arena.centre(), RADIUS);
		Vec3 wandered = arena.at(new Vec3(10.0, 0.0, 0.0));

		warden.snapTo(wandered.x, wandered.y, wandered.z);

		CompoundTag savedWarden = unload(helper, warden);

		helper.startSequence()
			// The effects arrive at the end of the first tick.
			.thenIdle(1)
			.thenExecuteFor(WEAR_OFF_TICKS, () -> assertEffectsNeverRunningOut(helper, player, "a player inside while the Warden is unloaded"))
			.thenExecute(() -> {
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(opened), "the Subspace while its Warden is unloaded");

				reloadSubspaces(level);
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(opened), "the Subspace once it's reloaded");

				Entity reloaded = arena.track(load(helper, savedWarden));

				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(opened), "the Subspaces once the Warden reloads");

				// How a Warden is removed as it digs back down.
				reloaded.remove(Entity.RemovalReason.DISCARDED);
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(), "the Subspaces once it digs down");
			})
			.thenSucceed();
	}

	// Peaceful never loads a saved Warden again, so neither its death nor its removal can end its Subspace. Peaceful's own
	// batch is shared, so this test leaves the other tests' players alone.
	@GameTest(environment = "maple_flavored_gametest:peaceful", maxTicks = 100)
	public void peacefulEndsTheSubspaceOfAWardenItWillNeverLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Arena arena = Arena.shared(helper);
		ServerPlayer player = arena.player(new Vec3(20.0, 0.0, 0.0), GameType.SURVIVAL);
		Warden warden = new Warden(EntityTypes.WARDEN, level);

		warden.snapTo(arena.centre().x, arena.centre().y, arena.centre().z);
		level.addFreshEntity(warden);
		helper.assertValueEqual(Subspaces.containing(level, arena.centre()).size(), 1, "the Subspaces as the Warden appears");

		// Unloaded before it ticks, as a Warden saved before the world turned Peaceful would be.
		unload(helper, warden);

		helper.startSequence()
			.thenExecuteAfter(WEAR_OFF_TICKS, () -> {
				helper.assertValueEqual(Subspaces.containing(level, arena.centre()), List.of(), "the Subspaces in Peaceful");
				assertSubspaceEffects(helper, player, false, "a player inside in Peaceful");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_remnants", maxTicks = 100)
	public void aRemnantInsideStaysWhereItsOwnerDiedUnlessItCantBeCollectedThere(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Arena arena = Arena.cleared(helper);
		ServerPlayer onTheFloor = arena.player(new Vec3(10.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer inLava = arena.player(new Vec3(10.0, 0.0, 6.0), GameType.SURVIVAL);
		BlockPos floorSpot = arena.blockAt(10, 0, 0);
		BlockPos solidGround = arena.blockAt(10, 0, 6);
		BlockPos lavaSpot = arena.blockAt(13, 0, 6);

		arena.warden(Vec3.ZERO);
		onTheFloor.giveExperiencePoints(STARTING_EXPERIENCE);
		inLava.giveExperiencePoints(STARTING_EXPERIENCE);
		standAt(level, onTheFloor, floorSpot);
		standAt(level, inLava, solidGround);

		helper.startSequence()
			// Lets the last solid ground be recorded.
			.thenIdle(1)
			.thenExecute(() -> {
				arena.track(dieAndRespawn(helper, onTheFloor));
				helper.assertValueEqual(remnantsAt(level, floorSpot), 1, "the Remnants where the player died on the floor");

				level.setBlock(lavaSpot, Blocks.LAVA.defaultBlockState(), 3);
				standAt(level, inLava, lavaSpot);
				arena.track(dieAndRespawn(helper, inLava));
				helper.assertValueEqual(remnantsAt(level, lavaSpot), 0, "the Remnants in the lava");
				helper.assertValueEqual(remnantsAt(level, solidGround), 1, "the Remnants on the last solid ground");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_respawn")
	public void deathInsideRespawnsAtARespawnPointInside(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Arena arena = Arena.cleared(helper);
		ServerPlayer player = arena.player(new Vec3(20.0, 0.0, 0.0), GameType.SURVIVAL);
		BlockPos respawnPoint = arena.blockAt(5, 0, 0);
		var respawnData = LevelData.RespawnData.of(level.dimension(), respawnPoint, 0.0F, 0.0F);

		arena.warden(Vec3.ZERO);
		player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawnData, true), false);

		ServerPlayer respawned = arena.track(dieAndRespawn(helper, player));

		helper.assertValueEqual(respawned.blockPosition(), respawnPoint, "where the player respawns");
		helper.assertValueEqual(Subspaces.containing(level, respawned.position()).size(), 1, "the Subspaces around the respawned player");
		helper.succeed();
	}

	// Every target starts outside vanilla's awareness box, about 24 blocks around the Warden, so the Warden can only know
	// of them through its Subspace. A player comes first even when a mob is closer.
	@GameTest(environment = "maple_flavored_gametest:subspace_nearest_player", maxTicks = 600)
	public void theWardenGoesForTheNearestPlayerAnywhereInItsSubspace(GameTestHelper helper) {
		Arena arena = Arena.cleared(helper);

		arena.floor(-70, 70);

		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer nearer = arena.player(new Vec3(35.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer farther = arena.player(new Vec3(-60.0, 0.0, 0.0), GameType.SURVIVAL);
		double startingDistanceToFarther = warden.distanceTo(farther);

		arena.stillPig(new Vec3(-30.0, 0.0, 0.0));

		helper.succeedWhen(() -> {
			helper.assertTrue(warden.distanceTo(nearer) <= 25.0, "the Warden closed on the nearer player, " + warden.distanceTo(nearer) + " blocks away");
			helper.assertTrue(warden.distanceTo(farther) > startingDistanceToFarther, "the Warden moved away from the farther player");
			helper.assertValueEqual(warden.getAngerManagement().getActiveAnger(nearer), 0, "the Warden's anger at a player it hasn't sniffed up close");
		});
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_non_players", maxTicks = 600)
	public void theWardenIsAwareOfMobsAnywhereInItsSubspace(GameTestHelper helper) {
		Arena arena = Arena.cleared(helper);

		arena.floor(-10, 50);

		Warden warden = arena.warden(Vec3.ZERO);
		Pig pig = arena.stillPig(new Vec3(40.0, 0.0, 0.0));

		helper.succeedWhen(() -> helper.assertTrue(warden.distanceTo(pig) <= 30.0, "the Warden closed on the pig, " + warden.distanceTo(pig) + " blocks away"));
	}

	@GameTest(environment = "maple_flavored_gametest:subspace_burrow_far", maxTicks = 700)
	public void aTargetKnownOnlyThroughTheSubspaceDoesntKeepTheWardenFromDiggingDown(GameTestHelper helper) {
		Arena arena = Arena.cleared(helper);

		arena.pen();

		Warden warden = arena.warden(Vec3.ZERO);

		shortenDigCooldown(warden);
		arena.player(new Vec3(40.0, 0.0, 0.0), GameType.SURVIVAL);

		helper.succeedWhen(() -> {
			helper.assertTrue(warden.isRemoved(), "the Warden dug down");
			helper.assertValueEqual(Subspaces.containing(helper.getLevel(), arena.centre()), List.of(), "the Subspaces once it dug down");
		});
	}

	// The Warden's hitbox reaches 0.45 blocks out, and vanilla's box 24 blocks past that. A player's hitbox reaches 0.3
	// blocks back from where they stand, so a player 24.68 blocks out overlaps the box by 0.07, more than the 0.05 the
	// Warden can shift within its pen. The centre of the block they stand in, 25 blocks out, lies outside the box.
	@GameTest(environment = "maple_flavored_gametest:subspace_burrow_near", maxTicks = 500)
	public void aTargetJustInsideVanillasAwarenessBoxKeepsTheWardenFromDiggingDown(GameTestHelper helper) {
		Arena arena = Arena.cleared(helper);

		arena.pen();

		Warden warden = arena.warden(Vec3.ZERO);

		shortenDigCooldown(warden);
		arena.player(new Vec3(24.68, 0.0, 0.0), GameType.SURVIVAL);

		helper.startSequence()
			.thenExecuteAfter(DIG_COOLDOWN_TICKS + DIGGING_TICKS + 100, () -> {
				helper.assertFalse(warden.isRemoved(), "the Warden is still here");
				helper.assertValueEqual(Subspaces.containing(helper.getLevel(), arena.centre()).size(), 1, "the Subspaces while it stays");
			})
			.thenSucceed();
	}

	private static void assertSubspaceEffects(GameTestHelper helper, ServerPlayer player, boolean expected, String who) {
		helper.assertValueEqual(player.hasEffect(MobEffects.DARKNESS), expected, "whether " + who + " has Darkness");
		helper.assertValueEqual(player.hasEffect(Grounded.EFFECT), expected, "whether " + who + " is Grounded");
	}

	// Checked every tick: Darkness fades out over its last 22 ticks, so it must never get that close to ending while the
	// player stays inside.
	private static void assertEffectsNeverRunningOut(GameTestHelper helper, ServerPlayer player, String who) {
		assertSubspaceEffects(helper, player, true, who);
		helper.assertFalse(player.getEffect(MobEffects.DARKNESS).endsWithin(DARKNESS_FADE_TICKS), "whether the Darkness of " + who + " is fading");
		helper.assertFalse(player.getEffect(Grounded.EFFECT).endsWithin(DARKNESS_FADE_TICKS), "whether " + who + " is about to stop being Grounded");
	}

	private static List<UUID> bossesAround(ServerLevel level, ServerPlayer player) {
		return Subspaces.containing(level, player.position()).stream().map(Subspace::boss).toList();
	}

	private static void shortenDigCooldown(Warden warden) {
		warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, DIG_COOLDOWN_TICKS);
	}

	// Saves the level's Subspaces as the world does and reads them back, as reopening the world would.
	private static void reloadSubspaces(ServerLevel level) {
		Codec<List<Subspace>> codec = Subspaces.ATTACHMENT.persistenceCodec();
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Tag saved = codec.encodeStart(ops, level.getAttachedOrElse(Subspaces.ATTACHMENT, List.of())).getOrThrow();

		level.setAttached(Subspaces.ATTACHMENT, codec.parse(ops, saved).getOrThrow());
	}

	// Saves the entity the way its chunk would on unloading, and removes it as unloading does.
	private static CompoundTag unload(GameTestHelper helper, Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());

		entity.save(output);
		entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

		return output.buildResult();
	}

	private static Entity load(GameTestHelper helper, CompoundTag saved) {
		var input = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved);
		Entity loaded = EntityType.loadEntityRecursive(input, helper.getLevel(), EntitySpawnReason.LOAD, entity -> entity);

		helper.assertTrue(loaded != null, "the saved entity loads");
		helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "the saved entity is added back");

		return loaded;
	}

	// The server takes a player's on-ground flag from their client's movement, which a mock player never sends.
	private static void standAt(ServerLevel level, ServerPlayer player, BlockPos spot) {
		level.setBlock(spot.below(), Blocks.STONE.defaultBlockState(), 3);
		teleport(player, Vec3.atBottomCenterOf(spot));
		player.setOnGround(true);
	}

	// Moves the player as the server does, so the chunks around them load. A player who has moved is invulnerable until
	// their client's first movement arrives, which a mock never sends, so that wait is cleared.
	private static void teleport(ServerPlayer player, Vec3 position) {
		player.teleport(new TeleportTransition(player.level(), position, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		player.hasChangedDimension();
	}

	private static int remnantsAt(ServerLevel level, BlockPos spot) {
		return level.getEntities(Remnants.REMNANT, new AABB(spot), Entity::isAlive).size();
	}

	/**
	 * One test's arena, 80 blocks above its test structure: clear of the runways other tests build, with the test's
	 * Wardens and players placed relative to its centre.
	 *
	 * <p>Whether the test passes or fails, the arena discards its Wardens, logs out its players and ends every Subspace
	 * in the level, so nothing it opened reaches a later batch.
	 */
	private static final class Arena {
		private static final int HEIGHT = 80;
		// Past the Subspace's reach, so nothing an earlier batch left is aware of the arena or affected by it.
		private static final double CLEARED_RADIUS = RADIUS + 10.0;
		// How far along the arena's x axis its chunks are kept loaded, past every Warden and target the tests place.
		private static final int LOADED_REACH = 80;

		private static final BlockPos RELATIVE_CENTRE = new BlockPos(1, HEIGHT, 1);

		private final GameTestHelper helper;
		private final ServerLevel level;
		private final BlockPos centreBlock;
		private final List<Entity> mobs = new ArrayList<>();
		private final Set<UUID> players = ConcurrentHashMap.newKeySet();

		private Arena(GameTestHelper helper) {
			this.helper = helper;
			this.level = helper.getLevel();
			this.centreBlock = helper.absolutePos(RELATIVE_CENTRE);

			// Every arena stands its Wardens on a floor.
			level.setBlock(centreBlock.below(), Blocks.STONE.defaultBlockState(), 3);
			keepLoaded();
			cleanUpWhenDone();
		}

		/**
		 * An arena for a test running alone in its batch, cleared of the players, mobs and Subspaces earlier batches
		 * left behind.
		 */
		static Arena cleared(GameTestHelper helper) {
			Arena arena = new Arena(helper);
			PlayerList playerList = helper.getLevel().getServer().getPlayerList();

			List.copyOf(playerList.getPlayers()).forEach(playerList::remove);
			arena.level.getEntitiesOfClass(
				LivingEntity.class,
				AABB.ofSize(arena.centre(), CLEARED_RADIUS * 2.0, CLEARED_RADIUS * 2.0, CLEARED_RADIUS * 2.0),
				entity -> !(entity instanceof ServerPlayer)
			).forEach(Entity::discard);
			arena.level.setAttached(Subspaces.ATTACHMENT, List.of());

			return arena;
		}

		/** An arena for a test sharing its batch with others, so it leaves their players and mobs alone. */
		static Arena shared(GameTestHelper helper) {
			return new Arena(helper);
		}

		/** Where the arena's first Warden stands. */
		Vec3 centre() {
			return Vec3.atBottomCenterOf(centreBlock);
		}

		Vec3 at(Vec3 offset) {
			return centre().add(offset);
		}

		BlockPos blockAt(int x, int y, int z) {
			return centreBlock.offset(x, y, z);
		}

		/** A Warden that can dig back down, standing at the offset from the centre. */
		Warden warden(Vec3 offset) {
			Warden warden = helper.spawnEntity(EntityTypes.WARDEN, relativeAt(offset))
				.requirePersistence(false)
				.spawnReason(EntitySpawnReason.MOB_SUMMONED)
				.spawn();

			return track(warden);
		}

		/** A pig that stays where it's put. */
		Pig stillPig(Vec3 offset) {
			Pig pig = track(helper.spawn(EntityTypes.PIG, relativeAt(offset)));

			pig.setNoAi(true);

			return pig;
		}

		// The helper spawns entities relative to the test.
		private Vec3 relativeAt(Vec3 offset) {
			return Vec3.atBottomCenterOf(RELATIVE_CENTRE).add(offset);
		}

		/**
		 * A loaded player standing at the offset from the centre, ticked each tick as a client would tick it. Ticking moves
		 * it as a client would, so it falls unless it stands on something.
		 */
		ServerPlayer player(Vec3 offset, GameType gameType) {
			ServerPlayer player = track(MockPlayers.survivalPlayer(helper));

			player.setGameMode(gameType);
			moveTo(player, offset);

			return player;
		}

		/** Moves the player onto a block at the offset from the centre. */
		void moveTo(ServerPlayer player, Vec3 offset) {
			Vec3 feet = at(offset);

			level.setBlock(BlockPos.containing(feet).below(), Blocks.STONE.defaultBlockState(), 3);
			teleport(player, feet);
		}

		/** Lays a three-block-wide floor along the arena's x axis, from {@code fromX} to {@code toX} blocks out. */
		void floor(int fromX, int toX) {
			for (int x = fromX; x <= toX; x++) {
				for (int z = -1; z <= 1; z++) {
					level.setBlock(blockAt(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
				}
			}
		}

		/** Walls in the centre so a Warden there can't walk away. */
		void pen() {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && z == 0) continue;

					for (int y = 0; y < 3; y++) {
						level.setBlock(blockAt(x, y, z), Blocks.STONE.defaultBlockState(), 3);
					}
				}
			}
		}

		/** Includes the entity, or a player's respawned replacement, in the arena's cleanup. */
		<E extends Entity> E track(E entity) {
			if (entity instanceof ServerPlayer player) {
				players.add(player.getUUID());
				TICKED_PLAYERS.add(player.getUUID());
			} else {
				mobs.add(entity);
			}

			return entity;
		}

		// Only the test's own chunks are kept loaded for it. An entity anywhere else wouldn't tick, and neither a Warden's
		// sensor nor its Subspace could find it. The runner releases every forced chunk as each batch ends, just before it
		// forces the next batch's, so the arena never releases them itself: that would unload the next test's chunks.
		private void keepLoaded() {
			int fromX = SectionPos.blockToSectionCoord(centreBlock.getX() - LOADED_REACH);
			int toX = SectionPos.blockToSectionCoord(centreBlock.getX() + LOADED_REACH);
			int fromZ = SectionPos.blockToSectionCoord(centreBlock.getZ() - 1);
			int toZ = SectionPos.blockToSectionCoord(centreBlock.getZ() + 1);

			for (int x = fromX; x <= toX; x++) {
				for (int z = fromZ; z <= toZ; z++) {
					level.setChunkForced(x, z, true);
				}
			}
		}

		private void cleanUpWhenDone() {
			GameTestInfo testInfo = ((GameTestHelperAccessor) helper).maple$testInfo();

			testInfo.addListener(new GameTestListener() {
				@Override
				public void testStructureLoaded(GameTestInfo info) {
				}

				@Override
				public void testPassed(GameTestInfo info, GameTestRunner runner) {
					cleanUp();
				}

				@Override
				public void testFailed(GameTestInfo info, GameTestRunner runner) {
					cleanUp();
				}

				@Override
				public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
				}
			});
		}

		private void cleanUp() {
			PlayerList playerList = level.getServer().getPlayerList();

			mobs.stream().filter(mob -> !mob.isRemoved()).forEach(Entity::discard);

			for (UUID uuid : players) {
				ServerPlayer player = playerList.getPlayer(uuid);

				if (player != null) playerList.remove(player);

				TICKED_PLAYERS.remove(uuid);
			}

			level.setAttached(Subspaces.ATTACHMENT, List.of());
		}
	}
}

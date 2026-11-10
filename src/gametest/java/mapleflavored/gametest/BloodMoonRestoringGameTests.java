package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mapleflavored.bloodmoons.RestoreSpot;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.Restoring;
import mapleflavored.bloodmoons.RestoringAccess;
import mapleflavored.mobs.GoneForGood;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.subspaces.Subspace;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Moon Restoring: which mobs structures give spots as they generate, which spots empty, how a Blood Moon makes
 * them pending, and when a pending spot fills.
 *
 * <p>Each Restoring test builds its spot on a small walled floor far from the test area, in a chunk it keeps loaded, so
 * no other test's mock players come within {@link Restoring#PLAYER_CLEARANCE} of it. A spot is made the way generation
 * makes one: by adding its mob while its structure builds. Building a site waits until its chunk ticks entities and has
 * loaded its saved ones, so every mob a test places there is tracked from the start. Spots only fill once a chunk's
 * entities have loaded, so tests still wait for that before checking that a spot stays pending, and wait for a fill
 * rather than counting ticks.
 *
 * <p>A test's Blood Moon rises over {@linkplain Spots#riseBloodMoon its own spots alone}, so no other test's Blood Moon
 * can make its empty spots pending. {@code BloodMoonGameTests} checks that a Blood Moon in the sky reaches the spots.
 *
 * <p>A crash is a level whose saved spots are older than its chunks. Tests share the level's spots, so a test makes one
 * by putting back only its own spot as it was saved earlier, or by dropping it.
 */
public class BloodMoonRestoringGameTests {
	private static final int FLOOR_Y = 250;
	// In the air of the flat test world, where a test builds or checks at its sites.
	private static final int SITE_Y = 100;
	// Loading a site's entities waits on disk, while an idle test server runs through ticks as fast as it can.
	private static final int SITE_TICKS = 20_000;
	// Long enough for a retry to come round once the site's entities have loaded.
	private static final int RETRY_WAIT = Restoring.RETRY_INTERVAL + 10;

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aKilledMobReturnsAfterTheNextBloodMoon(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.kill(site.level());
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.EMPTY, "the spot once its mob died");
		Spots.riseBloodMoon(helper);
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot as a Blood Moon rises");

		helper.startSequence()
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "Restored mobs"))
			.thenExecute(() -> {
				Mob restored = site.mobs().getFirst();

				helper.assertValueEqual(restored.getType(), EntityTypes.VINDICATOR, "the Restored mob");
				helper.assertTrue(restored.isPersistenceRequired(), "the Restored mob keeps the original's persistence");
				helper.assertValueEqual(Spots.of(helper, restored).id(), spot.id(), "the Restored mob's spot");
				helper.assertValueEqual(Spots.current(helper.getLevel(), spot).occupant(), Optional.of(restored.getUUID()), "the spot's occupant");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aMobKilledAfterABloodMoonWaitsForTheNextOne(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		Spots.riseBloodMoon(helper);
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.OCCUPIED, "the spot through a Blood Moon");
		original.kill(site.level());
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.EMPTY, "the spot of a mob that died after it rose");
		Spots.riseBloodMoon(helper);
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot as the next Blood Moon rises");

		helper.startSequence()
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "Restored mobs"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aMobThatWanderedOffStillHoldsItsSpot(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		BlockPos ledge = site.floor().offset(5, 0, 0);

		// Out of the site, on a ledge of its own, where it stays put.
		site.level().setBlockAndUpdate(ledge, Blocks.GLASS.defaultBlockState());
		original.setNoAi(true);
		original.teleportTo(ledge.getX() + 0.5, ledge.getY() + 1, ledge.getZ() + 0.5);
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.OCCUPIED, "the spot of a mob that wandered off");
				site.assertMobs(helper, 0, "mobs Restored to the spot");
				original.discard();
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aConvertedMobEmptiesItsSpotAndReturnsAsTheOriginal(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.POLISHED_BLACKSTONE_BRICKS);
		Mob original = site.placeOriginal(Restorable.BASTION, EntityTypes.PIGLIN_BRUTE);
		RestoreSpot spot = Spots.of(helper, original);

		helper.startSequence()
			// A conversion removes the original as it ticks, which needs its entities loaded.
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecute(() -> {
				Mob zombified = original.convertTo(EntityTypes.ZOMBIFIED_PIGLIN, ConversionParams.single(original, true, true), converted -> {});

				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.EMPTY, "the spot once its Brute converted");
				zombified.discard();
				Spots.riseBloodMoon(helper);
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "Restored mobs"))
			.thenExecute(() -> {
				helper.assertValueEqual(site.mobs().getFirst().getType(), EntityTypes.PIGLIN_BRUTE, "the Restored mob");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aSpotWhoseBlockIsGoneWaitsUntilItsBack(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.kill(site.level());
		site.level().setBlockAndUpdate(site.floor(), Blocks.STONE.defaultBlockState());
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot with stone beneath");
				site.assertMobs(helper, 0, "mobs Restored onto stone");
				site.level().setBlockAndUpdate(site.floor(), Blocks.COBBLESTONE.defaultBlockState());
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored once the cobblestone is back"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aBlockedSpotWaitsUntilItsClear(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.kill(site.level());
		site.level().setBlockAndUpdate(site.floor().above(), Blocks.GLASS.defaultBlockState());
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the blocked spot");
				site.level().removeBlock(site.floor().above(), false);
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored once the spot is clear"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aMobStandingInTheSpotKeepsItPendingUntilItMoves(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		Mob squatter = EntityTypes.COW.create(site.level(), EntitySpawnReason.COMMAND);

		original.kill(site.level());
		squatter.setNoAi(true);
		squatter.snapTo(site.feet().x, site.feet().y, site.feet().z, 0.0F, 0.0F);
		site.level().addFreshEntity(squatter);
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot a cow stands in");
				squatter.teleportTo(site.feet().x + 2, site.feet().y, site.feet().z + 2);
			})
			.thenWaitUntil(() -> helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.OCCUPIED, "the spot once the cow moved"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest
	public void checkingASpotsBlockLoadsNoChunks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// A site nothing has loaded.
		Vec3 spot = Vec3.atCenterOf(TestSites.of(helper).chunk(0).getMiddleBlockPosition(SITE_Y));
		ChunkPos chunk = ChunkPos.containing(BlockPos.containing(spot));

		for (Restorable structure : Restorable.values()) {
			helper.assertFalse(structure.holds(level, spot), structure + " holds a spot in an unloaded chunk");
		}

		ChunkPos.rangeClosed(chunk, 2).forEach(near -> helper.assertFalse(level.hasChunk(near.x(), near.z()), "a chunk near the spot is loaded"));
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aNearbyPlayerKeepsASpotPendingUntilTheyLeave(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		site.putPlayer(player, 20);
		original.kill(site.level());
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot with a player 20 blocks away");
				site.putPlayer(player, 40);
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored once the player left"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	// Its own environment, since it moves the clock.
	@GameTest(environment = "maple_flavored_gametest:restoring_dawn", maxTicks = SITE_TICKS)
	public void aPendingSpotStillFillsAfterDawn(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		site.putPlayer(player, 10);
		original.kill(site.level());
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot during the night");
				setOverworldClock(helper, 2L * 24_000 + 1_000);
			})
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot after dawn");
				site.putPlayer(player, 40);
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored the morning after"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void missedBloodMoonsDontStack(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		site.putPlayer(player, 10);
		original.kill(site.level());
		Spots.riseBloodMoon(helper);
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> site.putPlayer(player, 40))
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored after two Blood Moons"))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				site.assertMobs(helper, 1, "mobs a retry later");
				Spots.riseBloodMoon(helper);
			})
			.thenExecuteAfter(RETRY_WAIT, () -> {
				site.assertMobs(helper, 1, "mobs after a Blood Moon over an occupied spot");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aPendingSpotSurvivesAReload(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.kill(site.level());
		Spots.riseBloodMoon(helper);
		reloadSpots(site.level());
		helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot after a reload");

		helper.startSequence()
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored after a reload"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aReloadedMobStillHoldsItsSpotAndEmptiesItWhenKilled(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		helper.startSequence()
			.thenExecuteAfter(1, site::letUnload)
			.thenWaitUntil(() -> helper.assertFalse(site.isLoaded(), "the site's chunk is loaded"))
			.thenExecute(() -> {
				Spots.riseBloodMoon(helper);
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.OCCUPIED, "the spot of an unloaded mob");
				site.keepLoaded();
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs loaded back with the site"))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				site.assertMobs(helper, 1, "mobs a retry after the original loaded back");

				Mob reloaded = site.mobs().getFirst();

				helper.assertValueEqual(reloaded.getUUID(), original.getUUID(), "the mob that loaded back");
				helper.assertValueEqual(Spots.of(helper, reloaded).id(), spot.id(), "the reloaded mob's spot");
				reloaded.kill(site.level());
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.EMPTY, "the spot once the reloaded mob died");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aPendingSpotLoadsNothingAndFillsWhenItsChunkLoads(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.kill(site.level());

		helper.startSequence()
			.thenExecuteAfter(1, site::release)
			.thenWaitUntil(() -> helper.assertFalse(site.isLoaded(), "the site's chunk is loaded"))
			.thenExecute(() -> {
				Spots.riseBloodMoon(helper);
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.PENDING, "the spot in an unloaded chunk");
			})
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertFalse(site.isLoaded(), "the site's chunk is loaded after a Blood Moon and a retry");
				site.keepLoaded();
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored once the chunk loaded"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aSpotACrashLeftOccupiedEmptiesWhenItsChunkLoadsAndIsRestored(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		Tag savedWhileOccupied = savedSpots(site.level()).get(indexOf(site.level(), spot)).copy();

		original.kill(site.level());
		// Gone at once, so the chunk doesn't save it dying.
		original.discard();

		helper.startSequence()
			.thenExecuteAfter(1, site::release)
			.thenWaitUntil(() -> helper.assertFalse(site.isLoaded(), "the site's chunk is loaded"))
			.thenExecute(() -> {
				ListTag saved = savedSpots(site.level());

				saved.set(indexOf(site.level(), spot), savedWhileOccupied);
				loadSpots(site.level(), saved);
				Spots.riseBloodMoon(helper);
				helper.assertValueEqual(Spots.current(site.level(), spot), spot, "the spot the crash left, through a Blood Moon");
				site.keepLoaded();
			})
			.thenWaitUntil(() -> helper.assertTrue(Spots.current(site.level(), spot).occupant().isEmpty(), "the spot has let go of its dead mob"))
			.thenExecute(() -> Spots.riseBloodMoon(helper))
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored after the crash"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aMobSavedInAnotherChunkKeepsItsSpotWhenTheSpotsChunkLoads(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Site elsewhere = Site.build(helper, 1, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);

		original.setNoAi(true);
		original.teleportTo(elsewhere.feet().x, elsewhere.feet().y, elsewhere.feet().z);

		helper.startSequence()
			.thenExecuteAfter(1, () -> {
				site.release();
				elsewhere.letUnload();
			})
			.thenWaitUntil(() -> helper.assertFalse(site.isLoaded() || elsewhere.isLoaded(), "a site's chunk is loaded"))
			.thenExecute(site::keepLoaded)
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				helper.assertFalse(elsewhere.isLoaded(), "the chunk the mob is saved in loaded");
				helper.assertValueEqual(stateOf(helper, spot), RestoreSpot.State.OCCUPIED, "the spot of a mob saved in another chunk");
				site.release();
				elsewhere.keepLoaded();
			})
			.thenWaitUntil(() -> elsewhere.assertMobs(helper, 1, "mobs where the original was saved"))
			.thenExecute(elsewhere::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aSavedMobWhoseSpotWasNeverSavedGetsItBack(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		Entity reloaded = reloaded(site.level(), original);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertReady(helper))
			.thenExecute(() -> {
				ListTag saved = savedSpots(site.level());

				// Its chunk unloads, as far as Restoring can tell, and the saved copy comes back to spots without its own.
				original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
				saved.remove(indexOf(site.level(), spot));
				loadSpots(site.level(), saved);
				helper.assertTrue(RestoringAccess.spotOf(site.level().getServer(), reloaded).isEmpty(), "the spot is still saved");

				site.level().addFreshEntity(reloaded);
				helper.assertValueEqual(Spots.of(helper, reloaded), spot, "the spot of a mob that loaded without one saved");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aSavedMobThatFailsToLoadLeavesTheSpotThatWasNeverSavedEmpty(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		ValueInput savedOriginal = saved(site.level(), original);
		ListTag saved = savedSpots(site.level());

		original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		saved.remove(indexOf(site.level(), spot));
		loadSpots(site.level(), saved);
		// As Peaceful refuses a saved Vindicator.
		GoneForGood.failedToLoad(site.level(), savedOriginal);

		helper.startSequence()
			.thenExecuteAfter(1, () -> {
				helper.assertTrue(indexOf(site.level(), spot) >= 0, "the spot of a mob that failed to load is recorded");
				helper.assertValueEqual(Spots.current(site.level(), spot).position(), spot.position(), "where the spot was recorded");
				helper.assertTrue(Spots.current(site.level(), spot).occupant().isEmpty(), "the spot of a mob that never loaded is empty");
				Spots.riseBloodMoon(helper);
			})
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "mobs Restored to the spot"))
			.thenExecute(site::release)
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aRestoredMobKeepsTheSpotACrashLeftWithItsDeadPredecessor(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		Tag savedWithOriginal = savedSpots(site.level()).get(indexOf(site.level(), spot)).copy();

		original.kill(site.level());
		original.discard();
		Spots.riseBloodMoon(helper);

		helper.startSequence()
			.thenWaitUntil(() -> site.assertMobs(helper, 1, "Restored mobs"))
			.thenExecute(() -> {
				Mob restored = site.mobs().getFirst();
				Entity reloaded = reloaded(site.level(), restored);
				ListTag saved = savedSpots(site.level());

				// The Restored mob's chunk reached the disk, and the spots that say it holds the spot didn't.
				restored.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
				saved.set(indexOf(site.level(), spot), savedWithOriginal);
				loadSpots(site.level(), saved);
				site.level().addFreshEntity(reloaded);

				helper.assertValueEqual(Spots.current(site.level(), spot).occupant(), Optional.of(reloaded.getUUID()), "the spot's occupant");
				helper.assertValueEqual(Spots.of(helper, reloaded).id(), spot.id(), "the Restored mob's spot");
				Spots.riseBloodMoon(helper);
			})
			.thenExecuteAfter(RETRY_WAIT, () -> {
				site.assertMobs(helper, 1, "mobs a Blood Moon and a retry later");
				site.release();
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aMobTheLevelRefusesLeavesNoSpot(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		Mob refused = EntityTypes.VINDICATOR.create(site.level(), EntitySpawnReason.STRUCTURE);
		int spots = RestoringAccess.spots(site.level()).size();

		// The level refuses a second entity with a UUID it already holds.
		refused.setUUID(original.getUUID());
		refused.snapTo(site.feet().x, site.feet().y, site.feet().z, 0.0F, 0.0F);
		Restoring.build(
			Optional.of(Restorable.WOODLAND_MANSION),
			() -> helper.assertFalse(site.level().addFreshEntity(refused), "the level took a second mob with the same UUID")
		);

		helper.assertValueEqual(RestoringAccess.spots(site.level()).size(), spots, "spots after a structure's mob was refused");
		site.release();
		helper.succeed();
	}

	// Its own environment, since it changes the difficulty.
	@RunsAlone
	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void peacefulRestoresNoMobItRefusesAndDropsTheOnesItLoads(GameTestHelper helper) {
		Site bruteSite = Site.build(helper, 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
		Site vindicatorSite = Site.build(helper, 2, Blocks.COBBLESTONE);
		Mob brute = bruteSite.placeOriginal(Restorable.BASTION, EntityTypes.PIGLIN_BRUTE);
		Mob vindicator = vindicatorSite.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot bruteSpot = Spots.of(helper, brute);
		RestoreSpot vindicatorSpot = Spots.of(helper, vindicator);
		ValueInput savedVindicator = saved(vindicatorSite.level(), vindicator);
		MinecraftServer server = helper.getLevel().getServer();

		helper.assertFalse(EntityTypes.PIGLIN_BRUTE.isAllowedInPeaceful(), "Brutes are allowed in Peaceful");

		brute.kill(bruteSite.level());
		vindicator.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		server.setDifficulty(Difficulty.PEACEFUL, true);
		Spots.riseBloodMoon(helper);

		// Peaceful refuses the saved Vindicator as its chunk would load it.
		Optional<Entity> loaded = EntityType.create(savedVindicator, vindicatorSite.level(), new EntitySpawnRequest(EntitySpawnReason.LOAD, false));

		helper.assertTrue(loaded.isEmpty(), "Peaceful loads a saved Vindicator");

		helper.startSequence()
			.thenWaitUntil(() -> bruteSite.assertReady(helper))
			.thenExecuteAfter(RETRY_WAIT, () -> {
				bruteSite.assertMobs(helper, 0, "Brutes Restored in Peaceful");
				helper.assertValueEqual(stateOf(helper, bruteSpot), RestoreSpot.State.PENDING, "the Brute's spot in Peaceful");
				helper.assertValueEqual(stateOf(helper, vindicatorSpot), RestoreSpot.State.EMPTY, "the dropped Vindicator's spot");
				server.setDifficulty(Difficulty.NORMAL, true);
			})
			.thenWaitUntil(() -> bruteSite.assertMobs(helper, 1, "Brutes Restored back on Normal"))
			.thenExecute(() -> List.of(bruteSite, vindicatorSite).forEach(Site::release))
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void aBastionGivesItsBrutesSpotsButNotItsPiglinsOrHoglins(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart bastion = Structures.place(level, BuiltinStructures.BASTION_REMNANT, TestSites.of(helper).chunk(0));
		List<Mob> others = level.getEntitiesOfClass(
			Mob.class, AABB.of(bastion.getBoundingBox()), mob -> mob.getType() == EntityTypes.PIGLIN || mob.getType() == EntityTypes.HOGLIN
		);

		assertMobsHoldSpots(helper, level, bastion, Restorable.BASTION);
		helper.assertFalse(others.isEmpty(), "the Bastion placed Piglins or Hoglins");

		for (Mob other : others) {
			helper.assertTrue(RestoringAccess.spotOf(level.getServer(), other).isEmpty(), "a " + other.getType().toShortString() + " holds a spot");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void aWoodlandMansionGivesItsIllagersSpotsButNotItsAllays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<WoodlandMansionPieces.WoodlandMansionPiece> pieces = new ArrayList<>();

		WoodlandMansionPieces.generateMansion(level.getStructureTemplateManager(), TestSites.of(helper).chunk(0).getBlockAt(0, SITE_Y, 0), Rotation.NONE, pieces, level.getRandom());

		StructureStart mansion = placePieces(level, BuiltinStructures.WOODLAND_MANSION, pieces);

		assertMobsHoldSpots(helper, level, mansion, Restorable.WOODLAND_MANSION);

		for (Allay allay : level.getEntitiesOfClass(Allay.class, AABB.of(mansion.getBoundingBox()))) {
			helper.assertTrue(RestoringAccess.spotOf(level.getServer(), allay).isEmpty(), "an Allay holds a spot");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void anOceanMonumentGivesItsElderGuardiansSpots(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkPos chunk = TestSites.of(helper).chunk(0);
		StructurePiece building = new OceanMonumentPieces.MonumentBuilding(level.getRandom(), chunk.getMinBlockX(), chunk.getMinBlockZ(), Direction.NORTH);
		StructureStart monument = placePieces(level, BuiltinStructures.OCEAN_MONUMENT, List.of(building));

		// A Monument fills with water only below sea level, which the flat test world sets at its floor.
		for (ElderGuardian elder : level.getEntitiesOfClass(ElderGuardian.class, AABB.of(monument.getBoundingBox()))) {
			flood(level, elder.blockPosition());
		}

		helper.assertValueEqual(assertMobsHoldSpots(helper, level, monument, Restorable.OCEAN_MONUMENT), 3, "Elder Guardians with spots");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void anEndCityGivesItsShulkersSpots(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<StructurePiece> pieces = new ArrayList<>();

		EndCityPieces.startHouseTower(level.getStructureTemplateManager(), TestSites.of(helper).chunk(0).getBlockAt(0, SITE_Y, 0), Rotation.NONE, pieces, level.getRandom());

		StructureStart city = placePieces(level, BuiltinStructures.END_CITY, pieces);

		assertMobsHoldSpots(helper, level, city, Restorable.END_CITY);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = SITE_TICKS)
	public void aSavedSpotThatCantBeReadIsLostAlone(GameTestHelper helper) {
		Site site = Site.build(helper, 0, Blocks.COBBLESTONE);
		Mob original = site.placeOriginal(Restorable.WOODLAND_MANSION, EntityTypes.VINDICATOR);
		RestoreSpot spot = Spots.of(helper, original);
		ListTag saved = savedSpots(site.level());
		CompoundTag savedSpot = saved.getCompoundOrEmpty(indexOf(site.level(), spot));
		// A structure this version doesn't Restore, as a save from another version might name.
		CompoundTag unknownStructure = withNewId(savedSpot);
		CompoundTag unknownState = withNewId(savedSpot);
		CompoundTag occupiedByNobody = withNewId(savedSpot);
		CompoundTag emptyWithAnOccupant = withNewId(savedSpot);
		List<CompoundTag> madeUp = List.of(unknownStructure, unknownState, occupiedByNobody, emptyWithAnOccupant);

		// The names a spot is saved under, which a world's saved spots depend on.
		helper.assertValueEqual(savedSpot.getStringOr("structure", ""), "woodland_mansion", "the saved name of a Woodland Mansion");
		helper.assertValueEqual(savedSpot.getStringOr("state", ""), "occupied", "the saved name of an occupied spot");

		unknownStructure.putString("structure", "sunken_library");
		unknownState.putString("state", "haunted");
		occupiedByNobody.remove("occupant");
		emptyWithAnOccupant.putString("state", "empty");
		madeUp.forEach(saved::add);
		loadSpots(site.level(), saved);

		List<RestoreSpot> loaded = RestoringAccess.spots(site.level());

		helper.assertValueEqual(Spots.current(site.level(), spot), spot, "the spot saved beside ones that can't be read");
		helper.assertValueEqual(loaded.size(), saved.size() - 3, "spots read from a save with three that can't be read");

		for (CompoundTag unreadable : List.of(unknownStructure, unknownState, occupiedByNobody)) {
			helper.assertFalse(loaded.stream().anyMatch(read -> read.id().equals(idOf(unreadable))), "a spot that can't be read is loaded");
		}

		RestoreSpot empty = loaded.stream().filter(read -> read.id().equals(idOf(emptyWithAnOccupant))).findFirst().orElseThrow();

		helper.assertValueEqual(empty.state(), RestoreSpot.State.EMPTY, "the spot saved empty with an occupant");
		helper.assertTrue(empty.occupant().isEmpty(), "an empty spot has no occupant, whatever was saved with it");

		// Back to the spots without the made-up ones.
		saved.removeAll(madeUp);
		loadSpots(site.level(), saved);
		site.release();
		helper.succeed();
	}

	// The lock kinds and Subspace kinds are saved by name too, and one a version doesn't know must not throw either.
	@GameTest
	public void anUnknownSavedLockOrSubspaceKindIsAnErrorNotAnException(GameTestHelper helper) {
		RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Subspace subspace = new Subspace(UUID.randomUUID(), UUID.randomUUID(), Subspace.Kind.WARDEN, Vec3.ZERO, 8.0);
		CompoundTag savedSubspace = (CompoundTag) Subspace.CODEC.encodeStart(ops, subspace).getOrThrow();
		CompoundTag savedLock = new CompoundTag();
		ListTag savedLocks = new ListTag();

		savedLock.putIntArray("book", new int[] {0, 0, 0});
		savedLock.putString("kind", "evokers");
		savedLock.put("keys", new ListTag());
		savedLock.put("done", new ListTag());
		savedLocks.add(savedLock);

		helper.assertValueEqual(savedSubspace.getStringOr("kind", ""), "warden", "the saved name of a Warden's Subspace");
		helper.assertValueEqual(Subspace.CODEC.parse(ops, savedSubspace).getOrThrow(), subspace, "a Subspace read back");
		helper.assertTrue(BookLocks.TYPE.codec().parse(ops, savedLocks).isSuccess(), "a lock saved as evokers is read");

		savedSubspace.putString("kind", "ender_dragon");
		savedLock.putString("kind", "ravagers");

		helper.assertTrue(Subspace.CODEC.parse(ops, savedSubspace).isError(), "a Subspace of an unknown kind is an error");
		helper.assertTrue(BookLocks.TYPE.codec().parse(ops, savedLocks).isError(), "a lock of an unknown kind is an error");
		helper.assertTrue(BookLocks.TYPE.codec().parse(ops, savedLocks).resultOrPartial(error -> {}).isPresent(), "the other locks are still read");
		helper.succeed();
	}

	// A copy of a saved spot that is a spot of its own.
	private static CompoundTag withNewId(CompoundTag savedSpot) {
		CompoundTag copy = savedSpot.copy();

		copy.putIntArray("id", UUIDUtil.uuidToIntArray(UUID.randomUUID()));

		return copy;
	}

	private static UUID idOf(CompoundTag savedSpot) {
		return UUIDUtil.uuidFromIntArray(savedSpot.getIntArray("id").orElseThrow());
	}

	// Every mob in the structure that it Restores holds an occupied spot of its own type. Returns how many there are.
	private static int assertMobsHoldSpots(GameTestHelper helper, ServerLevel level, StructureStart start, Restorable structure) {
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, AABB.of(start.getBoundingBox()), mob -> structure.restores(mob.getType()));

		helper.assertFalse(mobs.isEmpty(), "the structure placed mobs it Restores");

		for (Mob mob : mobs) {
			RestoreSpot spot = Spots.of(helper, mob);

			helper.assertValueEqual(spot.type(), mob.getType(), "a spot's mob");
			helper.assertValueEqual(spot.structure(), structure, "a spot's structure");
			helper.assertValueEqual(Spots.current(level, spot).occupant(), Optional.of(mob.getUUID()), "a spot's occupant");
			helper.assertTrue(structure.holds(level, spot.position()), "the block holds the spot at " + spot.position() + " as it generates");
		}

		return mobs.size();
	}

	// Fills the air at and below {@code pos} with water, down to the first block, as the ocean would.
	private static void flood(ServerLevel level, BlockPos pos) {
		for (BlockPos.MutableBlockPos at = pos.mutable(); level.getBlockState(at).isAir(); at.move(Direction.DOWN)) {
			level.setBlockAndUpdate(at, Blocks.WATER.defaultBlockState());
		}
	}

	// Builds a structure from pieces planned by hand, through the game's own chunk-by-chunk placement. Its usual
	// planning needs terrain, which the flat test world lacks.
	private static StructureStart placePieces(ServerLevel level, ResourceKey<Structure> key, List<? extends StructurePiece> pieces) {
		Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key).value();
		ChunkPos chunk = ChunkPos.containing(pieces.getFirst().getBoundingBox().getCenter());
		StructureStart start = new StructureStart(structure, chunk, 0, new PiecesContainer(new ArrayList<StructurePiece>(pieces)));

		Structures.chunks(start).forEach(each -> Structures.build(level, start, each));

		return start;
	}

	private static RestoreSpot.State stateOf(GameTestHelper helper, RestoreSpot spot) {
		return Spots.current(helper.getLevel(), spot).state();
	}

	private static void setOverworldClock(GameTestHelper helper, long ticks) {
		MinecraftServer server = helper.getLevel().getServer();
		var clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);

		server.clockManager().setTotalTicks(clock, ticks);
	}

	// The mob as its chunk would save it.
	private static ValueInput saved(ServerLevel level, Mob mob) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());

		mob.save(output);

		return TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult());
	}

	// The mob as it would load back from its saved chunk.
	private static Entity reloaded(ServerLevel level, Mob mob) {
		return EntityType.create(saved(level, mob), level, new EntitySpawnRequest(EntitySpawnReason.LOAD, false)).orElseThrow();
	}

	// Saves the level's spots and loads them back in their place, as a restart would.
	private static void reloadSpots(ServerLevel level) {
		loadSpots(level, savedSpots(level));
	}

	// The level's spots as it would save them now.
	private static ListTag savedSpots(ServerLevel level) {
		return RestoringAccess.saved(level);
	}

	// Loads saved spots in place of the level's, as a restart would.
	private static void loadSpots(ServerLevel level, ListTag saved) {
		RestoringAccess.load(level, saved);
	}

	// Where the spot is among the level's spots, saved or not.
	private static int indexOf(ServerLevel level, RestoreSpot spot) {
		return RestoringAccess.spots(level).stream().map(RestoreSpot::id).toList().indexOf(spot.id());
	}

	/**
	 * A spot's 5×5 floor, walled in so a Restored mob can't wander off and fall, high over one of the test's
	 * {@linkplain TestSites sites}, in a chunk the test keeps loaded.
	 *
	 * @param floor the block under the spot
	 */
	private record Site(GameTestHelper helper, ServerLevel level, BlockPos floor) {
		static Site build(GameTestHelper helper, int index, Block floorBlock) {
			Site site = new Site(helper, helper.getLevel(), TestSites.of(helper).chunk(index).getMiddleBlockPosition(FLOOR_Y));

			// Until the chunk ticks entities, the mobs a test places would sit untracked: the cow that should block a spot
			// would go unseen, and a converted Brute would leave without emptying its spot.
			site.keepLoaded();

			for (BlockPos pos : BlockPos.betweenClosed(site.floor.offset(-3, 0, -3), site.floor.offset(3, 3, 3))) {
				boolean wall = Math.abs(pos.getX() - site.floor.getX()) == 3 || Math.abs(pos.getZ() - site.floor.getZ()) == 3;
				Block block = pos.getY() == site.floor.getY() ? floorBlock : wall ? Blocks.GLASS : Blocks.AIR;

				site.level.setBlockAndUpdate(pos, block.defaultBlockState());
			}

			return site;
		}

		/** Where a mob on the spot stands. */
		Vec3 feet() {
			return Vec3.atBottomCenterOf(floor.above());
		}

		/** Places a mob on the spot as its structure would while generating, so it gets the spot. */
		Mob placeOriginal(Restorable structure, EntityType<? extends Mob> type) {
			Mob mob = type.create(level, EntitySpawnReason.STRUCTURE);

			mob.snapTo(feet().x, feet().y, feet().z, 0.0F, 0.0F);
			mob.setPersistenceRequired();
			Restoring.build(Optional.of(structure), () -> level.addFreshEntity(mob));

			return mob;
		}

		/** Puts the player in the air {@code blocks} east of the spot. */
		void putPlayer(ServerPlayer player, int blocks) {
			player.teleportTo(feet().x + blocks, feet().y + 5, feet().z);
		}

		/** The living mobs inside the site's walls. */
		List<Mob> mobs() {
			return level.getEntitiesOfClass(Mob.class, new AABB(floor).inflate(3, 4, 3), Mob::isAlive);
		}

		void assertMobs(GameTestHelper helper, int count, String what) {
			helper.assertValueEqual(mobs().size(), count, what);
		}

		/** Fails until the site's chunk has loaded its entities, as spots only fill once it has. */
		void assertReady(GameTestHelper helper) {
			helper.assertTrue(level.areEntitiesLoaded(ChunkPos.pack(floor)), "the site's entities are loaded");
		}

		void keepLoaded() {
			ForcedChunks.keepLoaded(helper, level, List.of(ChunkPos.containing(floor)));
		}

		boolean isLoaded() {
			return level.hasChunk(floor.getX() >> 4, floor.getZ() >> 4);
		}

		/** Removes the site's mobs and lets its chunk unload. */
		void release() {
			mobs().forEach(Entity::discard);
			letUnload();
		}

		/** Lets the site's chunk unload with its mobs in it. */
		void letUnload() {
			ForcedChunks.letUnload(helper, level, List.of(ChunkPos.containing(floor)));
		}
	}
}

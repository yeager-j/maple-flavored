package mapleflavored.gametest;

import java.util.List;
import java.util.Optional;
import mapleflavored.mixin.WardenSpawnTrackerAccessor;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.wardensummoning.WardenSummoning;
import mapleflavored.wardensummoning.WardenTerritory;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Warden summoning: a shriek counts only for players in Warden Territory, the Deep Dark or an Ancient City's bounds,
 * and its Warden spawns near one of them, widening the search when there's no room and clearing the cooldown when
 * there's none at all.
 *
 * <p>A summoned Warden opens a Subspace, so each test runs alone in its batch, in a {@link SubspaceArena}. Its players
 * stand on floors laid for them, so a Warden can spawn only where a test lays one.
 */
public class WardenSummoningGameTests {
	// How long a shriek lasts before its Warden is summoned, with a little room.
	private static final int SHRIEK_TICKS = 100;
	// Far above the arena's own floor and what earlier batches left on it, out of reach of the search's vertical scans.
	private static final int ABOVE_THE_ARENA = 30;
	// Vanilla's cooldown after a warning.
	private static final int COOLDOWN_TICKS = 200;

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_territory", maxTicks = 20)
	public void wardenTerritoryIsTheDeepDarkAndAncientCityBounds(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerLevel level = arena.level();
		StructureStart city = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, TestSites.of(helper).chunk(0));
		BoundingBox bounds = city.getBoundingBox();
		BlockPos inAPiece = city.getPieces().getFirst().getBoundingBox().getCenter();
		BlockPos betweenPieces = cornerOutsideEveryPiece(level, city);
		BlockPos pastTheBounds = new BlockPos(bounds.maxX() + 1, bounds.minY(), bounds.minZ());

		Structures.register(level, city);
		arena.deepDark(Vec3.ZERO);

		helper.assertTrue(WardenTerritory.contains(level, BlockPos.containing(arena.centre())), "The Deep Dark isn't Warden Territory");
		helper.assertFalse(WardenTerritory.contains(level, arena.blockAt(20, 0, 0)), "The arena's plains are Warden Territory");
		helper.assertTrue(WardenTerritory.contains(level, inAPiece), "An Ancient City's piece isn't Warden Territory");
		helper.assertTrue(WardenTerritory.contains(level, betweenPieces), "An Ancient City's bounds outside its pieces aren't Warden Territory");
		helper.assertFalse(WardenTerritory.contains(level, pastTheBounds), "Past an Ancient City's bounds is Warden Territory");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_remote_outside", maxTicks = 200)
	public void aShriekCausedFromOutsideWardenTerritoryWarnsNobody(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer cause = arena.player(Vec3.ZERO, GameType.SURVIVAL);
		Vec3 shriekerOffset = new Vec3(30.0, 0.0, 0.0);
		SculkShriekerBlockEntity shrieker = shrieker(arena, shriekerOffset);

		arena.deepDark(shriekerOffset);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);

		helper.assertFalse(shrieker.getBlockState().getValue(SculkShriekerBlock.SHRIEKING), "The shrieker shrieked");
		assertWarning(helper, cause, 3, false, "the cause");

		helper.startSequence()
			.thenExecuteAfter(SHRIEK_TICKS, () -> helper.assertValueEqual(wardens(arena), List.of(), "the Wardens around the arena"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_remote_inside", maxTicks = 300)
	public void aRemoteShriekSummonsNearTheCauseInWardenTerritory(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer cause = arena.player(Vec3.ZERO, GameType.SURVIVAL);
		Vec3 shriekerOffset = new Vec3(40.0, 0.0, 0.0);
		SculkShriekerBlockEntity shrieker = shrieker(arena, shriekerOffset);

		arena.deepDark(Vec3.ZERO);
		arena.floorArea(-15, 15, -15, 15, 0);
		arena.floorArea(25, 55, -15, 15, 0);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);

		helper.assertTrue(shrieker.getBlockState().getValue(SculkShriekerBlock.SHRIEKING), "The shrieker didn't shriek");
		assertWarning(helper, cause, 4, true, "the cause");

		helper.startSequence()
			.thenWaitUntil(() -> assertSummonedNear(helper, arena, cause, 5.0, 15.0))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_friend", maxTicks = 300)
	public void aFriendsShriekSummonsNearThePlayerInWardenTerritory(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Vec3 friendOffset = new Vec3(10.0, 0.0, 0.0);
		ServerPlayer cause = arena.player(new Vec3(-5.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer friend = arena.player(friendOffset, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, Vec3.ZERO);

		arena.deepDark(friendOffset);
		arena.floorArea(-5, 25, -15, 15, 0);
		friend.getWardenSpawnTracker().setWarningLevel(3);
		// A cooldown left over from before the cause left Warden Territory, which no longer counts.
		((WardenSpawnTrackerAccessor) cause.getWardenSpawnTracker()).maple$setCooldownTicks(COOLDOWN_TICKS);
		shrieker.tryShriek(arena.level(), cause);

		assertWarning(helper, friend, 4, true, "the friend in Warden Territory");
		assertWarning(helper, cause, 0, true, "the cause outside it");

		helper.startSequence()
			.thenWaitUntil(() -> assertSummonedNear(helper, arena, friend, 5.0, 15.0))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_widening", maxTicks = 300)
	public void withNoRoomNearbyTheSearchWidensTo50Blocks(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Vec3 standing = new Vec3(0.0, ABOVE_THE_ARENA, 0.0);
		ServerPlayer cause = arena.player(standing, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, standing.add(2.0, 0.0, 0.0));

		arena.deepDark(standing);
		// Nothing to stand on within 15 blocks, and plenty from 20 to 50.
		arena.floorArea(20, 50, -50, 50, ABOVE_THE_ARENA);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);

		helper.startSequence()
			.thenWaitUntil(() -> assertSummonedNear(helper, arena, cause, 20.0, 50.0))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_failing", maxTicks = 200)
	public void withNoRoomWithin50BlocksTheCooldownClears(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Vec3 standing = new Vec3(0.0, ABOVE_THE_ARENA * 2, 0.0);
		// Under 5 blocks from the cause, so the friend's own floor is too close for the Warden.
		Vec3 friendStanding = standing.add(4.0, 0.0, 0.0);
		ServerPlayer cause = arena.player(standing, GameType.SURVIVAL);
		ServerPlayer friend = arena.player(friendStanding, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, standing.add(2.0, 0.0, 0.0));

		arena.deepDark(standing);
		arena.deepDark(friendStanding);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);
		assertWarning(helper, cause, 4, true, "the cause while the shriek lasts");
		assertWarning(helper, friend, 4, true, "the friend while the shriek lasts");

		helper.startSequence()
			.thenExecuteAfter(SHRIEK_TICKS, () -> {
				helper.assertValueEqual(wardens(arena), List.of(), "the Wardens around the arena");
				assertWarning(helper, cause, 4, false, "the cause once no spot had room");
				assertWarning(helper, friend, 4, false, "the friend once no spot had room");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_cancelled", maxTicks = 200)
	public void withNobodyInWardenTerritoryTheSummonIsCancelled(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer cause = arena.player(Vec3.ZERO, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, new Vec3(2.0, 0.0, 0.0));

		arena.deepDark(Vec3.ZERO);
		arena.floorArea(-15, 45, -15, 15, 0);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);
		assertWarning(helper, cause, 4, true, "the cause while the shriek lasts");
		arena.moveTo(cause, new Vec3(30.0, 0.0, 0.0));

		helper.startSequence()
			.thenExecuteAfter(SHRIEK_TICKS, () -> {
				helper.assertValueEqual(wardens(arena), List.of(), "the Wardens around the arena");
				assertWarning(helper, cause, 4, true, "the cause, whose summon was cancelled rather than failed");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_nearest", maxTicks = 300)
	public void withTheCauseGoneTheWardenComesForTheNearestPlayerInWardenTerritory(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		Vec3 causeOffset = new Vec3(2.0, 0.0, 0.0);
		Vec3 nearerOffset = new Vec3(20.0, 0.0, 0.0);
		Vec3 fartherOffset = new Vec3(-30.0, 0.0, 0.0);
		ServerPlayer cause = arena.player(causeOffset, GameType.SURVIVAL);
		// Both too far from the shrieker for it to count for them.
		ServerPlayer nearer = arena.player(nearerOffset, GameType.SURVIVAL);
		arena.player(fartherOffset, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, Vec3.ZERO);

		for (Vec3 offset : List.of(causeOffset, nearerOffset, fartherOffset)) {
			arena.deepDark(offset);
		}

		arena.floorArea(5, 35, -15, 15, 0);
		arena.floorArea(-45, -15, -15, 15, 0);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(arena.level(), cause);
		assertWarning(helper, nearer, 0, false, "a player too far from the shrieker");
		arena.moveTo(cause, new Vec3(0.0, 0.0, 12.0));

		helper.startSequence()
			.thenWaitUntil(() -> assertSummonedNear(helper, arena, nearer, 5.0, 15.0))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_below_max", maxTicks = 200)
	public void aShriekBelowTheFourthWarningSummonsNothing(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer cause = arena.player(Vec3.ZERO, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, new Vec3(2.0, 0.0, 0.0));

		arena.deepDark(Vec3.ZERO);
		arena.floorArea(-15, 15, -15, 15, 0);
		shrieker.tryShriek(arena.level(), cause);
		assertWarning(helper, cause, 1, true, "the cause");

		helper.startSequence()
			.thenExecuteAfter(SHRIEK_TICKS, () -> helper.assertValueEqual(wardens(arena), List.of(), "the Wardens around the arena"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_search_alone", maxTicks = 20)
	public void theSearchAloneIgnoresWarningLevelsAndNearbyWardens(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer player = arena.player(Vec3.ZERO, GameType.SURVIVAL);

		arena.stillWarden(new Vec3(2.0, 0.0, 0.0));
		arena.floorArea(-15, 15, -15, 15, 0);

		Optional<Warden> spawned = WardenSummoning.spawnNear(arena.level(), player);

		helper.assertTrue(spawned.isPresent(), "No Warden appeared");

		Warden warden = arena.track(spawned.get());
		double distance = horizontalDistance(warden, player);
		boolean ownsSubspace = Subspaces.containing(arena.level(), warden.position()).stream()
			.anyMatch(subspace -> subspace.boss().equals(warden.getUUID()));

		helper.assertTrue(distance >= 5.0 && distance <= 15.0, "The Warden appeared " + distance + " blocks across from the player");
		helper.assertTrue(ownsSubspace, "The Warden opened no Subspace");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:warden_summoning_saved", maxTicks = 300)
	public void aShrieksPlayersOutlastTheShriekerBeingSavedAndLoaded(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerLevel level = arena.level();
		ServerPlayer cause = arena.player(Vec3.ZERO, GameType.SURVIVAL);
		SculkShriekerBlockEntity shrieker = shrieker(arena, new Vec3(30.0, 0.0, 0.0));
		BlockPos position = shrieker.getBlockPos();

		arena.deepDark(Vec3.ZERO);
		arena.floorArea(-15, 15, -15, 15, 0);
		cause.getWardenSpawnTracker().setWarningLevel(3);
		shrieker.tryShriek(level, cause);

		// As its chunk unloading and loading again would: a new block entity from the saved one, with the shriek's tick
		// still scheduled on the block.
		CompoundTag saved = shrieker.saveWithFullMetadata(level.registryAccess());

		level.removeBlockEntity(position);
		level.setBlockEntity(BlockEntity.loadStatic(position, level.getBlockState(position), saved, level.registryAccess()));
		helper.assertFalse(level.getBlockEntity(position) == shrieker, "The shrieker wasn't replaced");

		helper.startSequence()
			.thenWaitUntil(() -> assertSummonedNear(helper, arena, cause, 5.0, 15.0))
			.thenSucceed();
	}

	// A shrieker that can summon, as an Ancient City's do.
	private static SculkShriekerBlockEntity shrieker(SubspaceArena arena, Vec3 offset) {
		BlockPos position = BlockPos.containing(arena.at(offset));
		ServerLevel level = arena.level();

		level.setBlock(position, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);

		return (SculkShriekerBlockEntity) level.getBlockEntity(position);
	}

	private static List<Warden> wardens(SubspaceArena arena) {
		return arena.level().getEntitiesOfClass(Warden.class, AABB.ofSize(arena.centre(), 200.0, 200.0, 200.0));
	}

	private static void assertSummonedNear(GameTestHelper helper, SubspaceArena arena, ServerPlayer player, double min, double max) {
		List<Warden> summoned = wardens(arena);

		helper.assertValueEqual(summoned.size(), 1, "the Wardens summoned");

		Warden warden = arena.track(summoned.getFirst());
		double distance = horizontalDistance(warden, player);

		helper.assertTrue(
			distance >= min && distance <= max,
			"The Warden appeared " + distance + " blocks across from the player, not " + min + " to " + max
		);
	}

	private static double horizontalDistance(Entity from, Entity to) {
		return Math.hypot(from.getX() - to.getX(), from.getZ() - to.getZ());
	}

	private static void assertWarning(GameTestHelper helper, ServerPlayer player, int level, boolean onCooldown, String who) {
		WardenSpawnTracker tracker = player.getWardenSpawnTracker();

		helper.assertValueEqual(tracker.getWarningLevel(), level, "the warning level of " + who);
		helper.assertValueEqual(((WardenSpawnTrackerAccessor) tracker).maple$onCooldown(), onCooldown, "whether " + who + " is on cooldown");
	}

	// A corner of the city's bounds that none of its pieces covers.
	private static BlockPos cornerOutsideEveryPiece(ServerLevel level, StructureStart city) {
		BoundingBox bounds = city.getBoundingBox();
		List<BlockPos> corners = List.of(
			new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
			new BlockPos(bounds.maxX(), bounds.minY(), bounds.minZ()),
			new BlockPos(bounds.minX(), bounds.minY(), bounds.maxZ()),
			new BlockPos(bounds.maxX(), bounds.minY(), bounds.maxZ()),
			new BlockPos(bounds.minX(), bounds.maxY(), bounds.minZ()),
			new BlockPos(bounds.maxX(), bounds.maxY(), bounds.minZ()),
			new BlockPos(bounds.minX(), bounds.maxY(), bounds.maxZ()),
			new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ())
		);
		Optional<BlockPos> corner = corners.stream()
			.filter(position -> city.getPieces().stream().noneMatch(piece -> piece.getBoundingBox().isInside(position)))
			.findFirst();

		return corner.orElseThrow(() -> new IllegalStateException("every corner of the Ancient City is in a piece"));
	}
}

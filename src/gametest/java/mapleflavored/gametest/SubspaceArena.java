package mapleflavored.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.gametest.mixin.GameTestHelperAccessor;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One Subspace test's arena, 160 blocks above its test structure: far past a Subspace's reach, so it never touches the
 * test grid or the structures earlier batches left on it. Wardens, players and floors are placed relative
 * to the arena's centre.
 *
 * <p>Its floors are smooth stone, which sculk can't grow over, so a Warden's Subspace spreads no sculk unless a test
 * lays a {@linkplain #sculkFloor sculk floor}. Whether the test passes or fails, the arena discards its Wardens, logs out
 * its players, removes its sculk floors and ends every Subspace in the level, so nothing it opened reaches a later batch.
 *
 * <p>A mock player's connection never ticks it, so the arena ticks its own players, letting their effects run out as a
 * client's would.
 */
final class SubspaceArena {
	/** A Warden's Subspace radius, as the game rule sets it by default. */
	static final double RADIUS = MapleGameRules.WARDEN_SUBSPACE_RADIUS.defaultValue();

	/** The arena's floors: a solid block sculk can't grow over. */
	static final Block FLOOR = Blocks.SMOOTH_STONE;

	private static final int HEIGHT = 160;
	// Past the largest Subspace's reach, so nothing an earlier batch left is aware of the arena or affected by it.
	private static final double CLEARED_RADIUS = Math.max(RADIUS, WitherSubspaces.RADIUS) + 10.0;
	// How far along the arena's x axis a runway keeps chunks loaded, past every Warden and target the tests place.
	private static final int RUNWAY_REACH = 80;
	// Room above a sculk floor for the veins and growths that spread over it.
	private static final int GROWTH_HEIGHT = 2;

	// Mob.finalizeSpawn's modifier that scales a mob's follow range by up to 11.5% either way.
	private static final Identifier RANDOM_SPAWN_BONUS = Identifier.withDefaultNamespace("random_spawn_bonus");

	private static final BlockPos RELATIVE_CENTRE = new BlockPos(1, HEIGHT, 1);
	private static final Set<UUID> TICKED_PLAYERS = ConcurrentHashMap.newKeySet();

	static {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
				if (TICKED_PLAYERS.contains(player.getUUID())) player.doTick();
			}
		});
	}

	private final GameTestHelper helper;
	private final ServerLevel level;
	private final BlockPos centreBlock;
	private final List<Entity> mobs = new ArrayList<>();
	private final Set<UUID> players = ConcurrentHashMap.newKeySet();
	private final Set<BoundingBox> sculkFloors = new LinkedHashSet<>();
	private final Set<BoundingBox> floorAreas = new LinkedHashSet<>();
	private final Map<BlockPos, Holder<Biome>> replacedBiomes = new LinkedHashMap<>();

	private SubspaceArena(GameTestHelper helper) {
		this.helper = helper;
		this.level = helper.getLevel();
		this.centreBlock = helper.absolutePos(RELATIVE_CENTRE);

		// Every arena stands its Wardens on a floor.
		level.setBlock(centreBlock.below(), FLOOR.defaultBlockState(), 3);
		cleanUpWhenDone();
	}

	/**
	 * An arena for a test running alone in its batch, cleared of the players, mobs and Subspaces earlier batches left
	 * behind, with its chunks kept loaded along a runway on its x axis.
	 */
	static SubspaceArena cleared(GameTestHelper helper) {
		SubspaceArena arena = clearedWithoutRunway(helper);

		arena.keepLoaded(-RUNWAY_REACH, RUNWAY_REACH, -1, 1);

		return arena;
	}

	/**
	 * An arena like {@link #cleared}, but keeping only the chunks its Wardens and sculk floors need loaded, so a test can
	 * let other chunks unload.
	 */
	static SubspaceArena clearedWithoutRunway(GameTestHelper helper) {
		SubspaceArena arena = new SubspaceArena(helper);
		PlayerList playerList = helper.getLevel().getServer().getPlayerList();

		List.copyOf(playerList.getPlayers()).forEach(playerList::remove);
		// Not only mobs: the experience orbs and items a killed Warden drops would land by the next arena's Warden, and a
		// Warden hears them land.
		arena.level.getEntities(
			(Entity) null,
			AABB.ofSize(arena.centre(), CLEARED_RADIUS * 2.0, CLEARED_RADIUS * 2.0, CLEARED_RADIUS * 2.0),
			entity -> !(entity instanceof ServerPlayer)
		).forEach(Entity::discard);
		arena.level.setAttached(Subspaces.ATTACHMENT, List.of());

		return arena;
	}

	/** An arena for a test sharing its batch with others, so it leaves their players and mobs alone. */
	static SubspaceArena shared(GameTestHelper helper) {
		SubspaceArena arena = new SubspaceArena(helper);

		arena.keepLoaded(-RUNWAY_REACH, RUNWAY_REACH, -1, 1);

		return arena;
	}

	/** Saves the entity the way its chunk would on unloading, and removes it as unloading does. */
	static CompoundTag unload(GameTestHelper helper, Entity entity) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());

		entity.save(output);
		entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

		return output.buildResult();
	}

	/** Adds back an entity {@link #unload unloaded} earlier, as its chunk loading again would. */
	static Entity load(GameTestHelper helper, CompoundTag saved) {
		var input = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved);
		Entity loaded = EntityType.loadEntityRecursive(input, helper.getLevel(), EntitySpawnReason.LOAD, entity -> entity);

		helper.assertTrue(loaded != null, "the saved entity loads");
		helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "the saved entity is added back");

		return loaded;
	}

	/**
	 * Moves the player as the server does, so the chunks around them load. A player who has moved is invulnerable until
	 * their client's first movement arrives, which a mock never sends, so that wait is cleared.
	 */
	static void teleport(ServerPlayer player, Vec3 position) {
		player.teleport(new TeleportTransition(player.level(), position, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		player.hasChangedDimension();
	}

	ServerLevel level() {
		return level;
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

	/**
	 * A Warden that can dig back down, standing at the offset from the centre. Its follow range is vanilla's base 24
	 * blocks, without the random bonus a spawn adds, so vanilla's awareness box is the same size in every run.
	 */
	Warden warden(Vec3 offset) {
		Warden warden = helper.spawnEntity(EntityTypes.WARDEN, relativeAt(offset))
			.requirePersistence(false)
			.spawnReason(EntitySpawnReason.MOB_SUMMONED)
			.spawn();

		warden.getAttribute(Attributes.FOLLOW_RANGE).removeModifier(RANDOM_SPAWN_BONUS);

		return track(warden);
	}

	/**
	 * A Warden that stays where it's put, even in midair, and never digs down, so its Subspace lasts until the test ends
	 * it.
	 */
	Warden stillWarden(Vec3 offset) {
		BlockPos fromCentre = BlockPos.containing(at(offset)).subtract(centreBlock);

		keepLoaded(fromCentre.getX(), fromCentre.getX(), fromCentre.getZ(), fromCentre.getZ());

		Warden warden = track(helper.spawn(EntityTypes.WARDEN, relativeAt(offset)));

		warden.setNoAi(true);

		return warden;
	}

	/** A Wither that stays where it's put, even in midair, and never attacks, so its Subspace lasts until the test ends it. */
	WitherBoss stillWither(Vec3 offset) {
		BlockPos fromCentre = BlockPos.containing(at(offset)).subtract(centreBlock);

		keepLoaded(fromCentre.getX(), fromCentre.getX(), fromCentre.getZ(), fromCentre.getZ());

		WitherBoss wither = track(helper.spawn(EntityTypes.WITHER, relativeAt(offset)));

		wither.setNoAi(true);

		return wither;
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
	 * A loaded player standing at the offset from the centre, ticked each tick as a client would tick it. Ticking moves it
	 * as a client would, so it falls unless it stands on something.
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

		level.setBlock(BlockPos.containing(feet).below(), FLOOR.defaultBlockState(), 3);
		teleport(player, feet);
	}

	/** Lays a three-block-wide floor along the arena's x axis, from {@code fromX} to {@code toX} blocks out. */
	void floor(int fromX, int toX) {
		for (int x = fromX; x <= toX; x++) {
			for (int z = -1; z <= 1; z++) {
				level.setBlock(blockAt(x, -1, z), FLOOR.defaultBlockState(), 3);
			}
		}
	}

	/**
	 * Lays a floor over the box from the offsets {@code fromX, fromZ} to {@code toX, toZ}, {@code y} blocks above the
	 * centre, and keeps its chunks loaded. Teardown removes it.
	 */
	void floorArea(int fromX, int toX, int fromZ, int toZ, int y) {
		BoundingBox area = BoundingBox.fromCorners(blockAt(fromX, y - 1, fromZ), blockAt(toX, y - 1, toZ));

		floorAreas.add(area);
		keepLoaded(fromX, toX, fromZ, toZ);
		BlockPos.betweenClosedStream(area).forEach(position -> level.setBlock(position, FLOOR.defaultBlockState(), 3));
	}

	/**
	 * Turns the biome around the offset from the centre, at least 4 blocks each way, to the Deep Dark. Teardown puts back
	 * the biome that was there.
	 */
	void deepDark(Vec3 offset) {
		BlockPos middle = BlockPos.containing(at(offset));
		BoundingBox box = BoundingBox.fromCorners(middle.offset(-4, -4, -4), middle.offset(4, 4, 4));
		Holder<Biome> deepDark = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_DARK);

		// Each cell keeps the biome it had before this arena first changed it, so overlapping boxes still put back what was
		// there.
		BlockPos.betweenClosedStream(box)
			.map(SubspaceArena::biomeCell)
			.distinct()
			.forEach(cell -> replacedBiomes.putIfAbsent(cell, level.getBiome(cell)));
		fillBiome(box, deepDark).ifRight(error -> {
			throw new IllegalStateException("couldn't fill the biome: " + error.getMessage());
		});
	}

	// Biomes are stored per 4-block cell, named here by its lowest corner.
	private static BlockPos biomeCell(BlockPos position) {
		return new BlockPos(position.getX() & ~3, position.getY() & ~3, position.getZ() & ~3);
	}

	private Either<Integer, CommandSyntaxException> fillBiome(BoundingBox box, Holder<Biome> biome) {
		BlockPos from = new BlockPos(box.minX(), box.minY(), box.minZ());
		BlockPos to = new BlockPos(box.maxX(), box.maxY(), box.maxZ());

		return FillBiomeCommand.fill(level, from, to, biome);
	}

	private void restoreBiome(BlockPos cell, Holder<Biome> biome) {
		fillBiome(BoundingBox.fromCorners(cell, cell.offset(3, 3, 3)), biome);
	}

	/** Walls in the centre so a Warden there can't walk away. */
	void pen() {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (x == 0 && z == 0) continue;

				for (int y = 0; y < 3; y++) {
					level.setBlock(blockAt(x, y, z), FLOOR.defaultBlockState(), 3);
				}
			}
		}
	}

	/**
	 * Lays stone, which sculk grows over, under the box from {@code from} to {@code to} on the arena's floor level, with
	 * nothing above it, and keeps its chunks loaded. Teardown {@linkplain #clear clears} it.
	 */
	BoundingBox sculkFloor(int fromX, int fromZ, int toX, int toZ) {
		BoundingBox volume = BoundingBox.fromCorners(blockAt(fromX, -1, fromZ), blockAt(toX, GROWTH_HEIGHT, toZ));

		sculkFloors.add(volume);
		keepLoaded(fromX, toX, fromZ, toZ);
		renew(volume);

		return volume;
	}

	/** Removes a {@linkplain #sculkFloor sculk floor} with everything that grew on it. */
	void clear(BoundingBox sculkFloor) {
		BlockPos.betweenClosedStream(sculkFloor).forEach(position -> level.setBlock(position, Blocks.AIR.defaultBlockState(), 3));
	}

	/** Clears a {@linkplain #sculkFloor sculk floor} and lays it again as fresh stone. */
	void renew(BoundingBox sculkFloor) {
		clear(sculkFloor);

		for (int x = sculkFloor.minX(); x <= sculkFloor.maxX(); x++) {
			for (int z = sculkFloor.minZ(); z <= sculkFloor.maxZ(); z++) {
				level.setBlock(new BlockPos(x, sculkFloor.minY(), z), Blocks.STONE.defaultBlockState(), 3);
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

	/** Keeps the chunks under the box from the offsets {@code fromX, fromZ} to {@code toX, toZ} loaded. */
	void keepLoaded(int fromX, int toX, int fromZ, int toZ) {
		int fromChunkX = SectionPos.blockToSectionCoord(centreBlock.getX() + fromX);
		int toChunkX = SectionPos.blockToSectionCoord(centreBlock.getX() + toX);
		int fromChunkZ = SectionPos.blockToSectionCoord(centreBlock.getZ() + fromZ);
		int toChunkZ = SectionPos.blockToSectionCoord(centreBlock.getZ() + toZ);

		// Only the test's own chunks are kept loaded for it. An entity anywhere else wouldn't tick, and neither a
		// Warden's sensor nor its Subspace could find it. The runner releases every forced chunk as each batch ends, just
		// before it forces the next batch's, so the arena never releases them itself: that would unload the next test's
		// chunks.
		for (int x = fromChunkX; x <= toChunkX; x++) {
			for (int z = fromChunkZ; z <= toChunkZ; z++) {
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

		sculkFloors.forEach(this::clear);
		floorAreas.forEach(this::clear);
		// Best effort: a chunk unloaded by now keeps the Deep Dark, which only a shriek there would notice.
		replacedBiomes.forEach(this::restoreBiome);

		level.setAttached(Subspaces.ATTACHMENT, List.of());
	}
}

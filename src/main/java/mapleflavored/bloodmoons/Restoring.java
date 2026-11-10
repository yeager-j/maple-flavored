package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import mapleflavored.MapleFlavored;
import mapleflavored.mixin.ServerLevelEntityManagerAccessor;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

/**
 * Blood Moon Restoring: structures' original mobs return to their original spots after a Blood Moon.
 *
 * <p>As a {@link Restorable} structure generates, each mob it places that it Restores carries a {@link RestoreSpot},
 * which is recorded once the mob is in the world. The spot empties once that mob is gone for good: killed, converted,
 * despawned, or dropped as its save loads, as Peaceful drops hostile mobs. A mob that only wanders off, unloads or
 * changes dimension still holds it. Each Blood Moon makes every empty spot pending, without loading anything, and a
 * pending spot fills with a new mob of its type when its chunk loads, or on a retry while it's loaded, once the spot is
 * clear, its structure's block is still there and no player is near. Restoring never places a block.
 *
 * <p>The mob decides whether its spot is held. The level saves its spots at other times than a chunk saves its mobs, so
 * a crash can leave the two disagreeing, and then the spots follow the mobs: a living mob that loads holds the spot it
 * carries, and a spot whose mob isn't in the chunk it was saved in is empty.
 */
public final class Restoring {
	/** How often pending spots in loaded chunks retry, in ticks. */
	public static final int RETRY_INTERVAL = 100;

	/** How near a player, in blocks, keeps a spot from filling. */
	public static final double PLAYER_CLEARANCE = 32.0;

	// The key a mob's spot is saved under, among its attachments.
	private static final String SPOT_KEY = MapleFlavored.id("restore_spot").toString();

	private static final AttachmentType<SpotTag> SPOT = AttachmentRegistry.create(
		MapleFlavored.id("restore_spot"),
		builder -> builder.persistent(SpotTag.CODEC)
	);

	// Set on a worldgen thread while a Restorable structure builds a chunk, for the mobs it places to pick up.
	private static final ThreadLocal<@Nullable Restorable> BUILDING = new ThreadLocal<>();

	// Saved mobs holding spots that failed to load, as Peaceful refuses a hostile mob. Their spots empty on the next tick.
	private static final Queue<Dropped> DROPPED = new ConcurrentLinkedQueue<>();

	// The chunks each level loaded whose pending spots haven't been tried yet. Server thread only.
	private static final Map<ResourceKey<Level>, LongSet> LOADED_CHUNKS = new HashMap<>();

	private Restoring() {
	}

	static void register() {
		// A world opened after another in the same game must never receive the first world's spots.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			DROPPED.clear();
			LOADED_CHUNKS.clear();
		});
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> recordOccupantChunks(server));
		ServerTickEvents.END_SERVER_TICK.register(Restoring::tick);
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> loadedChunksOf(level).add(chunk.getPos().pack()));
		// A dying mob empties its spot the moment it dies; it's only removed once its death animation ends.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) empty(level.getServer(), entity);
		});
		ServerEntityEvents.ENTITY_LOAD.register(Restoring::hold);
		ServerEntityEvents.ENTITY_UNLOAD.register(Restoring::emptyOrRecordChunk);
		BloodMoons.RISEN.register(Restoring::makeEmptySpotsPending);
	}

	/**
	 * Builds one chunk of a structure, tagging each mob it places that the structure Restores with a spot. Call around
	 * every structure's chunk build.
	 */
	public static void build(Optional<Restorable> structure, Runnable buildChunk) {
		if (structure.isEmpty()) {
			buildChunk.run();

			return;
		}

		BUILDING.set(structure.get());

		try {
			buildChunk.run();
		} finally {
			BUILDING.remove();
		}
	}

	/**
	 * Tags a mob a structure is placing with its spot, if the structure Restores it. The spot is recorded once the mob is
	 * in the world, so a mob the level refuses leaves none. Call for every entity added to a level or a worldgen region.
	 * Safe to call from any thread.
	 *
	 * @param dimension the level the mob is being placed in
	 */
	public static void tagIfPlacing(Entity entity, ResourceKey<Level> dimension) {
		Restorable structure = BUILDING.get();

		if (structure == null || !(entity instanceof Mob mob) || !structure.restores(mob.getType())) return;

		RestoreSpot spot = RestoreSpot.occupiedBy(mob.getUUID(), mob.position(), structure, mob.getType(), mob.isPersistenceRequired());

		mob.setAttached(SPOT, new SpotTag(dimension, spot.id(), Optional.of(spot)));
	}

	/**
	 * Empties the spot of a saved mob that failed to load, as Peaceful refuses to load a hostile mob: it's gone for good.
	 * Call for every saved entity that fails to load. Safe to call from any thread.
	 */
	public static void dropIfOccupant(ValueInput saved) {
		Optional<SpotTag> tag = saved.child(AttachmentTarget.NBT_ATTACHMENT_KEY)
			.flatMap(attachments -> attachments.read(SPOT_KEY, SpotTag.CODEC));
		Optional<UUID> mob = saved.read("UUID", UUIDUtil.CODEC);

		if (tag.isPresent() && mob.isPresent()) DROPPED.add(new Dropped(tag.get(), mob.get()));
	}

	/** The spot the entity holds, if it holds one. */
	public static Optional<RestoreSpot> spotOf(MinecraftServer server, Entity entity) {
		SpotTag tag = entity.getAttached(SPOT);

		if (tag == null) return Optional.empty();

		ServerLevel level = server.getLevel(tag.dimension());

		return level == null ? Optional.empty() : RestoreSpots.of(level).get(tag.spot());
	}

	private static void tick(MinecraftServer server) {
		for (Dropped dropped = DROPPED.poll(); dropped != null; dropped = DROPPED.poll()) {
			empty(server, dropped.spot(), dropped.mob());
		}

		boolean retrying = server.getTickCount() % RETRY_INTERVAL == 0;

		for (ServerLevel level : server.getAllLevels()) {
			RestoreSpots spots = level.getDataStorage().get(RestoreSpots.TYPE);

			if (spots == null) {
				loadedChunksOf(level).clear();

				continue;
			}

			tryLoadedChunks(level, spots);

			if (retrying) {
				emptyLost(level, spots);
				spots.pending().forEach(spot -> tryFillIfLoaded(level, spots, spot));
			}

			if (spots.takeSaveSoon()) level.getDataStorage().scheduleSave();
		}
	}

	// A chunk's entities load after the chunk itself, so a loaded chunk waits here until they have, or until it unloads.
	private static void tryLoadedChunks(ServerLevel level, RestoreSpots spots) {
		LongSet chunks = loadedChunksOf(level);

		chunks.removeIf((long chunk) -> {
			if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) return true;

			if (!level.areEntitiesLoaded(chunk)) return false;

			spots.pendingIn(chunk).forEach(spot -> tryFill(level, spots, spot));

			return true;
		});
	}

	// Once a chunk's entities have loaded, every entity saved in it is in the level. A spot whose occupant was saved
	// there, and that no level holds, lost it without hearing: it was killed or dropped before a crash that kept the
	// spot from being saved, or the chunk's entities were deleted.
	private static void emptyLost(ServerLevel level, RestoreSpots spots) {
		spots.occupied().stream()
			.filter(spot -> spot.occupantChunk().filter(level::areEntitiesLoaded).isPresent())
			.filter(spot -> !isLoaded(level.getServer(), spot.occupant().orElseThrow()))
			.forEach(spot -> spots.update(spot.emptied()));
	}

	// Whether any level holds the entity, in a chunk players can see or not.
	private static boolean isLoaded(MinecraftServer server, UUID entity) {
		for (ServerLevel level : server.getAllLevels()) {
			if (((ServerLevelEntityManagerAccessor) level).maple$entityManager().isLoaded(entity)) return true;
		}

		return false;
	}

	private static void tryFillIfLoaded(ServerLevel level, RestoreSpots spots, RestoreSpot spot) {
		if (level.areEntitiesLoaded(spot.chunk())) tryFill(level, spots, spot);
	}

	private static void tryFill(ServerLevel level, RestoreSpots spots, RestoreSpot spot) {
		if (!canFill(level, spot)) return;

		Optional<Mob> mob = restoredMob(level, spot);

		if (mob.isEmpty() || !level.addFreshEntity(mob.get())) return;

		spots.update(spot.filledBy(mob.get().getUUID(), savedChunk(mob.get(), level.dimension())));
	}

	// Whether a pending spot can fill now. Clear means no block and no mob in the way. Collisions skip unloaded chunks, so
	// every chunk the mob would overlap must be loaded for "clear" to mean anything.
	private static boolean canFill(ServerLevel level, RestoreSpot spot) {
		Vec3 position = spot.position();
		AABB body = spot.type().getSpawnAABB(position.x, position.y, position.z);

		return level.hasChunksAt(Mth.floor(body.minX), Mth.floor(body.minZ), Mth.floor(body.maxX), Mth.floor(body.maxZ))
			&& spot.structure().holds(level, position)
			&& level.noCollision(body)
			&& level.isUnobstructed(null, Shapes.create(body))
			&& level.players().stream().noneMatch(player -> player.position().closerThan(position, PLAYER_CLEARANCE));
	}

	// A new mob of the spot's type, set up as its structure would set it up, holding the spot. Creating it fails, and the
	// spot waits, where the mob can't spawn now, as a hostile mob can't in Peaceful.
	private static Optional<Mob> restoredMob(ServerLevel level, RestoreSpot spot) {
		if (!(spot.type().create(level, EntitySpawnReason.STRUCTURE) instanceof Mob mob)) return Optional.empty();

		Vec3 position = spot.position();

		mob.snapTo(position.x, position.y, position.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(position)), EntitySpawnReason.STRUCTURE, null);

		if (spot.persistent()) mob.setPersistenceRequired();

		mob.setAttached(SPOT, new SpotTag(level.dimension(), spot.id(), Optional.of(spot)));

		return Optional.of(mob);
	}

	// A living mob that carries a spot holds it, whatever the level's spots say: they're saved at other times than the
	// mob, so a crash can leave them without its spot, with it empty, or with it held by a mob that died before this
	// one was Restored. Only a spot another loaded mob holds is lost to it.
	private static void hold(Entity entity, ServerLevel level) {
		SpotTag tag = entity.getAttached(SPOT);

		if (tag == null || !entity.isAlive()) return;

		ServerLevel spotLevel = level.getServer().getLevel(tag.dimension());

		if (spotLevel == null) return;

		RestoreSpots spots = RestoreSpots.of(spotLevel);
		Optional<RestoreSpot> recorded = spots.get(tag.spot());
		Optional<UUID> holder = recorded.flatMap(RestoreSpot::occupant);

		if (holder.equals(Optional.of(entity.getUUID()))) return;

		if (holder.isPresent() && isLoaded(level.getServer(), holder.get())) {
			entity.removeAttached(SPOT);

			return;
		}

		recorded.or(tag::planned).ifPresent(spot -> spots.update(spot.filledBy(entity.getUUID(), savedChunk(entity, tag.dimension()))));
	}

	// Removed for good without dying, as by converting, despawning or Peaceful's removal of monsters, or else unloading
	// to wherever it's saved next.
	private static void emptyOrRecordChunk(Entity entity, ServerLevel level) {
		Entity.RemovalReason reason = entity.getRemovalReason();

		if (reason != null && reason.shouldDestroy()) {
			empty(level.getServer(), entity);
		} else {
			recordOccupantChunk(level.getServer(), entity);
		}
	}

	// A chunk writes its mobs as it unloads, long before the next autosave writes the spots. So a spot whose occupant
	// unloads somewhere new is saved at the end of the tick, or a crash would leave it looking in the wrong chunk.
	private static void recordOccupantChunk(MinecraftServer server, Entity entity) {
		SpotTag tag = entity.getAttached(SPOT);
		ServerLevel spotLevel = tag == null ? null : server.getLevel(tag.dimension());

		if (spotLevel == null) return;

		RestoreSpots spots = RestoreSpots.of(spotLevel);
		Optional<RestoreSpot> held = spots.get(tag.spot()).filter(spot -> spot.occupant().equals(Optional.of(entity.getUUID())));

		if (held.isPresent() && recordOccupantChunk(spots, held.get(), entity, tag.dimension())) spots.saveSoon();
	}

	// An autosave writes the spots and then every loaded mob, wherever each has wandered since it loaded.
	private static void recordOccupantChunks(MinecraftServer server) {
		for (ServerLevel spotLevel : server.getAllLevels()) {
			RestoreSpots spots = spotLevel.getDataStorage().get(RestoreSpots.TYPE);

			if (spots == null) continue;

			for (RestoreSpot spot : spots.occupied()) {
				Entity occupant = loadedEntity(server, spot.occupant().orElseThrow());

				if (occupant != null) recordOccupantChunk(spots, spot, occupant, spotLevel.dimension());
			}
		}
	}

	// Returns whether the spot's occupant is saved somewhere new.
	private static boolean recordOccupantChunk(RestoreSpots spots, RestoreSpot spot, Entity occupant, ResourceKey<Level> spotDimension) {
		Optional<Long> chunk = savedChunk(occupant, spotDimension);

		if (chunk.equals(spot.occupantChunk())) return false;

		spots.update(spot.filledBy(occupant.getUUID(), chunk));

		return true;
	}

	// The chunk of the spot's level the mob is saved in. A mob is saved with whatever it rides, and in no chunk while it
	// leaves the level or rides with a player, who is saved with everything riding along.
	private static Optional<Long> savedChunk(Entity mob, ResourceKey<Level> spotDimension) {
		Entity.RemovalReason reason = mob.getRemovalReason();
		Entity savedWith = mob.getRootVehicle();
		boolean staysInLevel = reason == null || reason.shouldSave();
		boolean inChunk = staysInLevel && mob.level().dimension().equals(spotDimension) && savedWith.shouldBeSaved();

		return inChunk ? Optional.of(savedWith.chunkPosition().pack()) : Optional.empty();
	}

	// The entity where players' chunks show it, in whichever level that is.
	private static @Nullable Entity loadedEntity(MinecraftServer server, UUID entity) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity loaded = level.getEntity(entity);

			if (loaded != null) return loaded;
		}

		return null;
	}

	private static void empty(MinecraftServer server, Entity entity) {
		SpotTag tag = entity.getAttached(SPOT);

		if (tag != null) empty(server, tag, entity.getUUID());
	}

	// Emptying a spot twice, or one another mob holds now, changes nothing. A spot the level never saved is recorded
	// empty from the mob's tag.
	private static void empty(MinecraftServer server, SpotTag tag, UUID mob) {
		ServerLevel level = server.getLevel(tag.dimension());

		if (level == null) return;

		RestoreSpots spots = RestoreSpots.of(level);

		Optional<RestoreSpot> recorded = spots.get(tag.spot());
		Optional<RestoreSpot> held = recorded.isPresent() ? recorded.filter(spot -> spot.occupant().equals(Optional.of(mob))) : tag.planned();

		held.ifPresent(spot -> spots.update(spot.emptied()));
	}

	// Pending spots stay pending, so missed Blood Moons don't stack.
	private static void makeEmptySpotsPending(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			RestoreSpots spots = level.getDataStorage().get(RestoreSpots.TYPE);

			if (spots != null) spots.empty().forEach(spot -> spots.update(spot.madePending()));
		}
	}

	private record Dropped(SpotTag spot, UUID mob) {
	}

	private static LongSet loadedChunksOf(ServerLevel level) {
		return LOADED_CHUNKS.computeIfAbsent(level.dimension(), dimension -> new LongOpenHashSet());
	}

	/**
	 * The spot a mob holds. Saved with the mob.
	 *
	 * @param dimension the level its spot is in, which a mob that changed dimension has left
	 * @param planned the spot as the mob took it, to record if the level's spots lack it. A mob saved before tags carried
	 *     it has none.
	 */
	private record SpotTag(ResourceKey<Level> dimension, UUID spot, Optional<RestoreSpot> planned) {
		static final Codec<SpotTag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(SpotTag::dimension),
			UUIDUtil.CODEC.fieldOf("spot").forGetter(SpotTag::spot),
			RestoreSpot.CODEC.optionalFieldOf("planned").forGetter(SpotTag::planned)
		).apply(instance, SpotTag::new));
	}
}

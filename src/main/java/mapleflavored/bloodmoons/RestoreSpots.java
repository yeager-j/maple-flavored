package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import mapleflavored.MapleFlavored;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every Restore spot in a level, by id and by chunk. Spots are planned on worldgen threads with {@link #plan} and
 * recorded here by the server thread before any read.
 */
public final class RestoreSpots extends SavedData {
	private static final Codec<RestoreSpots> CODEC = RestoreSpot.CODEC.listOf().xmap(RestoreSpots::new, RestoreSpots::all);

	/** The level saved data that holds the spots. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<RestoreSpots> TYPE = new SavedDataType<>(MapleFlavored.id("restore_spots"), RestoreSpots::new, CODEC, null);

	// Worldgen threads can't touch saved data. Their spots wait here until the server thread records them, which it does
	// each tick, before every save and before any read. The save matters at shutdown: the server finishes its worldgen
	// after its last tick, and saves those chunks with their tagged mobs.
	private static final Queue<Planned> PLANNED = new ConcurrentLinkedQueue<>();

	private final Map<UUID, RestoreSpot> spotsById = new LinkedHashMap<>();
	private final Map<Long, Set<UUID>> spotsByChunk = new HashMap<>();
	private final Set<UUID> pending = new HashSet<>();

	private RestoreSpots() {
	}

	private RestoreSpots(List<RestoreSpot> spots) {
		spots.forEach(this::put);
	}

	/** Records a spot a structure just placed in {@code dimension}. Safe to call from any thread. */
	static void plan(ResourceKey<Level> dimension, RestoreSpot spot) {
		PLANNED.add(new Planned(dimension, spot));
	}

	/** Forgets spots planned for a world that's no longer running. */
	static void forgetPlanned() {
		PLANNED.clear();
	}

	/** Records every planned spot in its level. */
	static void recordPlanned(MinecraftServer server) {
		for (Planned planned = PLANNED.poll(); planned != null; planned = PLANNED.poll()) {
			ServerLevel level = server.getLevel(planned.dimension());

			// A dimension removed since, such as a datapack's, has no structure to Restore.
			if (level == null) continue;

			RestoreSpots spots = level.getDataStorage().computeIfAbsent(TYPE);

			spots.put(planned.spot());
			spots.setDirty();
		}
	}

	/** The level's spots, with every planned spot recorded. */
	static RestoreSpots of(ServerLevel level) {
		recordPlanned(level.getServer());

		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Every spot in the level, in the order they were recorded. */
	public static List<RestoreSpot> in(ServerLevel level) {
		return of(level).all();
	}

	Optional<RestoreSpot> get(UUID id) {
		return Optional.ofNullable(spotsById.get(id));
	}

	/** The pending spots in the chunk. */
	List<RestoreSpot> pendingIn(long chunk) {
		return spotsByChunk.getOrDefault(chunk, Set.of()).stream()
			.filter(pending::contains)
			.map(spotsById::get)
			.toList();
	}

	/** Every pending spot. */
	List<RestoreSpot> pending() {
		return pending.stream().map(spotsById::get).toList();
	}

	/** Every empty spot. */
	List<RestoreSpot> empty() {
		return spotsById.values().stream().filter(spot -> spot.state() == RestoreSpot.State.EMPTY).toList();
	}

	/** Replaces the spot with the same id. */
	void update(RestoreSpot spot) {
		put(spot);
		setDirty();
	}

	private void put(RestoreSpot spot) {
		spotsById.put(spot.id(), spot);
		spotsByChunk.computeIfAbsent(spot.chunk(), chunk -> new HashSet<>()).add(spot.id());

		if (spot.state() == RestoreSpot.State.PENDING) {
			pending.add(spot.id());
		} else {
			pending.remove(spot.id());
		}
	}

	private List<RestoreSpot> all() {
		return List.copyOf(spotsById.values());
	}

	private record Planned(ResourceKey<Level> dimension, RestoreSpot spot) {
	}
}

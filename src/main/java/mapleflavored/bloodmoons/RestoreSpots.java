package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongPredicate;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mapleflavored.MapleFlavored;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every Restore spot in a level, by id and by chunk, and the chunks whose pending spots are waiting to be tried. Server
 * thread only.
 */
public final class RestoreSpots extends SavedData {
	private static final Codec<RestoreSpots> CODEC = RestoreSpot.CODEC.listOf().xmap(RestoreSpots::new, RestoreSpots::all);

	// The level saved data that holds the spots. Fabric skips data fixing for a null fix type.
	static final SavedDataType<RestoreSpots> TYPE = new SavedDataType<>(MapleFlavored.id("restore_spots"), RestoreSpots::new, CODEC, null);

	private final Map<UUID, RestoreSpot> spotsById = new LinkedHashMap<>();
	private final Map<Long, Set<UUID>> spotsByChunk = new HashMap<>();
	private final Set<UUID> pending = new HashSet<>();
	// Chunks holding spots that have loaded since their pending spots were last tried. Not saved: a chunk that's loaded
	// when the level starts reports loading again.
	private final LongSet loadedChunks = new LongOpenHashSet();
	private boolean savesSoon;

	private RestoreSpots() {
	}

	private RestoreSpots(List<RestoreSpot> spots) {
		spots.forEach(this::put);
	}

	/** The level's spots, starting them if it has none. */
	static RestoreSpots of(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The level's spots, if it has ever had one. */
	static Optional<RestoreSpots> ifAny(ServerLevel level) {
		return Optional.ofNullable(level.getDataStorage().get(TYPE));
	}

	Optional<RestoreSpot> get(UUID id) {
		return Optional.ofNullable(spotsById.get(id));
	}

	/** Every spot, in the order they were recorded. */
	List<RestoreSpot> all() {
		return List.copyOf(spotsById.values());
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

	/** Every occupied spot. */
	List<RestoreSpot> occupied() {
		return spotsById.values().stream().filter(spot -> spot.state() == RestoreSpot.State.OCCUPIED).toList();
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

	/** Notes that the chunk has loaded, so its pending spots are tried. A chunk that holds no spot isn't kept. */
	void chunkLoaded(long chunk) {
		if (spotsByChunk.containsKey(chunk)) loadedChunks.add(chunk);
	}

	/** Offers each loaded chunk still waiting to {@code tried}, and stops waiting on those it returns true for. */
	void tryLoadedChunks(LongPredicate tried) {
		loadedChunks.removeIf(tried);
	}

	/** Asks for the spots to be saved at the end of this tick, ahead of the next autosave. */
	void saveSoon() {
		savesSoon = true;
	}

	/** Whether a save was asked for since this was last called. */
	boolean takeSaveSoon() {
		boolean asked = savesSoon;

		savesSoon = false;

		return asked;
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
}

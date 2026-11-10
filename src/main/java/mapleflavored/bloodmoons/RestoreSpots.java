package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
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

/** Every Restore spot in a level, by id and by chunk. Server thread only. */
public final class RestoreSpots extends SavedData {
	private static final Codec<RestoreSpots> CODEC = RestoreSpot.CODEC.listOf().xmap(RestoreSpots::new, RestoreSpots::all);

	/** The level saved data that holds the spots. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<RestoreSpots> TYPE = new SavedDataType<>(MapleFlavored.id("restore_spots"), RestoreSpots::new, CODEC, null);

	private final Map<UUID, RestoreSpot> spotsById = new LinkedHashMap<>();
	private final Map<Long, Set<UUID>> spotsByChunk = new HashMap<>();
	private final Set<UUID> pending = new HashSet<>();
	private boolean savesSoon;

	private RestoreSpots() {
	}

	private RestoreSpots(List<RestoreSpot> spots) {
		spots.forEach(this::put);
	}

	/** The level's spots. */
	static RestoreSpots of(ServerLevel level) {
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

	private List<RestoreSpot> all() {
		return List.copyOf(spotsById.values());
	}
}

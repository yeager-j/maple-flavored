package mapleflavored.strongholds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * What this mod knows about each kind of piece vanilla builds a Stronghold from: one row a kind. A piece with a row is a
 * Stronghold piece, and its row says who guards it and whether its Evoker can keep the Stronghold's book.
 */
final class StrongholdRooms {
	private static final float CORRIDOR_CHANCE = 0.25F;
	// Vanilla's test for a library with an upper walkway.
	private static final int SHORT_LIBRARY_HEIGHT = 6;

	// The ground floor of a piece: guards stand at the lowest open place in their column.
	private static final int GROUND = 1;
	// The feet of a guard on a tall library's upper walkway, over its oak plank floor.
	private static final int LIBRARY_WALKWAY = 6;

	private static final Area ANYWHERE = new Area((x, z) -> true, GROUND);
	private static final Area PORTAL_ROOM_FLOOR = new Area((x, z) -> !portalRoomBlocked(x, z), GROUND);
	private static final Area LIBRARY_FLOOR = new Area((x, z) -> !libraryBlocked(x, z), GROUND);
	private static final Area LIBRARY_BACK = new Area((x, z) -> !libraryBlocked(x, z) && z >= 10, GROUND);
	private static final Area LIBRARY_UPPER_WALKWAY = new Area(StrongholdRooms::onLibraryWalkway, LIBRARY_WALKWAY);
	// The hall the cells open onto, west of their bars.
	private static final Area PRISON_HALL = new Area((x, z) -> x <= 3, GROUND);

	private static final Garrison NO_GUARDS = new Garrison(List.of(), 0.0F);
	private static final Garrison CORRIDOR_GUARD = new Garrison(List.of(new Post(Guard.ROLL, ANYWHERE)), CORRIDOR_CHANCE);
	private static final Room CORRIDOR = new Room(CORRIDOR_GUARD, Optional.empty());

	private static final StructurePieceType PORTAL_ROOM = StructurePieceType.STRONGHOLD_PORTAL_ROOM;

	// Every kind of Stronghold piece. The portal room is always a keeper room, so its Evoker is one of its own guards.
	private static final Map<StructurePieceType, Room> ROOMS = Map.ofEntries(
		// The starting staircase is left unguarded.
		Map.entry(StructurePieceType.STRONGHOLD_START, new Room(NO_GUARDS, Optional.empty())),
		Map.entry(
			PORTAL_ROOM,
			new Room(
				always(
					new Post(Guard.EVOKER, PORTAL_ROOM_FLOOR),
					new Post(Guard.VINDICATOR, PORTAL_ROOM_FLOOR),
					new Post(Guard.VINDICATOR, PORTAL_ROOM_FLOOR)
				),
				Optional.empty()
			)
		),
		Map.entry(
			StructurePieceType.STRONGHOLD_LIBRARY,
			new Room(always(new Post(Guard.VINDICATOR, LIBRARY_FLOOR)), Optional.of(new BigRoomEvoker(StrongholdRooms::libraryEvokerArea, true)))
		),
		Map.entry(
			StructurePieceType.STRONGHOLD_ROOM_CROSSING,
			new Room(
				always(new Post(Guard.ROLL, ANYWHERE), new Post(Guard.ROLL, ANYWHERE)),
				Optional.of(new BigRoomEvoker(room -> ANYWHERE, false))
			)
		),
		Map.entry(
			StructurePieceType.STRONGHOLD_PRISON_HALL,
			new Room(always(new Post(Guard.VINDICATOR, PRISON_HALL)), Optional.of(new BigRoomEvoker(room -> PRISON_HALL, false)))
		),
		Map.entry(StructurePieceType.STRONGHOLD_FIVE_CROSSING, new Room(always(new Post(Guard.ROLL, ANYWHERE)), Optional.empty())),
		Map.entry(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_LEFT_TURN, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_RIGHT_TURN, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STAIRS_DOWN, CORRIDOR),
		Map.entry(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, CORRIDOR)
	);

	private StrongholdRooms() {
	}

	/** Whether vanilla builds Strongholds from pieces of this type. */
	static boolean isKind(StructurePieceType type) {
		return ROOMS.containsKey(type);
	}

	/** What's known about the piece's kind, if it's a Stronghold piece. */
	static Optional<Room> of(StructurePiece piece) {
		return Optional.ofNullable(ROOMS.get(piece.getType()));
	}

	static boolean isPortalRoom(StructurePiece piece) {
		return piece.getType() == PORTAL_ROOM;
	}

	private static Garrison always(Post... posts) {
		return new Garrison(List.of(posts), 1.0F);
	}

	// A library's Evoker keeps to the upper walkway when there is one, and to the back of the room when there isn't.
	private static Area libraryEvokerArea(StructurePiece library) {
		boolean tall = library.getBoundingBox().getYSpan() > SHORT_LIBRARY_HEIGHT;

		return tall ? LIBRARY_UPPER_WALKWAY : LIBRARY_BACK;
	}

	// The lava troughs by the door, the End portal with its frame and lava pool, and the silverfish spawner on the stairs.
	private static boolean portalRoomBlocked(int x, int z) {
		boolean lavaTrough = (x == 1 || x == 9) && z <= 3;
		boolean portal = x >= 3 && x <= 7 && z >= 8 && z <= 12;
		boolean spawner = x == 5 && z == 6;

		return lavaTrough || portal || spawner;
	}

	// The bookshelves along both walls and the shelves standing in rows between them.
	private static boolean libraryBlocked(int x, int z) {
		boolean wallShelf = x == 1 || x == 12;
		boolean shelfRow = z % 2 == 1 && z >= 3 && z <= 11 && x >= 3 && x <= 10 && x != 5 && x != 8;

		return wallShelf || shelfRow;
	}

	// The open edge of a tall library's walkway, between its wall shelves and its fence, clear of the ladder.
	private static boolean onLibraryWalkway(int x, int z) {
		boolean betweenShelves = x >= 2 && x <= 11;
		boolean edge = x == 2 || x == 11 || z == 1 || z == 13;
		boolean ladder = x == 10 && z == 13;

		return betweenShelves && edge && !ladder;
	}

	/**
	 * One kind of Stronghold piece.
	 *
	 * @param guards its garrison, apart from a big room's Evoker
	 * @param evoker the Evoker of a big room, which can be one of the Stronghold's far keeper rooms
	 */
	record Room(Garrison guards, Optional<BigRoomEvoker> evoker) {
		/** Whether its Evoker can keep the Stronghold's book, as one of the keeper rooms chosen far from the portal room. */
		boolean canBeFarKeeperRoom() {
			return evoker.isPresent();
		}

		/**
		 * The guards the piece places, its Evoker first.
		 *
		 * @param keeperRoom whether the piece is one of its Stronghold's keeper rooms
		 */
		Garrison garrison(StructurePiece piece, boolean keeperRoom) {
			return evoker.filter(post -> keeperRoom || post.evenWhenNoKeeper())
				.map(post -> guards.withFirst(new Post(Guard.EVOKER, post.area().apply(piece))))
				.orElse(guards);
		}
	}

	/**
	 * A big room's Evoker.
	 *
	 * @param area where in the room it stands
	 * @param evenWhenNoKeeper whether the room has it whether or not it's a keeper room, as every library does
	 */
	record BigRoomEvoker(Function<StructurePiece, Area> area, boolean evenWhenNoKeeper) {
	}

	/** The guards a piece places, all or none, with the chance it places them. */
	record Garrison(List<Post> posts, float chance) {
		private Garrison withFirst(Post post) {
			List<Post> withPost = new ArrayList<>(List.of(post));

			withPost.addAll(posts);

			return new Garrison(List.copyOf(withPost), chance);
		}
	}

	/** One guard of a garrison and the part of its piece it stands in. */
	record Post(Guard guard, Area area) {
	}

	/**
	 * Where in a piece a guard may stand.
	 *
	 * @param columns the piece-local columns open to it
	 * @param lowestFeet the lowest piece-local height its feet may stand at
	 */
	record Area(LocalColumns columns, int lowestFeet) {
	}

	/** A set of columns in a code-built piece's own coordinates. */
	@FunctionalInterface
	interface LocalColumns {
		boolean test(int x, int z);
	}

	enum Guard {
		VINDICATOR,
		EVOKER,
		/** A Vindicator or a Pillager, 50/50. */
		ROLL;

		EntityType<? extends Mob> roll(WorldgenRandom random) {
			return switch (this) {
				case VINDICATOR -> EntityTypes.VINDICATOR;
				case EVOKER -> EntityTypes.EVOKER;
				case ROLL -> random.nextBoolean() ? EntityTypes.VINDICATOR : EntityTypes.PILLAGER;
			};
		}
	}
}

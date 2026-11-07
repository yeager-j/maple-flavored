package mapleflavored.strongholds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Routes through a Stronghold room by room. A Stronghold's pieces sit wall to wall, and two pieces join where a doorway
 * opens through both walls. A route is the fewest doorways from one piece to another, found by reading the blocks, so a
 * doorway the player walls up closes it.
 */
public final class StrongholdRoutes {
	// Which rooms each room's walls touch, by Stronghold. Fixed once planned, so worked out once; whether a doorway is
	// open is read live. Server thread only.
	private static final Map<StructureStart, Map<StructurePiece, List<Touching>>> TOUCHING = new WeakHashMap<>();

	private StrongholdRoutes() {
	}

	/**
	 * The way from {@code from} to {@code to} through the Stronghold they're both in: one spot per doorway, each just
	 * inside the next room on the way, in order. Empty when they're in the same room, not both in the same Stronghold, or
	 * no open route joins their rooms. Reads only loaded chunks: a doorway in an unloaded one is closed.
	 */
	public static List<BlockPos> route(ServerLevel level, BlockPos from, BlockPos to) {
		StructureStart stronghold = level.structureManager().getStructureWithPieceAt(from, structure -> structure.is(BuiltinStructures.STRONGHOLD));

		if (!stronghold.isValid()) return List.of();

		Map<StructurePiece, List<Touching>> touching = TOUCHING.computeIfAbsent(stronghold, StrongholdRoutes::touchingRooms);
		Optional<StructurePiece> start = roomAt(touching.keySet(), from);
		Optional<StructurePiece> end = roomAt(touching.keySet(), to);

		if (start.isEmpty() || end.isEmpty() || start.get() == end.get()) return List.of();

		return fewestDoorways(level, touching, start.get(), end.get());
	}

	/**
	 * How many rooms apart each of the Stronghold's {@code rooms} is from {@code from}, by walls that touch, as planned and
	 * before any block is placed: 0 for {@code from} itself, 1 for the rooms beside it. A room no chain of touching walls
	 * reaches is left out. Rooms that touch without a doorway between them count as beside each other.
	 */
	public static Map<StructurePiece, Integer> roomsApart(List<StructurePiece> rooms, StructurePiece from) {
		Map<StructurePiece, List<Touching>> touching = touchingRooms(rooms);
		Map<StructurePiece, Integer> apart = new HashMap<>(Map.of(from, 0));
		Queue<StructurePiece> frontier = new ArrayDeque<>(List.of(from));

		while (!frontier.isEmpty()) {
			StructurePiece room = frontier.poll();

			for (Touching next : touching.getOrDefault(room, List.of())) {
				if (apart.containsKey(next.room())) continue;

				apart.put(next.room(), apart.get(room) + 1);
				frontier.add(next.room());
			}
		}

		return apart;
	}

	private static Map<StructurePiece, List<Touching>> touchingRooms(StructureStart stronghold) {
		return touchingRooms(stronghold.getPieces().stream().filter(StrongholdLayout::isStrongholdPiece).toList());
	}

	private static Map<StructurePiece, List<Touching>> touchingRooms(List<StructurePiece> rooms) {
		Map<StructurePiece, List<Touching>> touching = new HashMap<>();

		for (StructurePiece room : rooms) {
			List<Touching> neighbours = new ArrayList<>();

			for (StructurePiece other : rooms) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					if (touchesOn(room.getBoundingBox(), other.getBoundingBox(), side)) neighbours.add(new Touching(other, side));
				}
			}

			touching.put(room, List.copyOf(neighbours));
		}

		return touching;
	}

	// Breadth first, so the route found crosses the fewest doorways.
	private static List<BlockPos> fewestDoorways(
		ServerLevel level,
		Map<StructurePiece, List<Touching>> touching,
		StructurePiece start,
		StructurePiece end
	) {
		Map<StructurePiece, Step> stepsInto = new HashMap<>();
		Queue<StructurePiece> frontier = new ArrayDeque<>(List.of(start));

		stepsInto.put(start, new Step(start, BlockPos.ZERO));

		while (!frontier.isEmpty()) {
			StructurePiece room = frontier.poll();

			if (room == end) return waypoints(stepsInto, start, end);

			for (Touching next : touching.get(room)) {
				if (stepsInto.containsKey(next.room())) continue;

				Optional<BlockPos> doorway = lowestDoorway(level, room.getBoundingBox(), next.room().getBoundingBox(), next.side());

				if (doorway.isEmpty()) continue;

				stepsInto.put(next.room(), new Step(room, doorway.get()));
				frontier.add(next.room());
			}
		}

		return List.of();
	}

	private static List<BlockPos> waypoints(Map<StructurePiece, Step> stepsInto, StructurePiece start, StructurePiece end) {
		List<BlockPos> waypoints = new ArrayList<>();

		for (StructurePiece room = end; room != start; room = stepsInto.get(room).from()) {
			waypoints.addFirst(stepsInto.get(room).waypoint());
		}

		return waypoints;
	}

	// Whether {@code into} sits against {@code from}'s {@code side} wall, overlapping it along that wall.
	private static boolean touchesOn(BoundingBox from, BoundingBox into, Direction side) {
		boolean overlapsY = from.minY() <= into.maxY() && into.minY() <= from.maxY();
		boolean overlapsX = from.minX() <= into.maxX() && into.minX() <= from.maxX();
		boolean overlapsZ = from.minZ() <= into.maxZ() && into.minZ() <= from.maxZ();

		return overlapsY && switch (side) {
			case EAST -> from.maxX() + 1 == into.minX() && overlapsZ;
			case WEST -> into.maxX() + 1 == from.minX() && overlapsZ;
			case SOUTH -> from.maxZ() + 1 == into.minZ() && overlapsX;
			case NORTH -> into.maxZ() + 1 == from.minZ() && overlapsX;
			default -> false;
		};
	}

	// The spot just inside {@code into} through the lowest doorway two blocks high that opens through both walls.
	private static Optional<BlockPos> lowestDoorway(ServerLevel level, BoundingBox from, BoundingBox into, Direction side) {
		int minY = Math.max(from.minY(), into.minY());
		int maxY = Math.min(from.maxY(), into.maxY());
		boolean alongX = side.getAxis() == Direction.Axis.Z;
		int minAcross = alongX ? Math.max(from.minX(), into.minX()) : Math.max(from.minZ(), into.minZ());
		int maxAcross = alongX ? Math.min(from.maxX(), into.maxX()) : Math.min(from.maxZ(), into.maxZ());
		// The wall layer of each room, at the plane where they meet, then one step on into the next room.
		int fromWall = switch (side) {
			case EAST -> from.maxX();
			case WEST -> from.minX();
			case SOUTH -> from.maxZ();
			default -> from.minZ();
		};

		for (int y = minY; y < maxY; y++) {
			for (int across = minAcross; across <= maxAcross; across++) {
				BlockPos fromWallPos = alongX ? new BlockPos(across, y, fromWall) : new BlockPos(fromWall, y, across);
				BlockPos intoWallPos = fromWallPos.relative(side);

				if (isOpening(level, fromWallPos) && isOpening(level, intoWallPos)) return Optional.of(intoWallPos.relative(side));
			}
		}

		return Optional.empty();
	}

	// Two blocks of air a mob can walk through.
	private static boolean isOpening(ServerLevel level, BlockPos feet) {
		return level.hasChunkAt(feet) && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
	}

	private static Optional<StructurePiece> roomAt(Collection<StructurePiece> rooms, BlockPos pos) {
		return rooms.stream().filter(room -> room.getBoundingBox().isInside(pos)).findFirst();
	}

	// How the route enters a room: from which room, and the spot just inside it.
	private record Step(StructurePiece from, BlockPos waypoint) {
	}

	// A room whose wall touches another's {@code side} wall.
	private record Touching(StructurePiece room, Direction side) {
	}
}

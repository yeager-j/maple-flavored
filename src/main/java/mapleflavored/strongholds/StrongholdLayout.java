package mapleflavored.strongholds;

import java.util.Set;
import mapleflavored.mixin.StructurePieceInvoker;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * How Stronghold pieces build, apart from vanilla.
 *
 * <p>Strongholds generate whole, as Trial Chambers do. Vanilla builds a Stronghold piece's shell only where there's no
 * air yet, so caves, Mineshafts and dungeons dug earlier leave holes in it. Here every piece builds its whole shell over
 * whatever was there, and the Stronghold's {@code encapsulate} terrain adaptation, in data, packs rock around it.
 *
 * <p>Strongholds have no doors, so their Illagers can rush the player through every doorway. Wooden and iron doors,
 * the prison cells' among them, leave their frames open, and the buttons that opened iron doors go with them.
 *
 * <p>Every room can be walked to. Vanilla carves a north- or west-facing five-way crossing's side doorways where the
 * crossing's other side rooms would be, so a side room's doorway meets solid wall and a hole opens onto nothing, cutting
 * the Stronghold in two. Here each side doorway opens where its room is, and the stray holes are bricked up.
 */
public final class StrongholdLayout {
	private static final Set<StructurePieceType> PIECES = Set.of(
		StructurePieceType.STRONGHOLD_CHEST_CORRIDOR,
		StructurePieceType.STRONGHOLD_FILLER_CORRIDOR,
		StructurePieceType.STRONGHOLD_FIVE_CROSSING,
		StructurePieceType.STRONGHOLD_LEFT_TURN,
		StructurePieceType.STRONGHOLD_LIBRARY,
		StructurePieceType.STRONGHOLD_PORTAL_ROOM,
		StructurePieceType.STRONGHOLD_PRISON_HALL,
		StructurePieceType.STRONGHOLD_RIGHT_TURN,
		StructurePieceType.STRONGHOLD_ROOM_CROSSING,
		StructurePieceType.STRONGHOLD_STAIRS_DOWN,
		StructurePieceType.STRONGHOLD_START,
		StructurePieceType.STRONGHOLD_STRAIGHT,
		StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN
	);

	// A five-way crossing's side doorways, in its own coordinates: the lower one near its entrance and the upper one at the
	// back, through each side wall.
	private static final int LEFT_WALL = 0;
	private static final int RIGHT_WALL = 9;
	private static final int LOWER_DOORWAY_Y = 3;
	private static final int LOWER_DOORWAY_Z = 1;
	private static final int UPPER_DOORWAY_Y = 5;
	private static final int UPPER_DOORWAY_Z = 7;
	private static final int DOORWAY_SIZE = 3;

	private StrongholdLayout() {
	}

	/**
	 * Opens a five-way crossing's side doorways where its side rooms are, and bricks up the rest. Call after the crossing
	 * is built in each chunk, with the side rooms it planned.
	 */
	public static void realignFiveCrossingDoorways(
		StructurePiece crossing,
		WorldGenLevel level,
		BoundingBox chunkBox,
		boolean leftLow,
		boolean leftHigh,
		boolean rightLow,
		boolean rightHigh
	) {
		Direction facing = crossing.getOrientation();

		// Facing south or east, vanilla lines its doorways up with its rooms.
		if (facing != Direction.NORTH && facing != Direction.WEST) return;

		// Facing north or west, each planned "low" room lands at the upper doorway and each "high" room at the lower one.
		StructurePieceInvoker local = (StructurePieceInvoker) crossing;

		setDoorway(local, level, chunkBox, LEFT_WALL, LOWER_DOORWAY_Y, LOWER_DOORWAY_Z, leftHigh);
		setDoorway(local, level, chunkBox, LEFT_WALL, UPPER_DOORWAY_Y, UPPER_DOORWAY_Z, leftLow);
		setDoorway(local, level, chunkBox, RIGHT_WALL, LOWER_DOORWAY_Y, LOWER_DOORWAY_Z, rightHigh);
		setDoorway(local, level, chunkBox, RIGHT_WALL, UPPER_DOORWAY_Y, UPPER_DOORWAY_Z, rightLow);
	}

	private static void setDoorway(StructurePieceInvoker crossing, WorldGenLevel level, BoundingBox chunkBox, int wall, int y, int z, boolean open) {
		BlockState block = open ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
		int top = y + DOORWAY_SIZE - 1;
		int back = z + DOORWAY_SIZE - 1;

		crossing.fillBox(level, chunkBox, wall, y, z, wall, top, back, block, block, false);
	}

	/** Whether the piece is one of vanilla's Stronghold rooms, corridors or stairs, rather than one this mod adds. */
	public static boolean isStrongholdPiece(StructurePiece piece) {
		return PIECES.contains(piece.getType());
	}

	/**
	 * Whether a box the piece fills skips positions that are already air. Never for a Stronghold piece; otherwise as the
	 * piece asked.
	 */
	public static boolean skipsAir(StructurePiece piece, boolean skipAir) {
		return skipAir && !isStrongholdPiece(piece);
	}

	/** The block the piece places where it asked for {@code state}: cave air for a Stronghold's doors and buttons. */
	public static BlockState placed(StructurePiece piece, BlockState state) {
		boolean doorway = state.getBlock() instanceof DoorBlock || state.getBlock() instanceof ButtonBlock;

		return doorway && isStrongholdPiece(piece) ? Blocks.CAVE_AIR.defaultBlockState() : state;
	}
}

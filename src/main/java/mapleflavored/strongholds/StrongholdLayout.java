package mapleflavored.strongholds;

import mapleflavored.access.StructurePieceInvoker;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.Restoring;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * What a Stronghold is, and how its pieces build, apart from vanilla.
 *
 * <p>A Stronghold is the structure {@link Restorable#STRONGHOLD} names, the same one whose book and whose Illagers' hunt
 * go by that name. Its pieces build as described here only as part of one: another structure made of the same kinds of
 * piece, as a data pack can add, builds as vanilla's does and gets no guards.
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

	/** Whether the structure is a Stronghold. */
	public static boolean isStronghold(Holder<Structure> structure) {
		return Restorable.STRONGHOLD.is(structure);
	}

	/** Whether this thread is building a chunk of a Stronghold right now. */
	public static boolean isBuildingStronghold() {
		return Restoring.isBuilding(Restorable.STRONGHOLD);
	}

	/**
	 * Opens a Stronghold's five-way crossing's side doorways where its side rooms are, and bricks up the rest. Call
	 * after the crossing is built in each chunk, with the side rooms it planned.
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
		if (!isBuildingStronghold() || (facing != Direction.NORTH && facing != Direction.WEST)) return;

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

	/**
	 * Whether the piece is one of the rooms, corridors or stairs vanilla builds a Stronghold from, rather than one this
	 * mod adds. Sorts the pieces of a structure already known to be a Stronghold.
	 */
	public static boolean isStrongholdPiece(StructurePiece piece) {
		return isStrongholdPiece(piece.getType());
	}

	/** Whether vanilla builds Strongholds from pieces of this type. */
	public static boolean isStrongholdPiece(StructurePieceType type) {
		return StrongholdRooms.isKind(type);
	}

	/**
	 * Whether a box the piece fills skips positions that are already air. Never for a piece of a Stronghold being built;
	 * otherwise as the piece asked.
	 */
	public static boolean skipsAir(StructurePiece piece, boolean skipAir) {
		return skipAir && !isBuiltInStronghold(piece);
	}

	/**
	 * The block the piece places where it asked for {@code state}: cave air for the doors and buttons of a Stronghold
	 * being built.
	 */
	public static BlockState placed(StructurePiece piece, BlockState state) {
		boolean doorway = state.getBlock() instanceof DoorBlock || state.getBlock() instanceof ButtonBlock;

		return doorway && isBuiltInStronghold(piece) ? Blocks.CAVE_AIR.defaultBlockState() : state;
	}

	// The kind of piece is the cheaper test, and nearly every piece of every other structure fails it.
	private static boolean isBuiltInStronghold(StructurePiece piece) {
		return isStrongholdPiece(piece) && isBuildingStronghold();
	}
}

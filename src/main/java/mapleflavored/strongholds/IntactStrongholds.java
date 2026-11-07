package mapleflavored.strongholds;

import java.util.Set;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Strongholds generate whole, as Trial Chambers do. Vanilla builds a Stronghold piece's shell only where there's no air
 * yet, so caves, Mineshafts and dungeons dug earlier leave holes in it. Here every piece builds its whole shell over
 * whatever was there, and the Stronghold's {@code encapsulate} terrain adaptation, in data, packs rock around it.
 */
public final class IntactStrongholds {
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

	private IntactStrongholds() {
	}

	/**
	 * Whether a box the piece fills skips positions that are already air. Never for a Stronghold piece; otherwise as the
	 * piece asked.
	 */
	public static boolean skipsAir(StructurePiece piece, boolean skipAir) {
		return skipAir && !PIECES.contains(piece.getType());
	}
}

package mapleflavored.structurebooks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;

/** A Nether Fortress's Blaze spawner rooms, whose spawners lock the fortress's book until each is Cleared. */
public final class FortressBlazeSpawners {
	// Where a spawner room places its Blaze spawner, in the room's own coordinates.
	private static final BlockPos SPAWNER = new BlockPos(3, 5, 5);

	private FortressBlazeSpawners() {
	}

	/** Whether any of a fortress's planned pieces is a Blaze spawner room. */
	public static boolean hasRoom(List<StructurePiece> pieces) {
		return pieces.stream().anyMatch(FortressBlazeSpawners::isRoom);
	}

	/** Where each of the fortress's spawner rooms will place its Blaze spawner. */
	static List<BlockPos> spawners(List<StructurePiece> pieces) {
		return pieces.stream()
			.filter(FortressBlazeSpawners::isRoom)
			.map(room -> StructureBookSpots.inCodeBuiltPiece(room, SPAWNER).pos())
			.toList();
	}

	private static boolean isRoom(StructurePiece piece) {
		return piece instanceof NetherFortressPieces.MonsterThrone;
	}
}

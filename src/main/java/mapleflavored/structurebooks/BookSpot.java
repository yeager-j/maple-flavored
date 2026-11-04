package mapleflavored.structurebooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Where a structure's book goes, as planned: a world position inside one of its pieces. The book moves with that piece
 * when the piece settles onto the terrain as it's built.
 *
 * @param anchor the planned piece the book sits in
 * @param pos the book's world position while the pieces are planned
 */
public record BookSpot(StructurePiece anchor, BlockPos pos) {
}

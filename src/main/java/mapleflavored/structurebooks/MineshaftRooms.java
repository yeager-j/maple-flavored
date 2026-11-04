package mapleflavored.structurebooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Keeps a Mineshaft's central room, which holds its structure book, open and dry. Vanilla skips carving a room where it
 * touches liquid, which would bury the book; instead the room is always carved, and liquid around it turns to stone.
 */
public final class MineshaftRooms {
	private MineshaftRooms() {
	}

	/**
	 * Turns every liquid in the room's box and the layer around it to stone (deepslate below Y 0), within the chunk
	 * being built. Each chunk seals its own part, so the result doesn't depend on which chunk is built first.
	 */
	public static void sealLiquid(WorldGenLevel level, BoundingBox room, BoundingBox chunkBB) {
		BoundingBox shell = room.inflatedBy(1);
		int minX = Math.max(shell.minX(), chunkBB.minX());
		int minY = Math.max(shell.minY(), chunkBB.minY());
		int minZ = Math.max(shell.minZ(), chunkBB.minZ());
		int maxX = Math.min(shell.maxX(), chunkBB.maxX());
		int maxY = Math.min(shell.maxY(), chunkBB.maxY());
		int maxZ = Math.min(shell.maxZ(), chunkBB.maxZ());

		for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			if (level.getBlockState(pos).liquid()) level.setBlock(pos, sealFor(pos), Block.UPDATE_CLIENTS);
		}
	}

	private static BlockState sealFor(BlockPos pos) {
		return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
	}
}

package mapleflavored.structurebooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mapleflavored.mixin.MineShaftCorridorAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;

/**
 * Where a Mineshaft's book goes: floating at head height in a hallway, at the support in the middle of a random
 * corridor, between its two oak fence posts and above its rail line, in a pocket carved around it.
 *
 * <p>Vanilla skips building a corridor wherever it touches water or lava, or the Deep Dark. Chunks are built
 * independently and in any order, so the corridor is checked against the terrain preview when the Mineshaft is
 * planned, never as it's built.
 */
public final class MineshaftBookSpots {
	// Enough tries to find a dry corridor in a partly flooded Mineshaft, few enough to keep planning cheap.
	private static final int MAX_TRIES = 16;
	// Vanilla corridors are built from 5-block sections, with a support 2 blocks into each.
	private static final int SECTION_LENGTH = 5;
	private static final int SUPPORT_OFFSET = 2;

	private MineshaftBookSpots() {
	}

	/**
	 * The middle support of the first of the Mineshaft's corridors, in random order, whose spot the terrain preview shows
	 * dry and outside the Deep Dark. Cobwebbed spider corridors are tried only when there are no others. When no
	 * corridor passes, the first one tried. Empty only for a Mineshaft without corridors.
	 */
	public static Optional<BookSpot> choose(List<StructurePiece> pieces, TerrainPreview terrain, RandomSource random) {
		List<StructurePiece> corridors = corridorsToTry(pieces);

		Util.shuffle(corridors, random);

		List<StructurePiece> tries = corridors.subList(0, Math.min(MAX_TRIES, corridors.size()));

		return tries.stream()
			.filter(corridor -> isBuilt(corridor, terrain))
			.findFirst()
			.or(() -> tries.stream().findFirst())
			.map(corridor -> new BookSpot(corridor, middleSupport(corridor), BookSpot.Placement.POCKET));
	}

	private static List<StructurePiece> corridorsToTry(List<StructurePiece> pieces) {
		List<StructurePiece> corridors = pieces.stream().filter(MineshaftPieces.MineShaftCorridor.class::isInstance).toList();
		List<StructurePiece> openCorridors = corridors.stream()
			.filter(corridor -> !((MineShaftCorridorAccessor) corridor).isSpiderCorridor())
			.toList();

		return new ArrayList<>(openCorridors.isEmpty() ? corridors : openCorridors);
	}

	// At head height in the corridor's middle, at the support nearest its centre.
	private static BlockPos middleSupport(StructurePiece corridor) {
		BoundingBox box = corridor.getBoundingBox();
		boolean alongZ = corridor.getOrientation() != null && corridor.getOrientation().getAxis() == Direction.Axis.Z;
		int length = alongZ ? box.getZSpan() : box.getXSpan();
		int sections = length / SECTION_LENGTH;
		int alongMin = sections / 2 * SECTION_LENGTH + SUPPORT_OFFSET;
		int y = box.minY() + 1;

		return alongZ
			? new BlockPos(box.minX() + 1, y, box.minZ() + alongMin)
			: new BlockPos(box.minX() + alongMin, y, box.minZ() + 1);
	}

	// Mirrors vanilla's per-chunk skip check for the chunk holding the spot: no liquid on the corridor's box, grown by
	// one, and not in a biome that blocks Mineshafts. The preview is sampled at that box's corners and the spot.
	private static boolean isBuilt(StructurePiece corridor, TerrainPreview terrain) {
		BlockPos spot = middleSupport(corridor);

		if (terrain.biomeIs(spot, BiomeTags.MINESHAFT_BLOCKING)) return false;

		BoundingBox box = corridor.getBoundingBox().inflatedBy(1);
		ChunkPos chunk = ChunkPos.containing(spot);
		int minX = Math.max(box.minX(), chunk.getMinBlockX());
		int maxX = Math.min(box.maxX(), chunk.getMaxBlockX());
		int minZ = Math.max(box.minZ(), chunk.getMinBlockZ());
		int maxZ = Math.min(box.maxZ(), chunk.getMaxBlockZ());
		List<BlockPos> columns = List.of(
			spot,
			new BlockPos(minX, 0, minZ),
			new BlockPos(minX, 0, maxZ),
			new BlockPos(maxX, 0, minZ),
			new BlockPos(maxX, 0, maxZ)
		);

		return columns.stream().noneMatch(column -> terrain.hasLiquid(column.getX(), column.getZ(), box.minY(), box.maxY()));
	}
}

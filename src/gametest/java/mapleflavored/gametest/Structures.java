package mapleflavored.gametest;

import java.util.List;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Plans and places structures with the game's own structure placement, the way {@code /place structure} does, and finds
 * the structure books they hold.
 */
final class Structures {
	private Structures() {
	}

	/** Plans the structure as if it started in {@code chunkPos}, in any biome. Fails if it can't generate there. */
	// Mirrors PlaceCommand.placeStructure's call.
	static StructureStart plan(ServerLevel level, ResourceKey<Structure> key, ChunkPos chunkPos) {
		Holder.Reference<Structure> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		StructureStart start = structure.value().generate(
			structure,
			level.dimension(),
			level.registryAccess(),
			generator,
			generator.getBiomeSource(),
			randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED),
			randomState,
			level.getStructureTemplateManager(),
			level.getSeed(),
			chunkPos,
			0,
			level,
			biome -> true
		);

		if (!start.isValid()) throw new IllegalStateException(key.identifier() + " failed to generate at " + chunkPos);

		return start;
	}

	/** Plans the structure and builds every chunk it covers. */
	static StructureStart place(ServerLevel level, ResourceKey<Structure> key, ChunkPos chunkPos) {
		StructureStart start = plan(level, key, chunkPos);

		chunks(start).forEach(chunk -> build(level, start, chunk));

		return start;
	}

	/**
	 * Builds the structure's part of one chunk, loading it and its neighbours first. Worldgen always has the neighbours'
	 * terrain when it builds a chunk, and structures that settle onto the terrain, such as Ocean Ruins and Desert
	 * Pyramids, read heights there. An unloaded neighbour reads as the bottom of the world.
	 */
	static void build(ServerLevel level, StructureStart start, ChunkPos chunk) {
		BoundingBox chunkBox = new BoundingBox(
			chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY() + 1, chunk.getMaxBlockZ()
		);

		ChunkPos.rangeClosed(chunk, 1).forEach(near -> level.getChunk(near.x(), near.z()));
		start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, chunk);
	}

	/**
	 * Records the structure in its start chunk and every chunk it covers, as generation does, so the structure manager
	 * can find it by position.
	 */
	static void register(ServerLevel level, StructureStart start) {
		ChunkPos startChunk = start.getChunkPos();

		level.getChunk(startChunk.x(), startChunk.z()).setStartForStructure(start.getStructure(), start);
		chunks(start).forEach(chunk -> level.getChunk(chunk.x(), chunk.z()).addReferenceForStructure(start.getStructure(), startChunk.pack()));
	}

	/** The chunks the structure covers. */
	static List<ChunkPos> chunks(StructureStart start) {
		BoundingBox box = start.getBoundingBox();
		ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
		ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));

		return ChunkPos.rangeClosed(min, max).toList();
	}

	/** The positions of every structure book in the chunks the structure covers. */
	static List<BlockPos> books(ServerLevel level, StructureStart start) {
		return chunks(start).stream()
			.flatMap(chunk -> level.getChunk(chunk.x(), chunk.z()).getBlockEntities().values().stream())
			.filter(StructureBookBlockEntity.class::isInstance)
			.map(blockEntity -> blockEntity.getBlockPos().immutable())
			.toList();
	}

	/** The structure as saved with its chunk and loaded back. */
	static StructureStart saveAndLoad(ServerLevel level, StructureStart start) {
		StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level);
		CompoundTag tag = start.createTag(context, start.getChunkPos());
		StructureStart loaded = StructureStart.loadStaticStart(context, tag, level.getSeed());

		if (loaded == null) throw new IllegalStateException("the saved structure didn't load");

		return loaded;
	}
}

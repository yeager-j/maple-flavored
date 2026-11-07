package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import mapleflavored.bloodmoons.Restorable;
import mapleflavored.bloodmoons.Restoring;
import mapleflavored.structurebooks.StructureBooks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Builds each piece with the keepers it places, as the chunk it's in is built, so they unlock their book; and builds
 * each chunk of a Restorable structure so the mobs it places get Restore spots.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
	@WrapMethod(method = "placeInChunk")
	private void buildWithRestoreSpots(
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator generator,
		RandomSource random,
		BoundingBox chunkBB,
		ChunkPos chunkPos,
		Operation<Void> placeInChunk
	) {
		Optional<Restorable> structure = Restorable.of(level.registryAccess(), ((StructureStart) (Object) this).getStructure());

		Restoring.build(structure, () -> placeInChunk.call(level, structureManager, generator, random, chunkBB, chunkPos));
	}

	@WrapOperation(
		method = "placeInChunk",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;postProcess(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V"
		)
	)
	private void buildWithKeepers(
		StructurePiece piece,
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator generator,
		RandomSource random,
		BoundingBox chunkBB,
		ChunkPos chunkPos,
		BlockPos referencePos,
		Operation<Void> postProcess
	) {
		StructureStart start = (StructureStart) (Object) this;

		StructureBooks.buildPiece(
			piece,
			start.getPieces(),
			level,
			() -> postProcess.call(piece, level, structureManager, generator, random, chunkBB, chunkPos, referencePos)
		);
	}
}

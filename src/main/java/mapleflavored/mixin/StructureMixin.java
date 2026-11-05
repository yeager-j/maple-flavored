package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.TerrainPreview;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds a structure's book to its pieces as they're planned, for world generation and {@code /place structure} alike.
 * Fabric has no event for structure planning.
 */
@Mixin(Structure.class)
public abstract class StructureMixin {
	@ModifyExpressionValue(
		method = "generate",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePiecesBuilder;build()Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;"
		)
	)
	private PiecesContainer addStructureBook(
		PiecesContainer pieces,
		@Local(argsOnly = true) Holder<Structure> selected,
		@Local(argsOnly = true) ResourceKey<Level> dimension,
		@Local(argsOnly = true) ChunkGenerator chunkGenerator,
		@Local(argsOnly = true) BiomeSource biomeSource,
		@Local(argsOnly = true) Climate.Sampler climateSampler,
		@Local(argsOnly = true) RandomState randomState,
		@Local(argsOnly = true) StructureTemplateManager structureTemplateManager,
		@Local(argsOnly = true) long seed,
		@Local(argsOnly = true) ChunkPos sourceChunkPos,
		@Local(argsOnly = true) LevelHeightAccessor heightAccessor
	) {
		TerrainPreview terrain = TerrainPreview.of(chunkGenerator, biomeSource, climateSampler, randomState, heightAccessor);

		return StructureBookRules.withBook(selected, pieces, seed, sourceChunkPos, dimension, terrain, structureTemplateManager);
	}
}

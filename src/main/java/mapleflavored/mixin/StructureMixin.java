package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.StructureBookRules;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
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
		@Local(argsOnly = true) long seed,
		@Local(argsOnly = true) ChunkPos sourceChunkPos
	) {
		return StructureBookRules.withBook(selected, pieces, seed, sourceChunkPos);
	}
}

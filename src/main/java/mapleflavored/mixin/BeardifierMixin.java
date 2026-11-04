package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.StructureBookPiece;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps structure books out of terrain adaptation. Vanilla turns every non-jigsaw piece of a terrain-adapting structure,
 * such as an Abandoned Camp or Pillager Outpost, into a solid box the terrain is shaped around, which would raise a
 * mound under a floating book.
 */
@Mixin(Beardifier.class)
public abstract class BeardifierMixin {
	@ModifyExpressionValue(
		method = "forStructuresInChunk",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;isCloseToChunk(Lnet/minecraft/world/level/ChunkPos;I)Z"
		)
	)
	private static boolean skipStructureBooks(boolean isClose, @Local StructurePiece piece) {
		return isClose && !(piece instanceof StructureBookPiece);
	}
}

package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.structurebooks.StructureBooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps a structure's pieces from replacing a structure book, so a structure placed later, such as an overlapping
 * Mineshaft or Stronghold, can't erase another structure's book. Wrapping the check at its call site also covers
 * pieces that override it, as Mineshaft pieces do.
 */
@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {
	@ModifyExpressionValue(
		method = "placeBlock",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;canBeReplaced(Lnet/minecraft/world/level/LevelReader;IIILnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z"
		)
	)
	private boolean keepStructureBook(boolean canBeReplaced, @Local(argsOnly = true) WorldGenLevel level, @Local BlockPos pos) {
		return canBeReplaced && !level.getBlockState(pos).is(StructureBooks.BLOCK);
	}
}

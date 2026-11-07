package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.strongholds.StrongholdLayout;
import mapleflavored.structurebooks.StructureBooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Keeps a structure's pieces from replacing a structure book, so a structure placed later, such as an overlapping
 * Mineshaft or Stronghold, can't erase another structure's book. Wrapping the check at its call site also covers
 * pieces that override it, as Mineshaft pieces do. Also builds every Stronghold piece whole, over caves and all, and
 * without doors.
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

	// Every Stronghold piece builds its shell with this overload.
	@ModifyVariable(
		method = "generateBox(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;IIIIIIZLnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/StructurePiece$BlockSelector;)V",
		at = @At("HEAD"),
		argsOnly = true
	)
	private boolean buildStrongholdsWhole(boolean skipAir) {
		return StrongholdLayout.skipsAir((StructurePiece) (Object) this, skipAir);
	}

	@ModifyVariable(method = "placeBlock", at = @At("HEAD"), argsOnly = true)
	private BlockState openStrongholdDoorways(BlockState state) {
		return StrongholdLayout.placed((StructurePiece) (Object) this, state);
	}
}

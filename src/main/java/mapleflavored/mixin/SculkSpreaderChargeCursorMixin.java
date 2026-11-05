package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.subspaces.WardenSculk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A Warden Subspace's sculk cursors never move where they could spread outside it. */
@Mixin(SculkSpreader.ChargeCursor.class)
public abstract class SculkSpreaderChargeCursorMixin {
	@WrapOperation(
		method = "getValidMovementPos",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/SculkSpreader$ChargeCursor;canMoveToPos(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/SculkSpreader;)Z"
		)
	)
	private static boolean keepSubspaceSculkInside(
		BlockPos origin,
		BlockPos target,
		SculkSpreader spreader,
		Operation<Boolean> original,
		@Local(argsOnly = true) LevelAccessor level
	) {
		return WardenSculk.allowsCursorMove(level, spreader, target, () -> original.call(origin, target, spreader));
	}
}

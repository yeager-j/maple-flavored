package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mapleflavored.subspaces.GrownShriekers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shriekers grown by a Warden Subspace's sculk can summon, and remember the Subspace that grew them. */
@Mixin(SculkBlock.class)
public abstract class SculkBlockMixin {
	// The growth a charged sculk block places on top of itself.
	@WrapOperation(
		method = "attemptUseCharge",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/LevelAccessor;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
		)
	)
	private boolean recordShriekersGrownBySubspaces(
		LevelAccessor level,
		BlockPos position,
		BlockState growth,
		Operation<Boolean> original,
		@Local(argsOnly = true) SculkSpreader spreader
	) {
		return GrownShriekers.placeGrowth(level, position, growth, spreader, (at, state) -> original.call(level, at, state));
	}
}

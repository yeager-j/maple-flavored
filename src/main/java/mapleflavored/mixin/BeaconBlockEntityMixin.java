package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mapleflavored.alloys.AlloyBeacons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A lit beacon that counts a full Alloy pyramid credits But why? to the players near it. */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
	// Vanilla counts the levels every 80 ticks while the beam is unobstructed. Its Beaconator fires only when the beacon
	// turns on; checking every count also catches a pyramid finished under a beacon that is already lit.
	@WrapOperation(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;updateBase(Lnet/minecraft/world/level/Level;III)I")
	)
	private static int creditAlloyPyramid(
		Level level, int x, int y, int z, Operation<Integer> original, Level tickLevel, BlockPos pos, BlockState state, BeaconBlockEntity beacon
	) {
		int levels = original.call(level, x, y, z);

		if (!level.isClientSide()) AlloyBeacons.levelsCounted(level, pos, levels);

		return levels;
	}
}

package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mapleflavored.client.subspaces.WitherSubspaceLight;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dims what's drawn inside a Wither's Subspace. Blocks, fluids, block entities and particles all take their light from
 * here; a block's own glow is added afterwards, so it still glows.
 */
@Mixin(LightCoordsUtil.class)
public abstract class LightCoordsUtilMixin {
	@ModifyExpressionValue(
		method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;packedBrightness(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I")
	)
	private static int dimInsideWitherSubspace(int packedBrightness, LightCoordsUtil.BrightnessGetter getter, BlockAndLightGetter level, BlockState state, BlockPos pos) {
		return WitherSubspaceLight.packedLight(pos, packedBrightness);
	}
}

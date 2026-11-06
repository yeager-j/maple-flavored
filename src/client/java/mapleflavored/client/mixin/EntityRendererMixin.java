package mapleflavored.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.client.subspaces.WitherSubspaceLight;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dims entities drawn inside a Wither's Subspace. Renderers that make their entity glow, such as a Blaze's, override
 * these, and a burning entity's light stays full, so those still glow.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@ModifyReturnValue(method = "getSkyLightLevel", at = @At("RETURN"))
	private int dimSkyLight(int light, Entity entity, BlockPos position) {
		return WitherSubspaceLight.lightLevel(position, light);
	}

	@ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
	private int dimBlockLight(int light, Entity entity, BlockPos position) {
		return entity.isOnFire() ? light : WitherSubspaceLight.lightLevel(position, light);
	}
}

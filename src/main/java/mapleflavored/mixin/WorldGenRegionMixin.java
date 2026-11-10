package mapleflavored.mixin;

import mapleflavored.FreshEntities;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every entity a structure places as its chunk generates, such as ocean ruin drowned, comes through here before the chunk
 * saves it. The chunk saves a mob together with its passengers, so they come through at the same time. See
 * {@link FreshEntities}.
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void enterTheWorld(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		FreshEntities.entering(entity, ((WorldGenRegion) (Object) this).getLevel().dimension());
	}
}

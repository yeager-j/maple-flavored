package mapleflavored.mixin;

import mapleflavored.mobs.CommonMobs;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mobs a structure places as its chunk generates, such as ocean ruin drowned, take their common mob stats before the
 * chunk saves them. The chunk saves a mob together with its passengers, so they take theirs at the same time.
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void applyCommonMobStats(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		CommonMobs.applySpawnStats(entity);
	}
}

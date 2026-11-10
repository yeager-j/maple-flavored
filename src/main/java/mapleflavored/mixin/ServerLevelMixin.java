package mapleflavored.mixin;

import mapleflavored.FreshEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every new entity in a loaded level comes through here (natural spawning, spawners, trial spawners, spawn eggs,
 * infestation, conversion, commands, and structures placed straight into the level, as by {@code /place}), set up and
 * with its age decided. Entities loaded from disk or arriving through a portal take another path. Entities placed during
 * worldgen go through {@link WorldGenRegionMixin}. See {@link FreshEntities}.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void enterTheWorld(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		FreshEntities.entering(entity, ((ServerLevel) (Object) this).dimension());
	}
}

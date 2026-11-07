package mapleflavored.mixin;

import mapleflavored.bloodmoons.Restoring;
import mapleflavored.mobs.CommonMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Common mobs take their stats as they first enter the world. Every new mob in a loaded level comes through here
 * (natural spawning, spawners, trial spawners, spawn eggs, infestation, conversion and commands), set up and with its
 * age decided. Mobs loaded from disk or arriving through a portal take another path and keep the stats they spawned
 * with. Mobs placed during worldgen go through {@link WorldGenRegionMixin}.
 *
 * <p>A mob a Restorable structure places straight into a loaded level, as {@code /place} does, gets its Restore spot.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void applyCommonMobStats(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		CommonMobs.applySpawnStats(entity);
	}

	// Structures placed straight into a loaded level, as by /place, give their mobs Restore spots here.
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void tagRestoreSpot(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		Restoring.tagIfPlacing(entity, ((ServerLevel) (Object) this).dimension());
	}
}

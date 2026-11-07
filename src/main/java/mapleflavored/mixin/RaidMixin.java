package mapleflavored.mixin;

import mapleflavored.mobs.RaidWaves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the raiders a raid spawns for its waves. Vanilla joins a wave's new raiders, Ravager riders included, with
 * {@code exists} false, and recruits raiders already in the world with {@code exists} true.
 */
@Mixin(Raid.class)
public abstract class RaidMixin {
	@Inject(method = "joinRaid", at = @At("HEAD"))
	private void markWaveRaiders(ServerLevel level, int groupNumber, Raider raider, @Nullable BlockPos pos, boolean exists, CallbackInfo info) {
		if (!exists) RaidWaves.markSpawnedByWave(raider);
	}
}

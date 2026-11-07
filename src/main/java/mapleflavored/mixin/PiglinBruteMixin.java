package mapleflavored.mixin;

import mapleflavored.structurebooks.MobKeepers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Brute a Bastion's template places is tagged as one of its book's keepers when its piece places one. */
@Mixin(PiglinBrute.class)
public abstract class PiglinBruteMixin {
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void tagAsKeeper(
		ServerLevelAccessor level,
		DifficultyInstance difficulty,
		EntitySpawnReason spawnReason,
		@Nullable SpawnGroupData groupData,
		CallbackInfoReturnable<SpawnGroupData> callback
	) {
		if (spawnReason == EntitySpawnReason.STRUCTURE) MobKeepers.tagIfPlacing((PiglinBrute) (Object) this);
	}
}

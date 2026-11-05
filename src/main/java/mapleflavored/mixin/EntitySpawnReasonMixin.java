package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.EntitySpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Normal spawners ignore monsters' darkness rule, as trial spawners already do. */
@Mixin(EntitySpawnReason.class)
public abstract class EntitySpawnReasonMixin {
	@ModifyReturnValue(method = "ignoresLightRequirements", at = @At("RETURN"))
	private static boolean spawnersIgnoreLight(boolean ignoresLight, EntitySpawnReason reason) {
		return ignoresLight || reason == EntitySpawnReason.SPAWNER;
	}
}

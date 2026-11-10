package mapleflavored.mixin;

import java.util.Optional;
import mapleflavored.mobs.GoneForGood;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A saved entity that fails to load, as Peaceful refuses to load a hostile mob from its chunk, is dropped from the save,
 * so it's {@linkplain GoneForGood gone for good}. Every saved entity a chunk loads comes through here.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
	@Inject(
		method = "create(Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Ljava/util/Optional;",
		at = @At("RETURN")
	)
	private static void reportFailedLoad(
		ValueInput input,
		Level level,
		EntitySpawnRequest request,
		CallbackInfoReturnable<Optional<Entity>> callback
	) {
		if (callback.getReturnValue().isEmpty() && level instanceof ServerLevel serverLevel) GoneForGood.failedToLoad(serverLevel, input);
	}
}

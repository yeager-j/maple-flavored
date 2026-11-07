package mapleflavored.mixin;

import java.util.Optional;
import mapleflavored.bloodmoons.Restoring;
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
 * A saved mob that fails to load, as Peaceful refuses to load a hostile mob from its chunk, is dropped from the save for
 * good, so it gives up its Restore spot. Every saved entity a chunk loads comes through here.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
	@Inject(
		method = "create(Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Ljava/util/Optional;",
		at = @At("RETURN")
	)
	private static void dropRestoreSpot(
		ValueInput input,
		Level level,
		EntitySpawnRequest request,
		CallbackInfoReturnable<Optional<Entity>> callback
	) {
		if (callback.getReturnValue().isEmpty() && !level.isClientSide()) Restoring.dropIfOccupant(input);
	}
}

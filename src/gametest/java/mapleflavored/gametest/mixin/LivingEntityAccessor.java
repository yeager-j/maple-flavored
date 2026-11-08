package mapleflavored.gametest.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sets how long an entity has left using its item, which vanilla counts down in a tick mock players don't run. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	/** Sets the ticks left on the item in use, so a raised shield can be made to have finished its block delay. */
	@Accessor("useItemRemaining")
	void maple$setUseItemRemaining(int ticks);
}

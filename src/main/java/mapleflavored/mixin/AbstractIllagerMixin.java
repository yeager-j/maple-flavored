package mapleflavored.mixin;

import mapleflavored.structurebooks.IllagerAlert;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An Illager hunting the player who took its structure's book targets them ahead of anyone else. */
@Mixin(AbstractIllager.class)
public abstract class AbstractIllagerMixin extends Raider {
	private AbstractIllagerMixin(EntityType<? extends Raider> type, Level level) {
		super(type, level);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void huntWhoeverTookTheBook(CallbackInfo info) {
		targetSelector.addGoal(0, IllagerAlert.huntGoal((AbstractIllager) (Object) this));
	}
}

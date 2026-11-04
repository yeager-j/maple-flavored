package mapleflavored.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The grindstone gives no XP, so grinding enchanted gear can't turn lapis into XP.
 *
 * <p>Targets the anonymous result slot class in {@code GrindstoneMenu}, the fourth anonymous class it declares.
 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	@Inject(method = "getExperienceAmount", at = @At("HEAD"), cancellable = true)
	private void giveNoExperience(Level level, CallbackInfoReturnable<Integer> callback) {
		callback.setReturnValue(0);
	}
}

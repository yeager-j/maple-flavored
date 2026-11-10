package mapleflavored.mixin;

import mapleflavored.enchanting.Grinding;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The grindstone gives no XP, so grinding enchanted gear can't turn lapis into XP. Taking a stripped item hands back
 * its deposit.
 *
 * <p>Targets the anonymous result slot class in {@code GrindstoneMenu}, the fourth anonymous class it declares.
 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	// The compiler's reference to the enclosing menu, which the slot uses to read and clear the inputs.
	@Shadow
	@Final
	GrindstoneMenu this$0;

	@Inject(method = "getExperienceAmount", at = @At("HEAD"), cancellable = true)
	private void giveNoExperience(Level level, CallbackInfoReturnable<Integer> callback) {
		callback.setReturnValue(0);
	}

	// Runs before vanilla clears the inputs. The client runs onTake too, as a prediction, and gives nothing.
	@Inject(method = "onTake", at = @At("HEAD"))
	private void refundDeposit(Player player, ItemStack carried, CallbackInfo callback) {
		if (player.level().isClientSide()) return;

		ItemStack input = this$0.getSlot(GrindstoneMenu.INPUT_SLOT).getItem();
		ItemStack additional = this$0.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem();

		Grinding.handBackDeposit(player, input, additional);
	}
}

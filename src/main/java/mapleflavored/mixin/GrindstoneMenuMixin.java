package mapleflavored.mixin;

import mapleflavored.enchanting.AppleEffect;
import mapleflavored.enchanting.EnchantingDeposit;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The grindstone never merges two items into one; it only strips one item at a time, and turns an enchanted golden
 * apple back into a golden apple. A stripped item loses its deposit too, which the result slot hands back.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void neverMerge(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> callback) {
		if (!input.isEmpty() && !additional.isEmpty()) callback.setReturnValue(ItemStack.EMPTY);
	}

	// Vanilla grinds only enchanted items, and a returned value here skips removeDeposit, so this removes it too.
	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void grindEnchantedGoldenApple(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> callback) {
		// Only the top slot takes apples, so a lone apple is always the input.
		if (!input.is(Items.ENCHANTED_GOLDEN_APPLE) || !additional.isEmpty()) return;

		ItemStack plain = AppleEffect.plain(input);

		plain.remove(EnchantingDeposit.COMPONENT);
		callback.setReturnValue(plain);
	}

	// The result is always a fresh copy of the input, so removing the deposit leaves the input as it was.
	@Inject(method = "computeResult", at = @At("RETURN"))
	private void removeDeposit(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> callback) {
		callback.getReturnValue().remove(EnchantingDeposit.COMPONENT);
	}
}

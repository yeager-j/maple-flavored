package mapleflavored.mixin;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The grindstone never merges two items into one; it only strips one item at a time. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void neverMerge(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> callback) {
		if (!input.isEmpty() && !additional.isEmpty()) callback.setReturnValue(ItemStack.EMPTY);
	}
}

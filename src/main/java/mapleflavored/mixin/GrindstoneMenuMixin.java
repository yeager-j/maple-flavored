package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.enchanting.Grinding;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The grindstone gives what {@link Grinding} says: it never merges, and strips one item of its Deposit too. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@ModifyReturnValue(method = "computeResult", at = @At("RETURN"))
	private ItemStack grind(ItemStack vanillaResult, ItemStack input, ItemStack additional) {
		return Grinding.result(input, additional, vanillaResult);
	}
}

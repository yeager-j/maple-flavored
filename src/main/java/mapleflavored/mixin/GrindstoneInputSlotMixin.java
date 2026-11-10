package mapleflavored.mixin;

import mapleflavored.enchanting.Grinding;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The grindstone's input slots hold one item each, and the top one also takes enchanted golden apples, which have no
 * enchantments, so apples are ground one at a time like gear. Taking apples in the top slot only means shift-clicking
 * a stack moves a single apple, where vanilla's quick move would fill both slots and grind nothing.
 *
 * <p>Targets the anonymous input slot classes in {@code GrindstoneMenu}, the second and third anonymous classes it
 * declares.
 */
@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
abstract class GrindstoneInputSlotMixin extends Slot {
	private GrindstoneInputSlotMixin(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void acceptEnchantedGoldenApples(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
		if (getContainerSlot() == GrindstoneMenu.INPUT_SLOT && Grinding.stripsUnenchanted(stack)) callback.setReturnValue(true);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}

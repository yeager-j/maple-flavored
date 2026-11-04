package mapleflavored.mixin;

import mapleflavored.smithing.MaterialUpgrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Material upgrades make nothing from a base that {@link MaterialUpgrades} refuses. */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void refuseEnchantedGear(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> callback) {
		if (MaterialUpgrades.refuses(input.base())) callback.setReturnValue(ItemStack.EMPTY);
	}
}

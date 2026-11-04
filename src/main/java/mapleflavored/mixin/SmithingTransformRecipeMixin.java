package mapleflavored.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Material upgrades refuse enchanted gear, so an enchanting deposit paid in a cheap material can't be upgraded into a
 * better one. Trims are a different recipe type and still work on enchanted gear.
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void refuseEnchantedGear(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> callback) {
		if (EnchantmentHelper.hasAnyEnchantments(input.base())) callback.setReturnValue(ItemStack.EMPTY);
	}
}

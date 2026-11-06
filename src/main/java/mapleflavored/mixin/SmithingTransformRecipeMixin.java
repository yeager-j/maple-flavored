package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.smithing.MaterialUpgrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Material upgrades make nothing from a base that {@link MaterialUpgrades} refuses, and Alloy gear keeps the share of
 * durability its base had left.
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void refuseEnchantedGear(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> callback) {
		if (MaterialUpgrades.refuses(input.base())) callback.setReturnValue(ItemStack.EMPTY);
	}

	// Vanilla copies the base's damage as it is, which could break gear smithed into an Alloy with less durability.
	@ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private ItemStack keepAlloyGearWear(ItemStack result, SmithingRecipeInput input) {
		ItemStack base = input.base();

		if (AlloyGear.alloyOf(result).isEmpty() || !base.isDamageableItem() || !result.isDamageableItem()) return result;

		result.setDamageValue(AlloyGear.keptDamage(base.getDamageValue(), base.getMaxDamage(), result.getMaxDamage()));

		return result;
	}
}

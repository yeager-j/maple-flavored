package mapleflavored.smithing;

import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The rule for smithing material upgrades: they refuse enchanted gear, so an enchanting deposit paid in a cheap
 * material can't be upgraded into a better one, and an enchanted addition, such as an Elytra going into an Armored
 * Elytra, doesn't lose its enchantments. Trims don't change material, so they aren't upgrades.
 */
public final class MaterialUpgrades {
	private MaterialUpgrades() {
	}

	/** Whether a material upgrade refuses these inputs. */
	public static boolean refuses(SmithingRecipeInput input) {
		return EnchantmentHelper.hasAnyEnchantments(input.base()) || EnchantmentHelper.hasAnyEnchantments(input.addition());
	}
}

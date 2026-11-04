package mapleflavored.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The rule for smithing material upgrades: they refuse enchanted gear, so an enchanting deposit paid in a cheap
 * material can't be upgraded into a better one. Trims don't change material, so they aren't upgrades.
 */
public final class MaterialUpgrades {
	private MaterialUpgrades() {
	}

	/** Whether a material upgrade refuses this base item. */
	public static boolean refuses(ItemStack base) {
		return EnchantmentHelper.hasAnyEnchantments(base);
	}
}

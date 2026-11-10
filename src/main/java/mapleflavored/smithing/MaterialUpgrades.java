package mapleflavored.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The rule for smithing material upgrades: they refuse enchanted gear, so an enchanting deposit paid in a cheap
 * material can't be upgraded into a better one, and an enchanted addition, such as an Elytra going into an Armored
 * Elytra, doesn't lose its enchantments.
 */
public final class MaterialUpgrades {
	private MaterialUpgrades() {
	}

	/**
	 * A smithing recipe that is a material upgrade: it turns its base into an item of another material. Trims and
	 * Reinforcement don't change material, so they aren't upgrades. A material upgrade makes nothing only when it
	 * {@linkplain #refuses refuses} its inputs.
	 */
	public interface Upgrade {
	}

	/** Whether a material upgrade refuses these inputs. */
	public static boolean refuses(SmithingRecipeInput input) {
		return EnchantmentHelper.hasAnyEnchantments(input.base()) || EnchantmentHelper.hasAnyEnchantments(input.addition());
	}

	/**
	 * Whether a smithing table's matched recipe was a material upgrade that refused its inputs.
	 *
	 * @param made what the matched recipe assembled
	 */
	public static boolean refused(Recipe<?> matched, ItemStack made) {
		return matched instanceof Upgrade && made.isEmpty();
	}
}

package mapleflavored.enchanting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The grindstone's rule: it never merges two items into one. It only strips one item, turning an enchanted golden
 * apple back into a golden apple, and hands back the stripped item's Deposit.
 */
public final class Grinding {
	private Grinding() {
	}

	/** Whether the grindstone strips the item though it has no enchantments: an enchanted golden apple. */
	public static boolean stripsUnenchanted(ItemStack item) {
		return item.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	/**
	 * What the grindstone gives for its two inputs: nothing for two items, and otherwise the lone item stripped and
	 * without its Deposit.
	 *
	 * @param vanillaResult what vanilla gives for the same inputs, a new stack
	 */
	public static ItemStack result(ItemStack input, ItemStack additional, ItemStack vanillaResult) {
		if (!input.isEmpty() && !additional.isEmpty()) return ItemStack.EMPTY;

		ItemStack ground = ground(input, additional);
		ItemStack stripped = stripsUnenchanted(ground) ? AppleEffect.plain(ground) : vanillaResult;

		return EnchantingDeposit.without(stripped);
	}

	/** Hands the player the Deposit of the item the grindstone strips, as they take the result. */
	public static void handBackDeposit(Player player, ItemStack input, ItemStack additional) {
		EnchantingDeposit.handBack(player, ground(input, additional));
	}

	// The grindstone never merges, so whichever input isn't empty is the one it strips.
	private static ItemStack ground(ItemStack input, ItemStack additional) {
		return input.isEmpty() ? additional : input;
	}
}

package mapleflavored.loot;

import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Swaps items in every loot roll, from any table, for the items Maple Flavored hands out instead. A swapped stack
 * keeps its count, damage and enchantments.
 */
public final class LootSubstitutions {
	private static final Map<Item, Item> SUBSTITUTES = Map.of(
		Items.STONE_SWORD, Items.COPPER_SWORD,
		Items.STONE_SHOVEL, Items.COPPER_SHOVEL,
		Items.STONE_PICKAXE, Items.COPPER_PICKAXE,
		Items.STONE_AXE, Items.COPPER_AXE,
		Items.STONE_HOE, Items.COPPER_HOE,
		Items.STONE_SPEAR, Items.COPPER_SPEAR,
		// Enchanted golden apples come only from the Enchanting Table.
		Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE
	);

	private LootSubstitutions() {
	}

	/** Applies the substitutions to every loot roll from now on. Call once at mod initialization. */
	static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> drops.replaceAll(LootSubstitutions::substitute));
	}

	private static ItemStack substitute(ItemStack drop) {
		Item substitute = SUBSTITUTES.get(drop.getItem());

		return substitute == null ? drop : drop.transmuteCopy(substitute);
	}
}

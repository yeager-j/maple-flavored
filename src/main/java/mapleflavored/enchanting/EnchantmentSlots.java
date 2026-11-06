package mapleflavored.enchanting;

import mapleflavored.MapleFlavored;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Enchantment Slots: how many enchantments a player can save on an item, set by the player's level. */
public final class EnchantmentSlots {
	/** Gear that gets one slot more than its enchanter's level gives: gold and Electrum tools, weapons and armor. */
	public static final TagKey<Item> EXTRA_SLOT_GEAR = TagKey.create(Registries.ITEM, MapleFlavored.id("extra_enchantment_slot"));

	private static final int LEVELS_PER_SLOT = 30;

	private EnchantmentSlots() {
	}

	/** The slots a player at {@code playerLevel} has for {@code item}: one per 30 levels, with no cap, plus any extra slot. */
	public static int available(int playerLevel, ItemStack item) {
		int extraSlots = item.is(EXTRA_SLOT_GEAR) ? 1 : 0;

		return playerLevel / LEVELS_PER_SLOT + extraSlots;
	}
}

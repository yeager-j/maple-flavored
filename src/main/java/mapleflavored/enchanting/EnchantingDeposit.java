package mapleflavored.enchanting;

import mapleflavored.MapleFlavored;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * The Deposit: the one material unit an item pays on its first save at the Enchanting Table, from when it is due to
 * when it is handed back.
 *
 * <p>An item holds a Deposit only while it is enchanted. The table takes one when it first enchants a plain item, and
 * whatever strips an item must take the Deposit off it and hand it back, as {@link Grinding} does. Anything new that
 * strips enchantments has to do the same, or refuse enchanted items the way smithing material upgrades do.
 */
public final class EnchantingDeposit {
	// The exact unit paid, so oak planks come back as oak planks.
	private static final DataComponentType<ItemStackTemplate> COMPONENT = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MapleFlavored.id("enchanting_deposit"),
		DataComponentType.<ItemStackTemplate>builder()
			.persistent(ItemStackTemplate.CODEC)
			.networkSynchronized(ItemStackTemplate.STREAM_CODEC)
			.build()
	);

	private EnchantingDeposit() {
	}

	/** Registers the component. Call once from mod init. */
	static void register() {
	}

	/**
	 * Whether saving the item at the table takes a Deposit: it is plain, with no enchantments or golden apple effects,
	 * and holds no Deposit yet. Enchanted items that never paid one, such as loot, still pay none.
	 */
	public static boolean isDue(ItemStack item) {
		if (item.isEmpty() || item.has(COMPONENT)) return false;

		return !item.isEnchanted() && !item.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	/**
	 * Takes one unit from {@code material} and stores it on {@code item} as its Deposit.
	 *
	 * @throws IllegalStateException if the item already holds a Deposit, which paying would destroy
	 */
	public static void pay(ItemStack item, ItemStack material) {
		if (item.has(COMPONENT)) throw new IllegalStateException(item + " already holds a deposit");

		item.set(COMPONENT, ItemStackTemplate.fromNonEmptyStack(material, 1));
		material.shrink(1);
	}

	/** The Deposit one of the item holds, as a new stack, or empty if it holds none. Leaves the item unchanged. */
	public static ItemStack held(ItemStack item) {
		ItemStackTemplate deposit = item.get(COMPONENT);

		return deposit == null ? ItemStack.EMPTY : deposit.create();
	}

	/** A copy of the item holding no Deposit, for the stripped item whose Deposit is handed back. */
	public static ItemStack without(ItemStack item) {
		ItemStack stripped = item.copy();

		stripped.remove(COMPONENT);

		return stripped;
	}

	/** Gives the player the Deposit of every item in the stack. Leaves the stack unchanged. */
	public static void handBack(Player player, ItemStack items) {
		for (int item = 0; item < items.getCount(); item++) {
			ItemStack deposit = held(items);

			if (!deposit.isEmpty()) player.getInventory().placeItemBackInInventory(deposit, Prediction.SERVER_ONLY);
		}
	}
}

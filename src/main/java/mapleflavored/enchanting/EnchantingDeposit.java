package mapleflavored.enchanting;

import mapleflavored.MapleFlavored;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The deposit: the one material unit an item paid on its first save at the Enchanting Table. It stays on the item until
 * the grindstone strips the item and hands it back.
 */
public final class EnchantingDeposit {
	/** The deposit an item holds, as the exact unit paid, so oak planks come back as oak planks. */
	public static final DataComponentType<ItemStackTemplate> COMPONENT = Registry.register(
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

	/** Takes one unit from {@code material} and stores it on {@code item} as its deposit. */
	public static void pay(ItemStack item, ItemStack material) {
		item.set(COMPONENT, ItemStackTemplate.fromNonEmptyStack(material, 1));
		material.shrink(1);
	}

	/** The item's deposit as a new stack to hand back, or empty if it holds none. Leaves the item unchanged. */
	public static ItemStack refund(ItemStack item) {
		ItemStackTemplate deposit = item.get(COMPONENT);

		return deposit == null ? ItemStack.EMPTY : deposit.create();
	}
}

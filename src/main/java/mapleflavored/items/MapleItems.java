package mapleflavored.items;

import java.util.function.Function;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * The mod's item registry: every Maple Flavored item is registered through {@link #register}. It also holds the two
 * materials no single feature owns, Pith and the Diamond Shard. A feature's own items live in that feature's package.
 */
public final class MapleItems {
	/** The rare material behind Crystal Hearts and Netherite Ingots. */
	public static final Item PITH = register("pith", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));

	/** What diamond ore drops; 4 craft 1 diamond. */
	public static final Item DIAMOND_SHARD = register("diamond_shard", Item::new, new Item.Properties());

	private MapleItems() {
	}

	/** Puts Pith and the Diamond Shard in the creative tabs. Call once from mod init. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.insertAfter(Items.NETHER_STAR, PITH));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.insertBefore(Items.DIAMOND, DIAMOND_SHARD));
	}

	/**
	 * Registers an item as {@code maple_flavored:<name>}.
	 *
	 * @param factory builds the item from {@code properties}, which by then carry the key the item is registered under
	 */
	public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MapleFlavored.id(name));

		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}

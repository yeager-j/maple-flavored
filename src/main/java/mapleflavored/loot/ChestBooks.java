package mapleflavored.loot;

import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/**
 * The structure books that come with a chest's loot. Buried Treasure has no open room for a floating book, so its
 * treasure chest always gives one, Aqua Affinity or Respiration at even odds. The chest's loot seed is drawn when the
 * treasure generates, so the world seed still decides which.
 *
 * <p>Each book is a pool of its own, added after the table's vanilla pools, so the table's other loot is unchanged.
 * These are the only enchanted books a chest table keeps; see {@link LootEnchantments}.
 */
public final class ChestBooks {
	/** The enchantments a table's book is chosen from, by the vanilla table it joins. */
	private static final Map<ResourceKey<LootTable>, List<ResourceKey<Enchantment>>> BOOKS = Map.of(
		BuiltInLootTables.BURIED_TREASURE, List.of(Enchantments.AQUA_AFFINITY, Enchantments.RESPIRATION)
	);

	private ChestBooks() {
	}

	/** Adds each book to its table on every data load. Call once at mod initialization. */
	public static void register() {
		// Runs for data pack replacements too, so a replaced table still gives its book.
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			List<ResourceKey<Enchantment>> enchantments = BOOKS.get(key);

			if (enchantments != null) table.withPool(pool(enchantments, registries.lookupOrThrow(Registries.ENCHANTMENT)));
		});
	}

	/** Whether the table's own book can hold the enchantment. */
	static boolean gives(Holder<LootTable> table, Holder<Enchantment> enchantment) {
		return table.unwrapKey()
			.map(key -> BOOKS.getOrDefault(key, List.of()))
			.filter(enchantments -> enchantments.stream().anyMatch(enchantment::is))
			.isPresent();
	}

	// One book per roll, each enchantment an entry of equal weight, at level I.
	private static LootPool.Builder pool(List<ResourceKey<Enchantment>> enchantments, HolderLookup.RegistryLookup<Enchantment> registry) {
		LootPool.Builder pool = LootPool.lootPool();

		for (ResourceKey<Enchantment> enchantment : enchantments) {
			pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).apply(
				new SetEnchantmentsFunction.Builder().withEnchantment(registry.getOrThrow(enchantment), Holder.direct(new ConstantValue(1)))
			));
		}

		return pool;
	}
}

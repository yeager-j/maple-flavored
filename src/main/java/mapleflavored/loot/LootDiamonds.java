package mapleflavored.loot;

import java.util.Set;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Which diamond loot gives: safe, early sources give a Diamond Shard for each diamond, and dangerous structures keep
 * whole diamonds, so a diamond from loot is worth 4 ores only where it was earned.
 */
public final class LootDiamonds {
	/** The tables whose diamonds become shards, one for one. Every other table keeps whole diamonds. */
	private static final Set<ResourceKey<LootTable>> SHARD_TABLES = Set.of(
		BuiltInLootTables.BURIED_TREASURE,
		BuiltInLootTables.SHIPWRECK_TREASURE,
		BuiltInLootTables.DESERT_PYRAMID,
		BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY,
		BuiltInLootTables.ABANDONED_CAMP_SECRET_CHEST,
		BuiltInLootTables.ABANDONED_MINESHAFT,
		BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION,
		BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION_BARREL,
		BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_POT
	);

	private LootDiamonds() {
	}

	/** Swaps the diamonds in every shard table's rolls from now on. Call once at mod initialization. */
	static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!givesShards(table)) return;

			drops.replaceAll(drop -> drop.is(Items.DIAMOND) ? drop.transmuteCopy(MapleItems.DIAMOND_SHARD) : drop);
		});
	}

	/** The item {@code table} gives where its loot would give a diamond. */
	static Item diamondFor(Holder<LootTable> table) {
		return givesShards(table) ? MapleItems.DIAMOND_SHARD : Items.DIAMOND;
	}

	private static boolean givesShards(Holder<LootTable> table) {
		return table.unwrapKey().filter(SHARD_TABLES::contains).isPresent();
	}
}

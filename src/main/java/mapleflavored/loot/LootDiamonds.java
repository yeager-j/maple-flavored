package mapleflavored.loot;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Which diamond loot gives: safe, early sources give a Diamond Shard for each diamond, and dangerous structures keep
 * whole diamonds, so a diamond from loot is worth 4 ores only where it was earned.
 */
public final class LootDiamonds {
	/** The tables whose diamonds become shards, one for one. Every other table keeps whole diamonds. */
	private static final Set<ResourceKey<LootTable>> SHARD_TABLES = Stream.of(
		"chests/buried_treasure",
		"chests/shipwreck_treasure",
		"chests/desert_pyramid",
		"archaeology/desert_pyramid",
		"chests/abandoned_camp_secret_chest",
		"chests/abandoned_mineshaft",
		"chests/trial_chambers/intersection",
		"chests/trial_chambers/intersection_barrel",
		"pots/trial_chambers/corridor"
	).map(path -> ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path))).collect(Collectors.toUnmodifiableSet());

	private LootDiamonds() {
	}

	/** Swaps the diamonds in every shard table's rolls from now on. Call once at mod initialization. */
	public static void register() {
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

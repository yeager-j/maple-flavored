package mapleflavored.loot;

import java.util.List;
import java.util.Map;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

/**
 * Gives structure chests, and each ominous vault opening, their own rolls for Crystal Hearts and Pith. Every roll is a
 * pool of its own, added after the table's vanilla pools, so the table's other loot is unchanged.
 *
 * <p>Only tables left as vanilla's are listed. A table Maple ships its own file for declares its rolls there: the
 * Bastion's treasure, hoglin stable and other chests.
 */
public final class StructureChestRolls {
	/** One extra pool: 1 to {@code most} of {@code item}, at even odds per count, with probability {@code chance}. */
	private record Roll(Item item, float chance, int most) {
		static Roll one(Item item, float chance) {
			return new Roll(item, chance, 1);
		}

		LootPool.Builder pool() {
			UniformContainerBase.Builder<?> entry = LootItem.lootTableItem(this.item);

			if (this.most > 1) entry.apply(SetItemCountFunction.setCount(Holder.direct(new UniformGenerator(constant(1), constant(this.most)))));

			return LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(this.chance)).add(entry);
		}

		private static Holder<ContextIntProvider> constant(int value) {
			return Holder.direct(new ConstantValue(value));
		}
	}

	/** The extra rolls, by the vanilla table they join. */
	private static final Map<ResourceKey<LootTable>, List<Roll>> ROLLS = Map.ofEntries(
		Map.entry(BuiltInLootTables.ANCIENT_CITY, List.of(Roll.one(Hearts.CRYSTAL_HEART, 0.09f), Roll.one(MapleItems.PITH, 0.18f))),
		Map.entry(BuiltInLootTables.STRONGHOLD_CORRIDOR, List.of(Roll.one(Hearts.CRYSTAL_HEART, 0.125f), Roll.one(MapleItems.PITH, 0.06f))),
		Map.entry(BuiltInLootTables.BASTION_BRIDGE, List.of(Roll.one(MapleItems.PITH, 0.05f))),
		Map.entry(BuiltInLootTables.WOODLAND_MANSION, List.of(Roll.one(Hearts.CRYSTAL_HEART, 0.125f))),
		Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, List.of(Roll.one(MapleItems.PITH, 0.05f))),
		Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, List.of(Roll.one(MapleItems.PITH, 0.05f))),
		// The vault rolls this table once per opening.
		Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, List.of(Roll.one(MapleItems.PITH, 0.10f))),
		// End Cities make up for the enchanted gear their chests no longer give.
		Map.entry(BuiltInLootTables.END_CITY_TREASURE, List.of(
			Roll.one(Hearts.CRYSTAL_HEART, 0.15f),
			new Roll(MapleItems.PITH, 0.50f, 2)
		))
	);

	private StructureChestRolls() {
	}

	/** Adds the rolls to their tables on every data load. Call once at mod initialization. */
	public static void register() {
		LootTableEvents.MODIFY.register((key, table, source, registries) ->
			ROLLS.getOrDefault(key, List.of()).forEach(roll -> table.withPool(roll.pool()))
		);
	}
}

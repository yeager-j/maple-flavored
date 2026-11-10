package mapleflavored.gametest;

import static mapleflavored.gametest.LootRolls.assertRate;
import static mapleflavored.gametest.LootRolls.rollMany;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Some structures' chests roll for Crystal Hearts and Pith, each its own chance of one item, and the ominous vault
 * rolls for Pith once per opening. No other chest or vault table gives either.
 */
public class StructureChestGameTests {
	/** A table's chance of a Crystal Heart and of Pith, from the spec's chest table. */
	private record Odds(ResourceKey<LootTable> table, double crystalHeart, double pith) {
	}

	private static final List<Odds> ODDS = List.of(
		new Odds(BuiltInLootTables.ANCIENT_CITY, 0.09, 0.18),
		new Odds(BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.125, 0.06),
		new Odds(BuiltInLootTables.BASTION_TREASURE, 0.25, 0.05),
		new Odds(BuiltInLootTables.BASTION_OTHER, 0.0, 0.05),
		new Odds(BuiltInLootTables.BASTION_BRIDGE, 0.0, 0.05),
		new Odds(BuiltInLootTables.BASTION_HOGLIN_STABLE, 0.0, 0.05),
		new Odds(BuiltInLootTables.WOODLAND_MANSION, 0.125, 0.0),
		new Odds(BuiltInLootTables.SIMPLE_DUNGEON, 0.0, 0.05),
		new Odds(BuiltInLootTables.ABANDONED_MINESHAFT, 0.0, 0.05),
		new Odds(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 0.0, 0.10),
		new Odds(BuiltInLootTables.END_CITY_TREASURE, 0.15, 0.50)
	);
	private static final int ROLLS = 2000;
	private static final int SWEEP_ROLLS = 100;

	@GameTest(maxTicks = 400)
	public void eachListedChestRollsForCrystalHeartsAndPithAtItsOwnRate(GameTestHelper helper) {
		for (Odds odds : ODDS) {
			String name = odds.table().identifier().toString();
			List<List<ItemStack>> rolls = rollMany(helper, odds.table(), ROLLS);
			boolean endCity = odds.table().equals(BuiltInLootTables.END_CITY_TREASURE);

			assertRate(helper, name + " Crystal Hearts", rollsWith(rolls, Hearts.CRYSTAL_HEART), ROLLS, odds.crystalHeart());
			assertRate(helper, name + " Pith", rollsWith(rolls, MapleItems.PITH), ROLLS, odds.pith());
			assertCounts(helper, name + " Crystal Heart", rolls, Hearts.CRYSTAL_HEART, Set.of(1));
			// An End City success gives 1 or 2 Pith; every other is one item.
			assertCounts(helper, name + " Pith", rolls, MapleItems.PITH, endCity ? Set.of(1, 2) : Set.of(1));
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void noOtherChestOrVaultTableGivesCrystalHeartsOrPith(GameTestHelper helper) {
		Set<ResourceKey<LootTable>> rolling = Set.copyOf(ODDS.stream().map(Odds::table).toList());
		Set<ContextKeySet> types = Set.of(LootContextParamSets.CHEST, LootContextParamSets.VAULT);
		var tables = helper.getLevel().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElements().toList();
		int swept = 0;

		for (Holder.Reference<LootTable> table : tables) {
			if (rolling.contains(table.key()) || !types.contains(table.value().getParamSet())) continue;

			String name = table.key().identifier().toString();

			for (List<ItemStack> roll : rollMany(helper, table.key(), SWEEP_ROLLS)) {
				for (ItemStack drop : roll) {
					helper.assertFalse(drop.is(Hearts.CRYSTAL_HEART) || drop.is(MapleItems.PITH), name + " gave " + drop);
				}
			}

			swept++;
		}

		// The rest of vanilla's chest tables, plus the vault's own sub-tables.
		helper.assertTrue(swept > 50, "swept " + swept + " tables");
		helper.succeed();
	}

	private static int rollsWith(List<List<ItemStack>> rolls, Item item) {
		return (int) rolls.stream().filter(roll -> roll.stream().anyMatch(drop -> drop.is(item))).count();
	}

	/** Each roll that gives {@code item} gives one stack, and every count in {@code expected} turns up. */
	private static void assertCounts(GameTestHelper helper, String what, List<List<ItemStack>> rolls, Item item, Set<Integer> expected) {
		List<ItemStack> stacks = new ArrayList<>();

		rolls.forEach(roll -> roll.stream().filter(drop -> drop.is(item)).forEach(stacks::add));

		if (stacks.isEmpty()) return;

		helper.assertValueEqual(stacks.size(), rollsWith(rolls, item), what + " stacks per roll");
		helper.assertValueEqual(Set.copyOf(stacks.stream().map(ItemStack::getCount).toList()), expected, what + " counts");
	}
}

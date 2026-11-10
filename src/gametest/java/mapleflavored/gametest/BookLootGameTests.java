package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Loot never skips the Library: chest books became diamonds, every book left is one enchantment at level I and comes
 * from fishing, the ominous vault or Buried Treasure's own structure book, gear from chests, vaults, fishing and bartering is plain, and bartering lost its
 * Soul Speed books and iron boots.
 */
public class BookLootGameTests {
	private static final Set<ResourceKey<Enchantment>> FISHING_BOOKS = Set.of(Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE);
	private static final Set<ResourceKey<Enchantment>> OMINOUS_VAULT_BOOKS = Set.of(Enchantments.BREACH, Enchantments.DENSITY, Enchantments.WIND_BURST);
	// The one chest table with a structure book of its own.
	private static final Set<ResourceKey<Enchantment>> BURIED_TREASURE_BOOKS = Set.of(Enchantments.AQUA_AFFINITY, Enchantments.RESPIRATION);
	// Other chest tables, and barter tables, hand out no enchanted books at all.
	private static final Map<ContextKeySet, Set<ResourceKey<Enchantment>>> BOOKS_BY_TABLE_TYPE = Map.of(
		LootContextParamSets.CHEST, Set.of(),
		LootContextParamSets.PIGLIN_BARTER, Set.of(),
		LootContextParamSets.FISHING, FISHING_BOOKS
	);
	// The ominous vault's opening and the rare table it nests are chest tables, and the only ones with books.
	private static final Set<ResourceKey<LootTable>> OMINOUS_VAULT_TABLES = Set.of(
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE
	);
	private static final int SWEEP_ROLLS = 50;
	// A seeded rate must land within this many binomial standard deviations of the expected rate.
	private static final double RATE_TOLERANCE = 4.0;

	@GameTest(maxTicks = 400)
	public void noChestVaultFishingOrBarterTableGivesEnchantedGearOrStrayBooks(GameTestHelper helper) {
		var tables = helper.getLevel().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElements().toList();
		int swept = 0;

		for (Holder.Reference<LootTable> table : tables) {
			Set<ResourceKey<Enchantment>> allowedBooks = allowedBooks(table);

			if (allowedBooks == null) continue;

			String name = table.key().identifier().toString();
			RandomSource random = RandomSource.create(name.hashCode());

			for (int i = 0; i < SWEEP_ROLLS; i++) {
				for (ItemStack drop : LootRolls.roll(helper, table.key(), random)) {
					helper.assertFalse(drop.isEnchanted(), name + " gave enchanted " + drop);

					if (drop.is(Items.ENCHANTED_BOOK)) {
						ResourceKey<Enchantment> enchantment = onlyEnchantmentAtLevelOne(helper, drop, name);

						helper.assertTrue(allowedBooks.contains(enchantment), name + " gave a " + enchantment.identifier() + " book");
					}
				}
			}

			swept++;
		}

		// Every vanilla chest table, the vault's among them, plus the fishing and barter tables.
		helper.assertTrue(swept > 60, "swept " + swept + " tables");
		helper.succeed();
	}

	private static Set<ResourceKey<Enchantment>> allowedBooks(Holder.Reference<LootTable> table) {
		if (OMINOUS_VAULT_TABLES.contains(table.key())) return OMINOUS_VAULT_BOOKS;
		if (table.key().equals(BuiltInLootTables.BURIED_TREASURE)) return BURIED_TREASURE_BOOKS;

		return BOOKS_BY_TABLE_TYPE.get(table.value().getParamSet());
	}

	@GameTest
	public void aChestBookEntryGivesOneDiamond(GameTestHelper helper) {
		// The library's first pool has no diamond of its own, and its enchanted book entry has weight 10 of 52.
		Rolls library = rollMany(helper, BuiltInLootTables.STRONGHOLD_LIBRARY, 500);
		List<ItemStack> poolRolls = library.drops().stream().filter(drop -> !drop.is(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE)).toList();
		List<ItemStack> diamonds = library.of(Items.DIAMOND);

		helper.assertTrue(library.of(Items.ENCHANTED_BOOK).isEmpty(), "the library gave an enchanted book");
		helper.assertTrue(diamonds.stream().allMatch(diamond -> diamond.getCount() == 1), "every diamond stack is 1");
		assertRate(helper, "library diamonds", diamonds.size(), poolRolls.size(), 10.0 / 52.0);
		helper.succeed();
	}

	@GameTest
	public void theNormalVaultGivesDiamondsForBooksAndPlainGear(GameTestHelper helper) {
		int rolls = 1000;
		Rolls rare = rollMany(helper, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, rolls);

		helper.assertTrue(rare.of(Items.ENCHANTED_BOOK).isEmpty(), "the normal vault gave an enchanted book");
		// Two book entries of weight 2 out of 23, and no other diamonds.
		assertRate(helper, "normal vault diamonds", rare.of(Items.DIAMOND).size(), rolls, 4.0 / 23.0);
		assertSeenAndPlain(helper, rare, Items.BOW, Items.CROSSBOW, Items.IRON_AXE, Items.IRON_CHESTPLATE, Items.DIAMOND_AXE, Items.DIAMOND_CHESTPLATE);
		helper.succeed();
	}

	@GameTest
	public void theOminousVaultKeepsOnlyTheMaceBooks(GameTestHelper helper) {
		int rolls = 1000;
		Rolls rare = rollMany(helper, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE, rolls);
		List<ItemStack> books = rare.of(Items.ENCHANTED_BOOK);
		List<ItemStack> diamonds = rare.of(Items.DIAMOND);

		helper.assertValueEqual(enchantmentsOn(helper, books, "ominous vault"), OMINOUS_VAULT_BOOKS, "ominous vault books");
		// Breach or Density at weight 2 and Wind Burst at weight 2, out of 29.
		assertRate(helper, "ominous vault books", books.size(), rolls, 4.0 / 29.0);
		// The other book entry, weight 2, is now a diamond. The table has no other diamond item.
		helper.assertTrue(diamonds.stream().allMatch(diamond -> diamond.getCount() == 1), "every diamond stack is 1");
		assertRate(helper, "ominous vault diamonds", diamonds.size(), rolls, 2.0 / 29.0);
		assertSeenAndPlain(helper, rare, Items.CROSSBOW, Items.DIAMOND_AXE, Items.DIAMOND_CHESTPLATE);
		helper.succeed();
	}

	// Vanilla declares both as chest tables. A table that kept its books by its declared type would lose them here.
	@GameTest
	public void theOminousVaultTablesAreChestTables(GameTestHelper helper) {
		for (ResourceKey<LootTable> key : OMINOUS_VAULT_TABLES) {
			LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);

			helper.assertValueEqual(table.getParamSet(), LootContextParamSets.CHEST, key.identifier() + "'s type");
		}

		helper.succeed();
	}

	@GameTest
	public void aWholeOminousVaultOpeningKeepsItsMaceBooks(GameTestHelper helper) {
		Rolls openings = rollMany(helper, BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 300);
		Set<ResourceKey<Enchantment>> seen = enchantmentsOn(helper, openings.of(Items.ENCHANTED_BOOK), "ominous vault opening");

		helper.assertFalse(seen.isEmpty(), "an ominous vault opening gave a Mace book");
		helper.assertTrue(OMINOUS_VAULT_BOOKS.containsAll(seen), "ominous vault opening books " + seen);
		helper.succeed();
	}

	@GameTest
	public void fishingGivesOnlyLuckOfTheSeaAndLureBooksAndPlainGear(GameTestHelper helper) {
		int rolls = 600;
		Rolls treasure = rollMany(helper, BuiltInLootTables.FISHING_TREASURE, rolls);
		List<ItemStack> books = treasure.of(Items.ENCHANTED_BOOK);

		helper.assertValueEqual(enchantmentsOn(helper, books, "fishing"), FISHING_BOOKS, "fishing books");
		// One of six equal entries.
		assertRate(helper, "fishing books", books.size(), rolls, 1.0 / 6.0);
		assertSeenAndPlain(helper, treasure, Items.BOW, Items.FISHING_ROD);
		helper.succeed();
	}

	@GameTest
	public void barteringLosesSoulSpeedBooksAndIronBootsWithNoReplacement(GameTestHelper helper) {
		// Vanilla 26.3's barter outputs, less the Soul Speed book and Soul Speed iron boots.
		Set<Item> remaining = Set.of(
			Items.POTION, Items.SPLASH_POTION, Items.IRON_NUGGET, Items.ENDER_PEARL, Items.DRIED_GHAST, Items.STRING,
			Items.QUARTZ, Items.OBSIDIAN, Items.CRYING_OBSIDIAN, Items.FIRE_CHARGE, Items.LEATHER, Items.SOUL_SAND,
			Items.NETHER_BRICK, Items.SPECTRAL_ARROW, Items.GRAVEL, Items.BLACKSTONE
		);
		RandomSource random = RandomSource.create(BuiltInLootTables.PIGLIN_BARTERING.identifier().hashCode());
		Set<Item> seen = new HashSet<>();

		for (int i = 0; i < 2000; i++) {
			List<ItemStack> barter = LootRolls.roll(helper, BuiltInLootTables.PIGLIN_BARTERING, random);

			// A removed entry left behind as an empty outcome would give nothing.
			helper.assertValueEqual(barter.size(), 1, "stacks per barter");
			seen.add(barter.getFirst().getItem());
		}

		helper.assertValueEqual(seen, remaining, "barter outputs");
		helper.succeed();
	}

	@GameTest
	public void chestGearTurnsPlainButKeepsItsOtherData(GameTestHelper helper) {
		ResourceKey<LootTable> fixture = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("maple_flavored_gametest:named_stone_pickaxe_in_chest"));
		List<ItemStack> drops = LootRolls.roll(helper, fixture, RandomSource.create(0L));

		helper.assertValueEqual(drops.size(), 1, "drop count");

		ItemStack pickaxe = drops.getFirst();

		helper.assertValueEqual(pickaxe.getItem(), Items.COPPER_PICKAXE, "chest tool");
		helper.assertFalse(pickaxe.isEnchanted(), "the chest tool kept its enchantments");
		// The table leaves 75% of a stone pickaxe's 131 durability: floor(0.25 * 131) = 32 damage.
		helper.assertValueEqual(pickaxe.getDamageValue(), 32, "chest tool damage");
		helper.assertValueEqual(pickaxe.getHoverName().getString(), "Old Pick", "chest tool name");
		helper.succeed();
	}

	@GameTest
	public void aBookWithSeveralEnchantmentsKeepsOneAtLevelOne(GameTestHelper helper) {
		ResourceKey<LootTable> fixture = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("maple_flavored_gametest:multi_enchantment_book"));
		RandomSource random = RandomSource.create(0L);
		Set<ResourceKey<Enchantment>> kept = new HashSet<>();

		for (int i = 0; i < 40; i++) {
			List<ItemStack> drops = LootRolls.roll(helper, fixture, random);

			helper.assertValueEqual(drops.size(), 1, "drop count");
			kept.add(onlyEnchantmentAtLevelOne(helper, drops.getFirst(), "fixture book"));
		}

		// The fixture book holds Sharpness V and Smite III.
		helper.assertValueEqual(kept, Set.of(Enchantments.SHARPNESS, Enchantments.SMITE), "enchantments kept");
		helper.succeed();
	}

	/** Every drop from rolling one table many times with a seed of its own. */
	private record Rolls(List<ItemStack> drops) {
		List<ItemStack> of(Item item) {
			return this.drops.stream().filter(drop -> drop.is(item)).toList();
		}
	}

	private static Rolls rollMany(GameTestHelper helper, ResourceKey<LootTable> key, int rolls) {
		RandomSource random = RandomSource.create(key.identifier().hashCode());
		List<ItemStack> drops = new ArrayList<>();

		for (int i = 0; i < rolls; i++) {
			drops.addAll(LootRolls.roll(helper, key, random));
		}

		return new Rolls(drops);
	}

	private static ResourceKey<Enchantment> onlyEnchantmentAtLevelOne(GameTestHelper helper, ItemStack book, String source) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		helper.assertValueEqual(stored.size(), 1, source + " book enchantment count");

		Holder<Enchantment> enchantment = stored.keySet().iterator().next();

		helper.assertValueEqual(stored.getLevel(enchantment), 1, source + " book level");

		return enchantment.unwrapKey().orElseThrow();
	}

	private static Set<ResourceKey<Enchantment>> enchantmentsOn(GameTestHelper helper, List<ItemStack> books, String source) {
		Set<ResourceKey<Enchantment>> enchantments = new HashSet<>();

		books.forEach(book -> enchantments.add(onlyEnchantmentAtLevelOne(helper, book, source)));

		return enchantments;
	}

	private static void assertSeenAndPlain(GameTestHelper helper, Rolls rolls, Item... gear) {
		for (Item item : gear) {
			List<ItemStack> found = rolls.of(item);

			helper.assertFalse(found.isEmpty(), item + " was rolled");
			helper.assertTrue(found.stream().noneMatch(ItemStack::isEnchanted), item + " is plain");
		}
	}

	private static void assertRate(GameTestHelper helper, String what, int count, int trials, double expected) {
		double mean = trials * expected;
		double band = RATE_TOLERANCE * Math.sqrt(trials * expected * (1.0 - expected));

		helper.assertTrue(Math.abs(count - mean) <= band, what + ": " + count + " of " + trials + ", expected about " + Math.round(mean));
	}
}

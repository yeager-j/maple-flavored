package mapleflavored.loot;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Keeps loot from skipping the Library. Every loot roll, from any table, is rewritten by what its table keeps: an
 * enchanted book the table doesn't keep becomes 1 diamond (a Diamond Shard where {@link LootDiamonds} says so), a book
 * it keeps holds one enchantment at level I, and gear is plain unless the table keeps it as rolled.
 *
 * <p>This holds for vanilla's tables and any data pack's. Maple's own table files declare what they give after it.
 */
public final class LootEnchantments {
	private static final Predicate<Holder<Enchantment>> ANY_BOOK = enchantment -> true;
	private static final Predicate<Holder<Enchantment>> NO_BOOK = enchantment -> false;
	// The Mace's enchantments, which only the ominous vault gives.
	private static final Predicate<Holder<Enchantment>> MACE_BOOKS = enchantment ->
		enchantment.is(Enchantments.BREACH) || enchantment.is(Enchantments.DENSITY) || enchantment.is(Enchantments.WIND_BURST);

	// The ominous vault's opening and the rare table it nests are chest tables that keep the Mace's books.
	private static final Kept OMINOUS_VAULT = new Kept(MACE_BOOKS, Gear.PLAIN);
	private static final Map<ResourceKey<LootTable>, Kept> KEPT_BY_TABLE = Map.of(
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, OMINOUS_VAULT,
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE, OMINOUS_VAULT
	);
	private static final Map<ContextKeySet, Kept> KEPT_BY_TABLE_TYPE = Map.of(
		LootContextParamSets.CHEST, new Kept(NO_BOOK, Gear.PLAIN),
		LootContextParamSets.VAULT, new Kept(ANY_BOOK, Gear.PLAIN),
		LootContextParamSets.FISHING, new Kept(ANY_BOOK, Gear.PLAIN),
		LootContextParamSets.PIGLIN_BARTER, new Kept(ANY_BOOK, Gear.PLAIN)
	);
	// Mob equipment tables keep their gear because a mob's own gear drops plain instead (SpawnedGear), and block tables
	// because a block can drop what it stored.
	private static final Kept KEPT_ELSEWHERE = new Kept(ANY_BOOK, Gear.AS_ROLLED);

	private LootEnchantments() {
	}

	/** Applies the rules to every loot roll from now on. Call once at mod initialization. */
	public static void register() {
		// Nested tables run this again on their parent's pass, so every rule leaves an already-rewritten drop alone.
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			drops.replaceAll(drop -> rewrite(drop, table, context.getRandom()));
		});
	}

	/** A copy of {@code stack} without its enchantments, or {@code stack} itself when it has none. */
	static ItemStack plain(ItemStack stack) {
		if (!stack.isEnchanted()) return stack;

		ItemStack plain = stack.copy();

		EnchantmentHelper.setEnchantments(plain, ItemEnchantments.EMPTY);

		return plain;
	}

	private static ItemStack rewrite(ItemStack drop, Holder<LootTable> table, RandomSource random) {
		Kept kept = keptBy(table);

		if (drop.is(Items.ENCHANTED_BOOK)) return keptBook(drop, table, kept.books(), random);

		return kept.gear() == Gear.PLAIN ? plain(drop) : drop;
	}

	// A table named in KEPT_BY_TABLE follows its own row, whatever type it declares.
	private static Kept keptBy(Holder<LootTable> table) {
		return table.unwrapKey()
			.map(KEPT_BY_TABLE::get)
			.orElseGet(() -> KEPT_BY_TABLE_TYPE.getOrDefault(table.value().getParamSet(), KEPT_ELSEWHERE));
	}

	/**
	 * The book with one of the enchantments its table keeps, at level I, chosen at random when it holds several. A
	 * book holding none the table keeps becomes the table's diamond.
	 */
	private static ItemStack keptBook(ItemStack book, Holder<LootTable> table, Predicate<Holder<Enchantment>> keeps, RandomSource random) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		// A book without enchantments has nothing to rewrite.
		if (stored.isEmpty()) return book;

		// The stored map iterates in identity-hash order, so sort it for a seeded roll to pick the same one every run.
		List<Holder<Enchantment>> keepable = stored.keySet().stream().filter(keeps).sorted(Comparator.comparing(Holder::getRegisteredName)).toList();

		if (keepable.isEmpty()) return new ItemStack(LootDiamonds.diamondFor(table));
		if (stored.size() == 1 && stored.getLevel(keepable.getFirst()) == 1) return book;

		// Only a real choice draws from the roll's random, so other loot in the roll comes out as it would in vanilla.
		Holder<Enchantment> chosen = keepable.size() == 1 ? keepable.getFirst() : keepable.get(random.nextInt(keepable.size()));
		ItemEnchantments.Mutable levelOne = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		ItemStack rewritten = book.copy();

		levelOne.set(chosen, 1);
		rewritten.set(DataComponents.STORED_ENCHANTMENTS, levelOne.toImmutable());

		return rewritten;
	}

	private enum Gear {
		PLAIN,
		AS_ROLLED
	}

	/** What a table keeps of the enchantments it rolls: the books whose enchantment passes, and its gear's or not. */
	private record Kept(Predicate<Holder<Enchantment>> books, Gear gear) {
	}
}

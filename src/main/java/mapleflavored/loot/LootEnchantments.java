package mapleflavored.loot;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Keeps loot from skipping the Library. Every loot roll, from any table, is rewritten by the type the table declares:
 * a chest's enchanted book becomes 1 diamond (a Diamond Shard where {@link LootDiamonds} says so), every other enchanted book holds one enchantment at level I, and gear
 * from chests, vaults, fishing and bartering is plain.
 */
public final class LootEnchantments {
	// Mob equipment tables are left out: a mob's own gear drops plain instead (SpawnedGear). Block tables are left out
	// because a block can drop what it stored.
	private static final Set<ContextKeySet> PLAIN_GEAR_TABLES = Set.of(
		LootContextParamSets.CHEST,
		LootContextParamSets.VAULT,
		LootContextParamSets.FISHING,
		LootContextParamSets.PIGLIN_BARTER
	);

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
		ContextKeySet tableType = table.value().getParamSet();

		if (drop.is(Items.ENCHANTED_BOOK)) {
			return tableType == LootContextParamSets.CHEST ? new ItemStack(LootDiamonds.diamondFor(table)) : levelOne(drop, random);
		}

		return PLAIN_GEAR_TABLES.contains(tableType) ? plain(drop) : drop;
	}

	/** The book with one of its enchantments, at level I, chosen at random when it holds several. */
	private static ItemStack levelOne(ItemStack book, RandomSource random) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		if (stored.isEmpty()) return book;

		// The stored map iterates in identity-hash order, so sort it for a seeded roll to pick the same one every run.
		List<Holder<Enchantment>> enchantments = stored.keySet().stream().sorted(Comparator.comparing(Holder::getRegisteredName)).toList();

		if (enchantments.size() == 1 && stored.getLevel(enchantments.getFirst()) == 1) return book;

		// Only a real choice draws from the roll's random, so other loot in the roll comes out as it would in vanilla.
		Holder<Enchantment> kept = enchantments.size() == 1 ? enchantments.getFirst() : enchantments.get(random.nextInt(enchantments.size()));
		ItemEnchantments.Mutable levelOne = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		ItemStack rewritten = book.copy();

		levelOne.set(kept, 1);
		rewritten.set(DataComponents.STORED_ENCHANTMENTS, levelOne.toImmutable());

		return rewritten;
	}
}

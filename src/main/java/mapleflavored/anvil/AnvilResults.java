package mapleflavored.anvil;

import java.util.Optional;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * What the anvil makes. It repairs an item with its material unit for free, combines two matching books into the next
 * level, and renames, charging XP points. It never combines two items or puts a book on gear.
 */
public final class AnvilResults {
	private static final int RENAME_COST = 10;
	private static final int BOOK_COST_PER_LEVEL = 50;

	private AnvilResults() {
	}

	/**
	 * What the anvil makes.
	 *
	 * @param item the stack to take from the result slot
	 * @param xpCost the XP points taking it costs
	 * @param materialUsed how many items taking it uses up from the second slot, or 0 to use up the whole slot
	 */
	public record Result(ItemStack item, int xpCost, int materialUsed) {
	}

	/** Book combining's only input: a book holding exactly one enchantment. */
	private record SingleEnchantment(Holder<Enchantment> enchantment, int level) {
	}

	/**
	 * The result of the anvil's two slots and the typed name, or empty if the anvil makes nothing from them.
	 *
	 * @param name the name typed in the anvil; null or blank removes a custom name
	 * @param recipes the server's recipes, which set how many units a full repair takes
	 */
	public static Optional<Result> compute(ItemStack input, ItemStack addition, @Nullable String name, RecipeManager recipes) {
		if (input.isEmpty() || !EnchantmentHelper.canStoreEnchantments(input)) return Optional.empty();
		if (triesToEnchant(input, addition)) return Optional.empty();

		Optional<Result> work = addition.isEmpty() ? Optional.of(new Result(input.copy(), 0, 0)) : combine(input, addition, recipes);

		if (work.isEmpty()) return Optional.empty();

		ItemStack result = work.get().item();
		boolean renamed = applyName(result, input, name);

		if (addition.isEmpty() && !renamed) return Optional.empty();

		int xpCost = work.get().xpCost() + (renamed ? RENAME_COST : 0);

		return Optional.of(new Result(result, xpCost, work.get().materialUsed()));
	}

	/**
	 * Whether the slots try to enchant an item: an enchanted book or enchanted item offered to anything the Enchanting
	 * Table enchants. The anvil always refuses this, since only the table enchants. Needs no recipes, so it works on
	 * either side.
	 */
	public static boolean triesToEnchant(ItemStack input, ItemStack addition) {
		return EnchantmentHelper.hasAnyEnchantments(addition) && MaterialUnits.materialOf(input).isPresent();
	}

	private static Optional<Result> combine(ItemStack input, ItemStack addition, RecipeManager recipes) {
		if (input.is(Items.ENCHANTED_BOOK) && addition.is(Items.ENCHANTED_BOOK)) return combineBooks(input, addition);

		return repair(input, addition, recipes);
	}

	private static Optional<Result> combineBooks(ItemStack book, ItemStack otherBook) {
		Optional<SingleEnchantment> enchantment = singleEnchantment(book);

		if (enchantment.isEmpty() || !enchantment.equals(singleEnchantment(otherBook))) return Optional.empty();

		Holder<Enchantment> holder = enchantment.get().enchantment();
		int combinedLevel = enchantment.get().level() + 1;

		if (combinedLevel > holder.value().getMaxLevel()) return Optional.empty();

		ItemEnchantments.Mutable combined = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		ItemStack result = book.copy();

		combined.set(holder, combinedLevel);
		EnchantmentHelper.setEnchantments(result, combined.toImmutable());

		return Optional.of(new Result(result, BOOK_COST_PER_LEVEL * combinedLevel, 0));
	}

	private static Optional<SingleEnchantment> singleEnchantment(ItemStack book) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		if (stored.size() != 1) return Optional.empty();

		var entry = stored.entrySet().iterator().next();

		return Optional.of(new SingleEnchantment(entry.getKey(), entry.getIntValue()));
	}

	/** Each unit restores an equal share of the item's durability, so a full repair takes its full repair units. */
	private static Optional<Result> repair(ItemStack item, ItemStack material, RecipeManager recipes) {
		Optional<Ingredient> itemMaterial = MaterialUnits.materialOf(item);

		if (!item.isDamaged() || itemMaterial.isEmpty() || !itemMaterial.get().test(material)) return Optional.empty();

		int share = Math.ceilDiv(item.getMaxDamage(), MaterialUnits.fullRepairUnits(item, recipes));
		int unitsUsed = Math.min(material.getCount(), Math.ceilDiv(item.getDamageValue(), share));
		ItemStack repaired = item.copy();

		repaired.setDamageValue(Math.max(item.getDamageValue() - unitsUsed * share, 0));

		return Optional.of(new Result(repaired, 0, unitsUsed));
	}

	/** Gives the result the typed name, matching vanilla's rules, and returns whether that changed its name. */
	private static boolean applyName(ItemStack result, ItemStack input, @Nullable String name) {
		if (name != null && !StringUtil.isBlank(name)) {
			if (name.equals(input.getHoverName().getString())) return false;

			result.set(DataComponents.CUSTOM_NAME, Component.literal(name));

			return true;
		}

		if (!input.has(DataComponents.CUSTOM_NAME)) return false;

		result.remove(DataComponents.CUSTOM_NAME);

		return true;
	}
}

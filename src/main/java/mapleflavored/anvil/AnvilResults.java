package mapleflavored.anvil;

import java.util.Optional;
import mapleflavored.enchanting.Checklist;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * What the anvil does with its two slots. It repairs an item with its material unit for free, combines two matching
 * books into the next level, and renames, charging XP points. It never combines two items or puts a book on gear.
 */
public final class AnvilResults {
	private static final int RENAME_COST = 10;
	private static final int BOOK_COST_PER_LEVEL = 50;

	private AnvilResults() {
	}

	/** What the anvil does with its slots and the typed name. */
	public sealed interface Outcome {
	}

	/**
	 * The anvil makes an item.
	 *
	 * @param item the stack to take from the result slot
	 * @param xpCost the XP points taking it costs
	 * @param materialUsed how many items taking it uses up from the second slot
	 */
	public record Made(ItemStack item, int xpCost, int materialUsed) implements Outcome {
	}

	/** The anvil refuses what the slots ask of it, for a reason the player can be told. */
	public enum Refused implements Outcome {
		/** An enchanted book or item was offered to something the Enchanting Table enchants; only the table enchants. */
		ENCHANTS_AT_TABLE,
		/** A damaged item was offered something other than its material unit, such as another copy of itself. */
		WRONG_MATERIAL,
		/** Two enchanted books that don't hold the same single enchantment at the same level. */
		MISMATCHED_BOOKS,
		/** Two matching books already at their enchantment's highest level. */
		MAXIMUM_LEVEL
	}

	/** The slots ask nothing of the anvil: they are empty, or hold nothing it works on. */
	public enum Nothing implements Outcome {
		INSTANCE
	}

	/** Book combining's only input: a book holding exactly one enchantment. */
	private record SingleEnchantment(Holder<Enchantment> enchantment, int level) {
	}

	/**
	 * What the anvil does with its two slots and the typed name.
	 *
	 * @param name the name typed in the anvil; null or blank removes a custom name
	 * @param recipes the server's recipes, which set how many units a full repair takes
	 * @param enchantments the enchantments the Enchanting Table can put on items
	 */
	public static Outcome compute(
		ItemStack input, ItemStack addition, @Nullable String name, RecipeManager recipes, Registry<Enchantment> enchantments
	) {
		if (input.isEmpty() || !EnchantmentHelper.canStoreEnchantments(input)) return Nothing.INSTANCE;
		if (EnchantmentHelper.hasAnyEnchantments(addition) && Checklist.accepts(input, enchantments)) return Refused.ENCHANTS_AT_TABLE;

		Outcome work = addition.isEmpty() ? new Made(input.copy(), 0, 0) : combine(input, addition, recipes);

		if (!(work instanceof Made made)) return work;

		boolean renamed = applyName(made.item(), input, name);

		if (addition.isEmpty() && !renamed) return Nothing.INSTANCE;

		return new Made(made.item(), made.xpCost() + (renamed ? RENAME_COST : 0), made.materialUsed());
	}

	private static Outcome combine(ItemStack input, ItemStack addition, RecipeManager recipes) {
		if (input.is(Items.ENCHANTED_BOOK) && addition.is(Items.ENCHANTED_BOOK)) return combineBooks(input, addition);

		return repair(input, addition, recipes);
	}

	private static Outcome combineBooks(ItemStack book, ItemStack otherBook) {
		Optional<SingleEnchantment> enchantment = singleEnchantment(book);

		if (enchantment.isEmpty() || !enchantment.equals(singleEnchantment(otherBook))) return Refused.MISMATCHED_BOOKS;

		Holder<Enchantment> holder = enchantment.get().enchantment();
		int combinedLevel = enchantment.get().level() + 1;

		if (combinedLevel > holder.value().getMaxLevel()) return Refused.MAXIMUM_LEVEL;

		ItemEnchantments.Mutable combined = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		ItemStack result = book.copy();

		combined.set(holder, combinedLevel);
		EnchantmentHelper.setEnchantments(result, combined.toImmutable());

		return new Made(result, BOOK_COST_PER_LEVEL * combinedLevel, 1);
	}

	private static Optional<SingleEnchantment> singleEnchantment(ItemStack book) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		if (stored.size() != 1) return Optional.empty();

		var entry = stored.entrySet().iterator().next();

		return Optional.of(new SingleEnchantment(entry.getKey(), entry.getIntValue()));
	}

	/** Each unit restores an equal share of the item's durability, so a full repair takes its full repair units. */
	private static Outcome repair(ItemStack item, ItemStack material, RecipeManager recipes) {
		if (!item.isDamaged() || !MaterialUnits.hasMaterial(item)) return Nothing.INSTANCE;
		if (!MaterialUnits.isMaterialOf(item, material)) return Refused.WRONG_MATERIAL;

		int share = Math.ceilDiv(item.getMaxDamage(), MaterialUnits.fullRepairUnits(item, recipes));
		int unitsUsed = Math.min(material.getCount(), Math.ceilDiv(item.getDamageValue(), share));
		ItemStack repaired = item.copy();

		repaired.setDamageValue(Math.max(item.getDamageValue() - unitsUsed * share, 0));

		return new Made(repaired, 0, unitsUsed);
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

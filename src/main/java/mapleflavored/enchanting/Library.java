package mapleflavored.enchanting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

/**
 * A table's Library: the enchanted books in Chiseled Bookshelves at the positions vanilla checks for bookshelf power.
 * It is read live from the world each time it is needed; nothing about it is stored, and books are never used up.
 *
 * @param bookshelves the table's bookshelves of either kind, as vanilla counts them for power
 * @param highestLevels the highest level shelved for each enchantment
 */
public record Library(int bookshelves, Map<Holder<Enchantment>, Integer> highestLevels) {
	/** Bookshelves of either kind a table needs before it offers anything. */
	public static final int REQUIRED_BOOKSHELVES = 15;

	/** Reads the Library of the enchanting table at {@code tablePos}. */
	public static Library read(Level level, BlockPos tablePos) {
		int bookshelves = 0;
		Map<Holder<Enchantment>, Integer> highestLevels = new HashMap<>();

		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (!EnchantingTableBlock.isValidBookShelf(level, tablePos, offset)) continue;

			bookshelves++;

			if (level.getBlockEntity(tablePos.offset(offset)) instanceof ChiseledBookShelfBlockEntity shelf) {
				shelf.getItems().forEach(book -> addHighestLevels(book, highestLevels));
			}
		}

		return new Library(bookshelves, Map.copyOf(highestLevels));
	}

	/** Whether books in the shelf at {@code shelfPos} belong to some enchanting table's Library. */
	public static boolean holdsShelf(Level level, BlockPos shelfPos) {
		return EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().anyMatch(offset -> {
			BlockPos tablePos = shelfPos.subtract(offset);

			return level.getBlockState(tablePos).is(Blocks.ENCHANTING_TABLE) && EnchantingTableBlock.isValidBookShelf(level, tablePos, offset);
		});
	}

	/**
	 * What the table offers: each shelved enchantment at its highest shelved level, or nothing below 15 bookshelves.
	 * Mending is never offered yet; it waits for the Full Library rule.
	 */
	public Map<Holder<Enchantment>, Integer> offers() {
		if (bookshelves < REQUIRED_BOOKSHELVES) return Map.of();

		Map<Holder<Enchantment>, Integer> offers = new HashMap<>(highestLevels);

		offers.keySet().removeIf(enchantment -> enchantment.is(Enchantments.MENDING));

		return Map.copyOf(offers);
	}

	private static void addHighestLevels(ItemStack book, Map<Holder<Enchantment>, Integer> highestLevels) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		stored.entrySet().forEach(entry -> highestLevels.merge(entry.getKey(), entry.getIntValue(), Math::max));
	}
}

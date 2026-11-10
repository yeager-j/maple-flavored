package mapleflavored.enchanting;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
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
 * <p>A Library is Full when it holds every enchantment but Mending at its max level. Its {@link Progress} shows how
 * close it is, and unlocks golden apple effects along the way. Only a Full Library offers Mending.
 *
 * @param enchantments the enchantment registry, whose entries other than Mending make up a Full Library
 * @param bookshelves the table's bookshelves of either kind, as vanilla counts them for power
 * @param highestLevels the highest level shelved for each enchantment
 */
public record Library(Registry<Enchantment> enchantments, int bookshelves, Map<Holder<Enchantment>, Integer> highestLevels) {
	/** Bookshelves of either kind a table needs before it offers anything. */
	public static final int REQUIRED_BOOKSHELVES = 15;

	/**
	 * Library Progress: the levels shelved toward a Full Library, counting each enchantment once, at its highest
	 * shelved level up to its max.
	 *
	 * @param levels the levels shelved, at most {@code fullLevels}
	 * @param fullLevels the levels a Full Library holds
	 */
	public record Progress(int levels, int fullLevels) {
		/** The share of a Full Library, in percent, that is a Full Library. */
		public static final int FULL_PERCENT = 100;

		/** Whether every enchantment but Mending is shelved at its max level. */
		public boolean isFull() {
			return reached(FULL_PERCENT);
		}

		/** The share of a Full Library shelved, from 0 to 1. */
		public float fraction() {
			if (fullLevels == 0) return 1.0F;

			return Mth.clamp((float) levels / fullLevels, 0.0F, 1.0F);
		}

		/** The levels needed to reach {@code percent} of a Full Library, rounded up: 15% of 130 needs 20. */
		public int levelsFor(int percent) {
			return Mth.positiveCeilDiv(percent * fullLevels, 100);
		}

		/** Whether enough levels are shelved to reach {@code percent} of a Full Library. */
		public boolean reached(int percent) {
			return levels >= levelsFor(percent);
		}
	}

	/**
	 * What decides a Library's offers, and all of it the table's screen is sent: the bookshelves that power the table
	 * and the Library Progress its books give. Enough books unlock nothing while the table has too few bookshelves.
	 *
	 * @param bookshelves the table's bookshelves of either kind, as vanilla counts them for power
	 * @param progress the Library Progress
	 */
	public record Status(int bookshelves, Progress progress) {
		/** Whether the table has the bookshelves it needs before it offers anything. */
		public boolean isPowered() {
			return bookshelves >= REQUIRED_BOOKSHELVES;
		}

		/** Whether the table offers what {@code percent} of a Full Library unlocks. */
		public boolean unlocked(int percent) {
			return isPowered() && progress.reached(percent);
		}

		/** Whether the table offers the checkpoint's reward. */
		public boolean unlocked(LibraryCheckpoint checkpoint) {
			return unlocked(checkpoint.percent());
		}
	}

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

		return new Library(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT), bookshelves, Map.copyOf(highestLevels));
	}

	/** Whether books in the shelf at {@code shelfPos} belong to some enchanting table's Library. */
	public static boolean holdsShelf(Level level, BlockPos shelfPos) {
		return EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().anyMatch(offset -> {
			BlockPos tablePos = shelfPos.subtract(offset);

			return level.getBlockState(tablePos).is(Blocks.ENCHANTING_TABLE) && EnchantingTableBlock.isValidBookShelf(level, tablePos, offset);
		});
	}

	/** The levels a Full Library holds: the max levels of every enchantment but Mending. */
	public static int fullLevels(Registry<Enchantment> enchantments) {
		return enchantments.listElements()
			.filter(Library::countsTowardFull)
			.mapToInt(enchantment -> enchantment.value().getMaxLevel())
			.sum();
	}

	/** How close this Library is to Full. Bookshelf power doesn't matter; only the books do. */
	public Progress progress() {
		int levels = enchantments.listElements()
			.filter(Library::countsTowardFull)
			.mapToInt(enchantment -> Math.min(highestLevels.getOrDefault(enchantment, 0), enchantment.value().getMaxLevel()))
			.sum();

		return new Progress(levels, fullLevels(enchantments));
	}

	/** This Library's bookshelves and Library Progress, which decide what it offers. */
	public Status status() {
		return new Status(bookshelves, progress());
	}

	/**
	 * What the table offers: each shelved enchantment at its highest shelved level, or nothing below 15 bookshelves.
	 * Mending is offered at its max level while the Library is Full, and never because a Mending book is shelved.
	 */
	public Map<Holder<Enchantment>, Integer> offers() {
		Status status = status();

		if (!status.isPowered()) return Map.of();

		Map<Holder<Enchantment>, Integer> offers = new HashMap<>(highestLevels);
		Holder<Enchantment> mending = enchantments.getOrThrow(Enchantments.MENDING);

		offers.remove(mending);

		if (status.unlocked(LibraryCheckpoint.MENDING)) offers.put(mending, mending.value().getMaxLevel());

		return Map.copyOf(offers);
	}

	/**
	 * The effects the table offers for golden apples: each one whose share of a Full Library this Library has reached,
	 * or none below 15 bookshelves.
	 */
	public Set<AppleEffect> appleOffers() {
		Set<AppleEffect> offers = EnumSet.noneOf(AppleEffect.class);
		Status status = status();

		Arrays.stream(AppleEffect.values()).filter(effect -> status.unlocked(effect.percent())).forEach(offers::add);

		return offers;
	}

	private static boolean countsTowardFull(Holder<Enchantment> enchantment) {
		return !enchantment.is(Enchantments.MENDING);
	}

	private static void addHighestLevels(ItemStack book, Map<Holder<Enchantment>, Integer> highestLevels) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		stored.entrySet().forEach(entry -> highestLevels.merge(entry.getKey(), entry.getIntValue(), Math::max));
	}
}

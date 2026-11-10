package mapleflavored.structurebooks;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * How one structure gets its book: the enchantments it can hold, one chosen with equal weight, where the book goes
 * among its planned pieces and how it's placed there, and what of the structure locks it. A spot rule that finds no
 * spot leaves the structure without a book.
 */
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, LockRule lock) {
	StructureBookRule {
		if (enchantments.isEmpty()) throw new IllegalArgumentException("a structure book rule needs an enchantment");

		enchantments = List.copyOf(enchantments);
	}

	/** A rule whose book is never locked. */
	StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot) {
		this(enchantments, spot, LockRule.NONE);
	}

	/** One of the enchantments, with equal weight. */
	ResourceKey<Enchantment> chooseEnchantment(RandomSource random) {
		return enchantments.get(random.nextInt(enchantments.size()));
	}

	/** Picks a book's spot among a structure's planned pieces. */
	@FunctionalInterface
	interface SpotRule {
		Optional<BookSpot> spot(List<StructurePiece> pieces, Planning planning);
	}

	/** Finds what among a structure's planned pieces must all be done before its book can be taken. */
	@FunctionalInterface
	interface LockRule {
		/** Nothing locks the book. */
		LockRule NONE = (pieces, book, random) -> BookLocks.Keys.NONE;

		/**
		 * The keys that lock the book, none when nothing does.
		 *
		 * @param book the book's spot as planned
		 * @param random the book's planning random, so the same world seed picks the same keys
		 */
		BookLocks.Keys keys(List<StructurePiece> pieces, BlockPos book, RandomSource random);
	}

	/**
	 * What a spot rule can read while a structure is planned.
	 *
	 * @param terrain the terrain the structure is planned into
	 * @param templates the templates the structure's pieces are built from
	 * @param random seeded per structure, so the same world seed picks the same spot
	 */
	record Planning(TerrainPreview terrain, StructureTemplateManager templates, RandomSource random) {
	}
}

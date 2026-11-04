package mapleflavored.structurebooks;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * How one structure gets its book: the enchantments it can hold, one chosen with equal weight, where the book goes
 * among its planned pieces, and how its spot is cleared when built. A spot rule that finds no spot leaves the structure
 * without a book.
 */
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Clearance clearance) {
	StructureBookRule {
		if (enchantments.isEmpty()) throw new IllegalArgumentException("a structure book rule needs an enchantment");

		enchantments = List.copyOf(enchantments);
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

	/**
	 * What a spot rule can read while a structure is planned.
	 *
	 * @param terrain the terrain the structure is planned into
	 * @param templates the templates the structure's pieces are built from
	 * @param random seeded per structure, so the same world seed picks the same spot
	 */
	record Planning(TerrainPreview terrain, StructureTemplateManager templates, RandomSource random) {
	}

	/** What a book does to its spot when the spot isn't open as the chunk is built. */
	enum Clearance {
		/** Carves an open pocket around the book, sealing any liquid touching it. */
		POCKET,
		/** Takes the spot as it is: the book replaces what's there and is waterlogged in water. */
		NONE
	}
}

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
 * among its planned pieces, how it's placed there when built, and which of the structure's spawners lock it. A spot
 * rule that finds no spot leaves the structure without a book.
 */
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Placement placement, SpawnerLock lock) {
	StructureBookRule {
		if (enchantments.isEmpty()) throw new IllegalArgumentException("a structure book rule needs an enchantment");

		enchantments = List.copyOf(enchantments);
	}

	/** A rule whose book is never locked. */
	StructureBookRule(List<ResourceKey<Enchantment>> enchantments, SpotRule spot, Placement placement) {
		this(enchantments, spot, placement, SpawnerLock.NONE);
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

	/** Finds the spawners among a structure's planned pieces that must all be Cleared before its book can be taken. */
	@FunctionalInterface
	interface SpawnerLock {
		/** No spawner locks the book. */
		SpawnerLock NONE = pieces -> List.of();

		/** Where each locking spawner will be placed. Empty when nothing locks the book. */
		List<BlockPos> spawners(List<StructurePiece> pieces);
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

	/** How the book is put at its spot as the chunk is built. */
	enum Placement {
		/** A floating book in an open pocket carved around it, sealing any liquid touching it. */
		POCKET,
		/** A floating book that takes the spot as it is: it replaces what's there and is waterlogged in water. */
		OPEN,
		/** A book item in the container at the spot, left there by the container's loot when it's rolled. */
		IN_CHEST,
		/** Suspicious gravel at the spot that gives the book when brushed. */
		IN_SUSPICIOUS_GRAVEL
	}
}

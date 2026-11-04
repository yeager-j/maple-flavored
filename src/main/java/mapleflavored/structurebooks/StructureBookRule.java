package mapleflavored.structurebooks;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * How one structure gets its book: the enchantments it can hold, one chosen with equal weight, and where the book goes
 * among its planned pieces. A spot rule that finds no spot leaves the structure without a book.
 */
record StructureBookRule(List<ResourceKey<Enchantment>> enchantments, Function<List<StructurePiece>, Optional<BlockPos>> spot) {
	StructureBookRule {
		if (enchantments.isEmpty()) throw new IllegalArgumentException("a structure book rule needs an enchantment");

		enchantments = List.copyOf(enchantments);
	}

	/** One of the enchantments, with equal weight. */
	ResourceKey<Enchantment> chooseEnchantment(RandomSource random) {
		return enchantments.get(random.nextInt(enchantments.size()));
	}
}

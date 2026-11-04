package mapleflavored.structurebooks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;

/** Which structures hold a book, and where each one's book goes. */
public final class StructureBookRules {
	// Seeds the enchantment choice apart from the structure's own randomness, so adding a book changes no layout.
	private static final int ENCHANTMENT_SALT = 0x5B00C;

	private static final StructureBookRule MINESHAFT = new StructureBookRule(
		List.of(Enchantments.EFFICIENCY),
		StructureBookRules::mineshaftRoomCentre
	);

	private static final Map<ResourceKey<Structure>, StructureBookRule> RULES = Map.of(
		BuiltinStructures.MINESHAFT, MINESHAFT,
		BuiltinStructures.MINESHAFT_MESA, MINESHAFT
	);

	private StructureBookRules() {
	}

	/**
	 * The structure's planned pieces with its book piece added last, or the same pieces when the structure holds no
	 * book, its rule finds no spot, or the pieces are empty (a structure that failed to generate).
	 *
	 * @param seed the world seed
	 * @param chunkPos the chunk the structure starts in
	 */
	public static PiecesContainer withBook(Holder<Structure> structure, PiecesContainer pieces, long seed, ChunkPos chunkPos) {
		if (pieces.isEmpty()) return pieces;

		Optional<StructureBookRule> rule = structure.unwrapKey().map(RULES::get);

		if (rule.isEmpty()) return pieces;

		Optional<BlockPos> spot = rule.get().spot().apply(pieces.pieces());

		if (spot.isEmpty()) return pieces;

		ResourceKey<Enchantment> enchantment = rule.get().chooseEnchantment(enchantmentRandom(seed, chunkPos));
		StructureBookPiece book = new StructureBookPiece(spot.get(), enchantment);

		return new PiecesContainer(Stream.concat(pieces.pieces().stream(), Stream.of(book)).toList());
	}

	private static WorldgenRandom enchantmentRandom(long seed, ChunkPos chunkPos) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));

		random.setLargeFeatureWithSalt(seed, chunkPos.x(), chunkPos.z(), ENCHANTMENT_SALT);

		return random;
	}

	// The central room every corridor branches from, on its floor: the first layer the room carves out.
	private static Optional<BlockPos> mineshaftRoomCentre(List<StructurePiece> pieces) {
		return pieces.stream()
			.filter(MineshaftPieces.MineShaftRoom.class::isInstance)
			.findFirst()
			.map(room -> {
				BoundingBox box = room.getBoundingBox();
				BlockPos centre = box.getCenter();

				return new BlockPos(centre.getX(), box.minY() + 1, centre.getZ());
			});
	}
}

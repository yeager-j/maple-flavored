package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A structure's planned book, saved with the structure as one of its pieces. Its bounding box is the book's spot, and
 * the book is placed when the chunk holding that spot is built.
 */
public class StructureBookPiece extends StructurePiece {
	private static final String ENCHANTMENT = "Enchantment";
	private static final Codec<ResourceKey<Enchantment>> ENCHANTMENT_CODEC = ResourceKey.codec(Registries.ENCHANTMENT);
	private static final int POCKET_RADIUS = 1;

	private final ResourceKey<Enchantment> enchantment;

	/** A book at {@code spot} that gives {@code enchantment} at level I. */
	public StructureBookPiece(BlockPos spot, ResourceKey<Enchantment> enchantment) {
		super(StructureBooks.PIECE, 0, new BoundingBox(spot));
		this.enchantment = enchantment;
	}

	/** Loads a saved book piece. */
	public StructureBookPiece(CompoundTag tag) {
		super(StructureBooks.PIECE, tag);
		this.enchantment = tag.read(ENCHANTMENT, ENCHANTMENT_CODEC).orElseThrow();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.store(ENCHANTMENT, ENCHANTMENT_CODEC, enchantment);
	}

	@Override
	public void postProcess(
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator generator,
		RandomSource random,
		BoundingBox chunkBB,
		ChunkPos chunkPos,
		BlockPos referencePos
	) {
		BlockPos spot = new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());

		if (!chunkBB.isInside(spot)) return;

		Optional<? extends Holder<Enchantment>> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment);

		if (holder.isEmpty()) {
			MapleFlavored.LOGGER.warn("Skipping a structure book at {}: unknown enchantment {}", spot, enchantment.identifier());

			return;
		}

		// A cave or lake the planning preview couldn't see can still stop the structure building around the spot.
		if (!level.getBlockState(spot).isAir()) openPocket(level, spot, chunkBB);

		level.setBlock(spot, StructureBooks.BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);

		if (level.getBlockEntity(spot) instanceof StructureBookBlockEntity book) book.setEnchantment(holder.get());
	}

	// Carves the 3-block cube around the spot, first turning any water or lava touching it to stone, so the book is
	// never buried or flooded. Stays within the chunk being built.
	private static void openPocket(WorldGenLevel level, BlockPos spot, BoundingBox chunkBB) {
		BoundingBox pocket = new BoundingBox(spot).inflatedBy(POCKET_RADIUS);

		insideChunk(pocket.inflatedBy(1), chunkBB)
			.filter(pos -> level.getBlockState(pos).liquid())
			.forEach(pos -> level.setBlock(pos, sealFor(pos), Block.UPDATE_CLIENTS));
		insideChunk(pocket, chunkBB).forEach(pos -> level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
	}

	private static Stream<BlockPos> insideChunk(BoundingBox box, BoundingBox chunkBB) {
		return BlockPos.betweenClosedStream(box).filter(chunkBB::isInside);
	}

	private static BlockState sealFor(BlockPos pos) {
		return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
	}
}

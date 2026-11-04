package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.Optional;
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

		level.setBlock(spot, StructureBooks.BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);

		if (level.getBlockEntity(spot) instanceof StructureBookBlockEntity book) book.setEnchantment(holder.get());
	}
}

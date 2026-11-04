package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
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
import org.jspecify.annotations.Nullable;

/**
 * A structure's planned book, saved with the structure as its last piece and placed when the chunk holding its spot is
 * built.
 *
 * <p>The spot is kept relative to the piece the book sits in, its anchor, so the book follows that piece when it
 * settles onto the terrain as it's built. Shipwrecks, Ocean Ruins, Igloos and temples all move up or down then. Those
 * moves are vertical, so the book's chunk never changes. The piece's own bounding box is the spot as planned.
 */
public class StructureBookPiece extends StructurePiece {
	private static final String ENCHANTMENT = "Enchantment";
	private static final String ANCHOR = "Anchor";
	private static final String OFFSET = "Offset";
	private static final String POCKET = "Pocket";
	private static final Codec<ResourceKey<Enchantment>> ENCHANTMENT_CODEC = ResourceKey.codec(Registries.ENCHANTMENT);
	private static final int NO_ANCHOR = -1;
	private static final int POCKET_RADIUS = 1;

	private final ResourceKey<Enchantment> enchantment;
	private final int anchorIndex;
	private final BlockPos offset;
	private final StructureBookRule.Clearance clearance;
	// Linked from the structure's pieces whenever they're gathered, so it's never saved.
	private @Nullable StructurePiece anchor;

	/**
	 * A book at {@code spot} that gives {@code enchantment} at level I, following the piece at {@code anchorIndex} in
	 * the structure's pieces.
	 */
	StructureBookPiece(
		BlockPos spot,
		ResourceKey<Enchantment> enchantment,
		int anchorIndex,
		StructurePiece anchor,
		StructureBookRule.Clearance clearance
	) {
		super(StructureBooks.PIECE, 0, new BoundingBox(spot));
		this.enchantment = enchantment;
		this.anchorIndex = anchorIndex;
		this.offset = spot.subtract(originOf(anchor));
		this.clearance = clearance;
	}

	/** Loads a saved book piece. A book saved without an anchor stays at its saved spot. */
	public StructureBookPiece(CompoundTag tag) {
		super(StructureBooks.PIECE, tag);
		this.enchantment = tag.read(ENCHANTMENT, ENCHANTMENT_CODEC).orElseThrow();
		this.anchorIndex = tag.getIntOr(ANCHOR, NO_ANCHOR);
		this.offset = tag.read(OFFSET, BlockPos.CODEC).orElse(BlockPos.ZERO);
		this.clearance = tag.getBooleanOr(POCKET, true) ? StructureBookRule.Clearance.POCKET : StructureBookRule.Clearance.NONE;
	}

	/** Hands each book among a structure's pieces the piece it follows. Call whenever the pieces are gathered. */
	public static void linkAnchors(List<StructurePiece> pieces) {
		for (StructurePiece piece : pieces) {
			if (!(piece instanceof StructureBookPiece book)) continue;

			boolean hasAnchor = book.anchorIndex >= 0 && book.anchorIndex < pieces.size();

			book.anchor = hasAnchor ? pieces.get(book.anchorIndex) : null;
		}
	}

	/** Where the book is, following its anchor's latest move. */
	public BlockPos spot() {
		if (anchor == null) return new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());

		return originOf(anchor).offset(offset);
	}

	/** The enchantment the book gives at level I. */
	public ResourceKey<Enchantment> enchantment() {
		return enchantment;
	}

	// A piece's bounding box always matches where it was last built: code-built pieces move their box to the ground,
	// and template pieces recompute theirs from the position they place at, even an Igloo's temporary one.
	private static BlockPos originOf(StructurePiece piece) {
		BoundingBox box = piece.getBoundingBox();

		return new BlockPos(box.minX(), box.minY(), box.minZ());
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.store(ENCHANTMENT, ENCHANTMENT_CODEC, enchantment);
		tag.putInt(ANCHOR, anchorIndex);
		tag.store(OFFSET, BlockPos.CODEC, offset);
		tag.putBoolean(POCKET, clearance == StructureBookRule.Clearance.POCKET);
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
		BlockPos spot = spot();

		if (!chunkBB.isInside(spot)) return;

		Optional<? extends Holder<Enchantment>> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment);

		if (holder.isEmpty()) {
			MapleFlavored.LOGGER.warn("Skipping a structure book at {}: unknown enchantment {}", spot, enchantment.identifier());

			return;
		}

		// A cave or lake the planning preview couldn't see can still stop the structure building around the spot.
		if (clearance == StructureBookRule.Clearance.POCKET && !level.getBlockState(spot).isAir()) openPocket(level, spot, chunkBB);

		boolean inWater = level.getFluidState(spot).is(FluidTags.WATER);
		BlockState book = StructureBooks.BLOCK.defaultBlockState().setValue(StructureBookBlock.WATERLOGGED, inWater);

		level.setBlock(spot, book, Block.UPDATE_CLIENTS);

		if (level.getBlockEntity(spot) instanceof StructureBookBlockEntity bookEntity) bookEntity.setEnchantment(holder.get());
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

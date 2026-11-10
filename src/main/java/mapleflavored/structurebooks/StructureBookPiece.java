package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import mapleflavored.mixin.BaseContainerBlockEntityInvoker;
import mapleflavored.mixin.BrushableBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
	private static final String PLACEMENT = "Placement";
	private static final String LOCK = "Lock";
	// Saved before the piece saved its whole lock: a keeper lock's keys, without their kind.
	private static final String LEGACY_KEEPERS = "Keepers";
	// Saved before books could be placed other than floating: whether the book carved a pocket.
	private static final String LEGACY_POCKET = "Pocket";
	private static final Codec<ResourceKey<Enchantment>> ENCHANTMENT_CODEC = ResourceKey.codec(Registries.ENCHANTMENT);
	private static final int NO_ANCHOR = -1;
	private static final int POCKET_RADIUS = 1;

	private final ResourceKey<Enchantment> enchantment;
	private final int anchorIndex;
	private final BlockPos offset;
	private final StructureBookRule.Placement placement;
	private final BookLocks.Keys lock;
	private final List<BlockPos> keepers;
	// Linked from the structure's pieces whenever they're gathered, so it's never saved.
	private @Nullable StructurePiece anchor;

	/**
	 * A book at {@code spot} that gives {@code enchantment} at level I, following the piece at {@code anchorIndex} in
	 * the structure's pieces.
	 *
	 * @param lock what locks the book, {@link BookLocks.Keys#NONE} when nothing does
	 */
	StructureBookPiece(
		BlockPos spot,
		ResourceKey<Enchantment> enchantment,
		int anchorIndex,
		StructurePiece anchor,
		StructureBookRule.Placement placement,
		BookLocks.Keys lock
	) {
		super(StructureBooks.PIECE, 0, new BoundingBox(spot));
		this.enchantment = enchantment;
		this.anchorIndex = anchorIndex;
		this.offset = spot.subtract(originOf(anchor));
		this.placement = placement;
		this.lock = lock;
		this.keepers = lock.kind().isKeeper() ? lock.positions() : List.of();
	}

	/**
	 * A floating book at {@code spot}, as most structures' books are, following the piece at {@code anchorIndex} in the
	 * structure's pieces.
	 *
	 * @param lock what locks the book, {@link BookLocks.Keys#NONE} when nothing does
	 */
	public static StructureBookPiece floating(
		BlockPos spot,
		ResourceKey<Enchantment> enchantment,
		int anchorIndex,
		StructurePiece anchor,
		BookLocks.Keys lock
	) {
		return new StructureBookPiece(spot, enchantment, anchorIndex, anchor, StructureBookRule.Placement.OPEN, lock);
	}

	/** Loads a saved book piece. A book saved without an anchor stays at its saved spot. */
	public StructureBookPiece(CompoundTag tag) {
		super(StructureBooks.PIECE, tag);
		this.enchantment = tag.read(ENCHANTMENT, ENCHANTMENT_CODEC).orElseThrow();
		this.anchorIndex = tag.getIntOr(ANCHOR, NO_ANCHOR);
		this.offset = tag.read(OFFSET, BlockPos.CODEC).orElse(BlockPos.ZERO);
		this.placement = tag.getString(PLACEMENT)
			.map(StructureBookRule.Placement::valueOf)
			.orElseGet(() -> tag.getBooleanOr(LEGACY_POCKET, true) ? StructureBookRule.Placement.POCKET : StructureBookRule.Placement.OPEN);
		this.lock = tag.read(LOCK, BookLocks.Keys.CODEC).orElse(BookLocks.Keys.NONE);
		this.keepers = lock.kind().isKeeper() ? lock.positions() : tag.read(LEGACY_KEEPERS, BlockPos.CODEC.listOf()).orElse(List.of());
	}

	/** The structure's book among its pieces, if it holds one. */
	public static Optional<StructureBookPiece> among(List<StructurePiece> pieces) {
		return pieces.stream().filter(StructureBookPiece.class::isInstance).map(StructureBookPiece.class::cast).findFirst();
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
		if (anchor == null) return plannedSpot();

		return originOf(anchor).offset(offset);
	}

	/** Where the book was planned, before its anchor settled. A book's lock is found by this spot. */
	public BlockPos plannedSpot() {
		return new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());
	}

	/** The enchantment the book gives at level I. */
	public ResourceKey<Enchantment> enchantment() {
		return enchantment;
	}

	/**
	 * What locks the book, as planned with its structure: {@link BookLocks.Keys#NONE} when nothing does, or when the piece
	 * was saved before it saved its lock.
	 */
	public BookLocks.Keys lock() {
		return lock;
	}

	/**
	 * The origins of the pieces that place the keepers locking the book: Brutes for a Bastion, sculk patches for an
	 * Ancient City, Evoker rooms for a Stronghold. None when keepers don't lock it.
	 */
	public List<BlockPos> keepers() {
		return keepers;
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
		tag.putString(PLACEMENT, placement.name());

		if (!lock.positions().isEmpty()) {
			tag.store(LOCK, BookLocks.Keys.CODEC, lock);
		} else if (!keepers.isEmpty()) {
			tag.store(LEGACY_KEEPERS, BlockPos.CODEC.listOf(), keepers);
		}
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

		switch (placement) {
			case POCKET -> {
				// A cave or lake the planning preview couldn't see can still stop the structure building around the spot.
				if (!level.getBlockState(spot).isAir()) openPocket(level, spot, chunkBB);

				placeFloatingBook(level, spot, holder.get());
			}
			case OPEN -> placeFloatingBook(level, spot, holder.get());
			case IN_CHEST -> putInChest(level, spot, holder.get());
			case IN_SUSPICIOUS_GRAVEL -> placeSuspiciousGravel(level, spot, holder.get());
		}
	}

	private static void placeFloatingBook(WorldGenLevel level, BlockPos spot, Holder<Enchantment> enchantment) {
		boolean inWater = level.getFluidState(spot).is(FluidTags.WATER);
		BlockState book = StructureBooks.BLOCK.defaultBlockState().setValue(StructureBookBlock.WATERLOGGED, inWater);

		level.setBlock(spot, book, Block.UPDATE_CLIENTS);

		if (level.getBlockEntity(spot) instanceof StructureBookBlockEntity bookEntity) bookEntity.setEnchantment(enchantment);
	}

	// Written straight into the slots, since setItem would roll the chest's loot now. The roll fills only empty slots.
	// With no chest at the spot, as when Buried Treasure finds no stone to rest on, there's no book either.
	private static void putInChest(WorldGenLevel level, BlockPos spot, Holder<Enchantment> enchantment) {
		if (!(level.getBlockEntity(spot) instanceof ChestBlockEntity chest)) return;

		NonNullList<ItemStack> slots = ((BaseContainerBlockEntityInvoker) chest).items();

		slots.set(slots.size() / 2, StructureBooks.levelOneBook(enchantment));
		chest.setChanged();
	}

	// Replaces whatever the structure's archaeology left at the spot, even its own suspicious gravel.
	private static void placeSuspiciousGravel(WorldGenLevel level, BlockPos spot, Holder<Enchantment> enchantment) {
		level.setBlock(spot, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);

		if (!(level.getBlockEntity(spot) instanceof BrushableBlockEntity gravel)) return;

		BrushableBlockEntityAccessor contents = (BrushableBlockEntityAccessor) gravel;

		contents.setLootTable(null);
		contents.setItem(StructureBooks.levelOneBook(enchantment));
		gravel.setChanged();
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

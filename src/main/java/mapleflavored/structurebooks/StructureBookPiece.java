package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mapleflavored.MapleFlavored;
import mapleflavored.access.BrushableBlockEntityAccessor;
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
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jspecify.annotations.Nullable;

/**
 * A structure's planned book, saved with the structure as its last piece and placed when the chunk holding its spot is
 * built. {@link #among} finds it.
 *
 * <p>The spot is kept relative to the piece the book sits in, its anchor, so the book follows that piece when it
 * settles onto the terrain as it's built. Shipwrecks, Ocean Ruins, Igloos and temples all move up or down then. Those
 * moves are vertical, so the book's chunk never changes. The piece's own bounding box is the spot as planned.
 *
 * <p>A book with a lock is the exception: it stays where it was planned, since its lock, its keepers and its structure
 * all find it by that spot.
 */
public class StructureBookPiece extends StructurePiece {
	private static final String ENCHANTMENT = "Enchantment";
	private static final String ANCHOR = "Anchor";
	private static final String OFFSET = "Offset";
	private static final String PLACEMENT = "Placement";
	private static final String LOCK = "Lock";
	private static final Codec<ResourceKey<Enchantment>> ENCHANTMENT_CODEC = ResourceKey.codec(Registries.ENCHANTMENT);
	private static final int POCKET_RADIUS = 1;

	private final ResourceKey<Enchantment> enchantment;
	private final int anchorIndex;
	private final BlockPos offset;
	private final BookSpot.Placement placement;
	private final Optional<BookLocks.Keys> lock;
	// Linked from the structure's pieces whenever they're gathered, so it's never saved.
	private @Nullable StructurePiece anchor;

	/**
	 * A book at {@code spot} that gives {@code enchantment} at level I. Add it after {@code pieces}, as the structure's
	 * last piece.
	 *
	 * @param pieces the structure's planned pieces, which hold the spot's anchor
	 * @param lock what locks the book, or empty when nothing does
	 * @throws IllegalArgumentException if the spot's anchor isn't one of the pieces
	 */
	public StructureBookPiece(List<StructurePiece> pieces, BookSpot spot, ResourceKey<Enchantment> enchantment, Optional<BookLocks.Keys> lock) {
		super(StructureBooks.PIECE, 0, new BoundingBox(spot.pos()));

		int anchorIndex = pieces.indexOf(spot.anchor());

		if (anchorIndex < 0) throw new IllegalArgumentException("the book's anchor isn't one of its structure's pieces");

		this.enchantment = enchantment;
		this.anchorIndex = anchorIndex;
		this.offset = spot.pos().subtract(originOf(spot.anchor()));
		this.placement = spot.placement();
		this.lock = lock;
	}

	/** Loads a saved book piece. A book nothing locks saves no lock. */
	public StructureBookPiece(CompoundTag tag) {
		super(StructureBooks.PIECE, tag);
		this.enchantment = tag.read(ENCHANTMENT, ENCHANTMENT_CODEC).orElseThrow();
		this.anchorIndex = tag.getInt(ANCHOR).orElseThrow();
		this.offset = tag.read(OFFSET, BlockPos.CODEC).orElseThrow();
		this.placement = tag.read(PLACEMENT, BookSpot.Placement.CODEC).orElseThrow();
		this.lock = tag.read(LOCK, BookLocks.Keys.CODEC);
	}

	/** The structure's book among its pieces, if it holds one. */
	// A book is added as its structure's last piece, so the search starts there.
	public static Optional<StructureBookPiece> among(List<StructurePiece> pieces) {
		return pieces.reversed().stream().filter(StructureBookPiece.class::isInstance).map(StructureBookPiece.class::cast).findFirst();
	}

	/** Hands the book among a structure's pieces the piece it follows. Call whenever the pieces are gathered. */
	public static void linkAnchors(List<StructurePiece> pieces) {
		among(pieces).ifPresent(book -> {
			boolean hasAnchor = book.anchorIndex >= 0 && book.anchorIndex < pieces.size();

			book.anchor = hasAnchor ? pieces.get(book.anchorIndex) : null;
		});
	}

	/**
	 * Where the book is placed. The spot of a book with a lock is where it was planned and never changes.
	 *
	 * <p>A book without one follows its anchor, so its spot is where it was planned until the anchor's part of the book's
	 * chunk is built, and the book's own block from then on, for as long as the structure stays in memory. A structure
	 * loaded again from its chunk only says where the book is if its anchor saves where it settled. An Igloo doesn't,
	 * so its book's spot reads as planned again. Building always sees the anchor settled.
	 */
	public BlockPos spot() {
		if (anchor == null || lock.isPresent()) return plannedSpot();

		return originOf(anchor).offset(offset);
	}

	/**
	 * Where the book was planned, before its anchor settled. This never changes, so it's what names the book to its
	 * lock and its keepers.
	 */
	public BlockPos plannedSpot() {
		return new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());
	}

	/** The enchantment the book gives at level I. */
	public ResourceKey<Enchantment> enchantment() {
		return enchantment;
	}

	/**
	 * What locks the book, as planned with its structure, or empty when nothing does. Keys with no positions mean the
	 * structure looked for keys and found none, which leaves the book free to take.
	 */
	public Optional<BookLocks.Keys> lock() {
		return lock;
	}

	/**
	 * The origins of the pieces that place the keepers locking the book: Brutes for a Bastion, sculk patches for an
	 * Ancient City, Evoker rooms for a Stronghold. None when keepers don't lock it.
	 */
	public List<BlockPos> keepers() {
		return lock.filter(keys -> keys.kind().isKeeper()).map(BookLocks.Keys::positions).orElse(List.of());
	}

	/** The key in the book's lock of the keeper mob {@code piece} places, if it places one of the book's keepers. */
	Optional<BlockPos> keeperMobPlacedBy(StructurePiece piece) {
		return keeperPlacedBy(piece, lock.filter(keys -> keys.kind().form() == BookLocks.KeyForm.KEEPER_MOB));
	}

	/** The key in the book's lock of the keeper shrieker {@code piece} places, if it places one of the book's keepers. */
	Optional<BlockPos> keeperShriekerPlacedBy(StructurePiece piece) {
		return keeperPlacedBy(piece, lock.filter(keys -> keys.kind() == BookLocks.Kind.SHRIEKERS));
	}

	private static Optional<BlockPos> keeperPlacedBy(StructurePiece piece, Optional<BookLocks.Keys> keepers) {
		return keepers.flatMap(keys -> keyGivenBy(keys.kind(), piece).filter(keys.positions()::contains));
	}

	// The key a piece gives the keeper it places, for each kind of keeper a piece can place.
	private static Optional<BlockPos> keyGivenBy(BookLocks.Kind kind, StructurePiece piece) {
		return switch (kind) {
			case BRUTES -> StructureBookSpots.bastionBruteKey(piece);
			case SHRIEKERS -> ShriekerKeepers.patchOrigin(piece);
			case EVOKERS -> Optional.of(MobKeepers.roomKey(piece));
			case SPAWNERS -> Optional.empty();
		};
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
		tag.store(PLACEMENT, BookSpot.Placement.CODEC, placement);

		lock.ifPresent(keys -> tag.store(LOCK, BookLocks.Keys.CODEC, keys));
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

		if (anchor != null && !originOf(anchor).offset(offset).equals(spot)) {
			MapleFlavored.LOGGER.error("The locked structure book at {} sits in a piece that settled, so it was left behind", spot);
		}

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
			case IN_SUSPICIOUS_GRAVEL -> placeSuspiciousGravel(level, spot, holder.get());
		}
	}

	private static void placeFloatingBook(WorldGenLevel level, BlockPos spot, Holder<Enchantment> enchantment) {
		boolean inWater = level.getFluidState(spot).is(FluidTags.WATER);
		BlockState book = StructureBooks.BLOCK.defaultBlockState().setValue(StructureBookBlock.WATERLOGGED, inWater);

		level.setBlock(spot, book, Block.UPDATE_CLIENTS);

		if (level.getBlockEntity(spot) instanceof StructureBookBlockEntity bookEntity) bookEntity.setEnchantment(enchantment);
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

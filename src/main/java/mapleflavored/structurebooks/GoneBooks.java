package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mapleflavored.MapleFlavored;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Whether a structure's floating book is gone from the spot its structure names. The block says: a book is in place
 * while its block is there, and gone once it isn't, whether it was taken, broken, replaced or never placed.
 *
 * <p>The block can only be read while its chunk is loaded, so what it says is remembered here for when it isn't, as
 * when a player stands at an Ancient City's edge: a book is remembered gone the moment its block leaves a loaded chunk,
 * and each answer read from the block is remembered too. A book whose chunk has never been seen loaded is in place.
 */
public final class GoneBooks extends SavedData {
	private static final Codec<GoneBooks> CODEC = BlockPos.CODEC.listOf().xmap(GoneBooks::new, GoneBooks::books);

	/** The level saved data that remembers the books found gone. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<GoneBooks> TYPE = new SavedDataType<>(MapleFlavored.id("gone_books"), GoneBooks::new, CODEC, null);

	private final Set<BlockPos> books = new HashSet<>();

	private GoneBooks() {
	}

	private GoneBooks(List<BlockPos> books) {
		this.books.addAll(books);
	}

	/**
	 * Whether the structure book that belongs at {@code spot} is gone. Reads the block when its chunk is loaded and
	 * fully generated, and otherwise answers as the block last did. Never loads the chunk.
	 */
	public static boolean isGone(ServerLevel level, BlockPos spot) {
		if (!BookLocks.isChunkBuilt(level, spot)) {
			GoneBooks remembered = level.getDataStorage().get(TYPE);

			return remembered != null && remembered.books.contains(spot);
		}

		boolean gone = !level.getBlockState(spot).is(StructureBooks.BLOCK);

		remember(level, spot, gone);

		return gone;
	}

	/**
	 * Remembers a structure book gone when its block leaves {@code pos}, by any means. Call on every block change in a
	 * loaded chunk.
	 *
	 * @param oldState the block that was at {@code pos}
	 */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState) {
		if (!oldState.is(StructureBooks.BLOCK) || level.getBlockState(pos).is(StructureBooks.BLOCK)) return;

		remember(level, pos, true);
	}

	private static void remember(ServerLevel level, BlockPos spot, boolean gone) {
		GoneBooks remembered = gone ? level.getDataStorage().computeIfAbsent(TYPE) : level.getDataStorage().get(TYPE);

		if (remembered == null) return;

		boolean changed = gone ? remembered.books.add(spot.immutable()) : remembered.books.remove(spot);

		if (changed) remembered.setDirty();
	}

	private List<BlockPos> books() {
		return List.copyOf(books);
	}
}

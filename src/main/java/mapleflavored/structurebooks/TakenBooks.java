package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mapleflavored.MapleFlavored;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every structure book taken in a level, by where it was. Read whether or not the book's chunk is loaded, as the Ancient
 * City does to know whether its book is still in place.
 */
public final class TakenBooks extends SavedData {
	private static final Codec<TakenBooks> CODEC = BlockPos.CODEC.listOf().xmap(TakenBooks::new, TakenBooks::books);

	/** The level saved data that holds the taken books. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<TakenBooks> TYPE = new SavedDataType<>(MapleFlavored.id("taken_books"), TakenBooks::new, CODEC, null);

	private final Set<BlockPos> books = new HashSet<>();

	private TakenBooks() {
	}

	private TakenBooks(List<BlockPos> books) {
		this.books.addAll(books);
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> {
			TakenBooks taken = level.getDataStorage().computeIfAbsent(TYPE);

			if (taken.books.add(pos.immutable())) taken.setDirty();
		});
	}

	/** Whether the structure book at {@code book} has been taken. */
	public static boolean isTaken(ServerLevel level, BlockPos book) {
		TakenBooks taken = level.getDataStorage().get(TYPE);

		return taken != null && taken.books.contains(book);
	}

	private List<BlockPos> books() {
		return List.copyOf(books);
	}
}

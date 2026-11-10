package mapleflavored.gametest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.structurebooks.TerrainPreview;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

/**
 * Finds the structure books that structures plan and place, plans a book for pieces a test put together itself, and
 * records which books were taken.
 */
final class Books {
	private static final AtomicBoolean RECORDING_TAKES = new AtomicBoolean();
	private static final Map<BlockPos, List<ResourceKey<Enchantment>>> TAKES = new ConcurrentHashMap<>();

	private Books() {
	}

	/**
	 * Starts recording every take for {@link #takes}. Call from the static initializer of each test class that reads
	 * them, so recording starts before any of its tests run.
	 */
	// Fabric events can't be unregistered, so one listener records for every test, by absolute position.
	static void recordTakes() {
		if (!RECORDING_TAKES.compareAndSet(false, true)) return;

		StructureBooks.TAKEN.register((player, level, pos, enchantment) ->
			TAKES.computeIfAbsent(pos.immutable(), unused -> new CopyOnWriteArrayList<>()).add(enchantment.unwrapKey().orElseThrow())
		);
	}

	/** The enchantment each take of the book at {@code pos} gave, in order: one entry per taken event fired there. */
	static List<ResourceKey<Enchantment>> takes(BlockPos pos) {
		return List.copyOf(TAKES.getOrDefault(pos, List.of()));
	}

	/** Every book among the structure's pieces. A structure plans at most one. */
	static List<StructureBookPiece> pieces(StructureStart start) {
		return start.getPieces().stream().filter(StructureBookPiece.class::isInstance).map(StructureBookPiece.class::cast).toList();
	}

	/** The book the structure planned. Fails if it planned none. */
	static StructureBookPiece piece(StructureStart start) {
		return StructureBookPiece.among(start.getPieces())
			.orElseThrow(() -> new IllegalStateException("the structure at " + start.getChunkPos() + " planned no book"));
	}

	/** Where the one book block in the chunks the structure covers is. Fails the test unless there's exactly one. */
	static BlockPos only(GameTestHelper helper, StructureStart start) {
		List<BlockPos> books = Structures.books(helper.getLevel(), start);

		helper.assertValueEqual(books.size(), 1, "structure books at " + books + " in the structure at " + start.getChunkPos());

		return books.getFirst();
	}

	/** The enchantment the book block at {@code pos} gives. */
	static ResourceKey<Enchantment> enchantmentAt(ServerLevel level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof StructureBookBlockEntity book)) throw new IllegalStateException("no book at " + pos);

		return book.enchantment().flatMap(Holder::unwrapKey).orElseThrow();
	}

	/**
	 * A structure of the given pieces with the book its rule plans for them, as generation adds it. For pieces a test
	 * planned or changed itself; {@link Structures#plan} already plans a whole structure with its book. The book isn't
	 * locked, since nothing records its lock.
	 */
	// Mirrors StructureMixin's call.
	static StructureStart planWithBook(ServerLevel level, ResourceKey<Structure> key, ChunkPos chunk, List<? extends StructurePiece> pieces) {
		Holder.Reference<Structure> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		TerrainPreview terrain = TerrainPreview.of(
			generator, generator.getBiomeSource(), randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED), randomState, level
		);
		PiecesContainer withBook = StructureBookRules.withBook(
			structure, new PiecesContainer(List.copyOf(pieces)), level.getSeed(), chunk, terrain, level.getStructureTemplateManager()
		);

		return new StructureStart(structure.value(), chunk, 0, withBook);
	}
}

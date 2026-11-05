package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import mapleflavored.MapleFlavored;
import mapleflavored.spawners.Spawners;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every locked structure book in a level: the spawners that lock it and which of them are Cleared. A book can be taken
 * once all of its spawners are Cleared; a book with no lock here was never locked. Only the Nether Fortress's book is
 * locked, by the Blaze spawners its fortress generated with.
 *
 * <p>A lock is planned with its structure and recorded here before any read, whether or not the book's chunk has been
 * built. A Cleared spawner stays counted even once it's broken.
 */
public final class BookLocks extends SavedData {
	private static final Codec<BookLocks> CODEC = Lock.CODEC.listOf().xmap(BookLocks::new, BookLocks::locks);

	/** The level saved data that holds the locks. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<BookLocks> TYPE = new SavedDataType<>(MapleFlavored.id("book_locks"), BookLocks::new, CODEC, null);

	// Structures are planned on worldgen threads, which can't touch saved data. Their locks wait here until the server
	// thread records them, which it does each tick, before every save and before any lock is read or changed. The save
	// matters at shutdown: the server finishes its worldgen after its last tick, and saves those chunks.
	private static final Queue<Planned> PLANNED = new ConcurrentLinkedQueue<>();

	private final Map<BlockPos, Lock> locksByBook = new HashMap<>();
	private final Map<BlockPos, BlockPos> booksBySpawner = new HashMap<>();

	private BookLocks() {
	}

	private BookLocks(List<Lock> locks) {
		locks.forEach(this::put);
	}

	/** Records planned locks and counts Cleared spawners toward them. Call once from mod init. */
	static void register() {
		// A world opened after another in the same game must never receive the first world's locks.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> PLANNED.clear());
		ServerTickEvents.END_SERVER_TICK.register(BookLocks::recordPlanned);
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> recordPlanned(server));
		Spawners.CLEARED.register(BookLocks::countCleared);
	}

	/**
	 * Locks the book planned at {@code book} in {@code dimension} until every spawner in {@code spawners} is Cleared.
	 * Safe to call from any thread. Planning the same book again never resets its Cleared spawners, and a book with no
	 * spawners stays unlocked.
	 */
	static void plan(ResourceKey<Level> dimension, BlockPos book, List<BlockPos> spawners) {
		if (spawners.isEmpty()) return;

		PLANNED.add(new Planned(dimension, book.immutable(), List.copyOf(spawners)));
	}

	/** Whether the book at {@code book} can't be taken yet. */
	public static boolean isLocked(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::isLocked).orElse(false);
	}

	/** The spawners still to be Cleared before the book at {@code book} can be taken, in planned order. */
	public static List<BlockPos> remainingSpawners(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::remainingSpawners).orElse(List.of());
	}

	/** The lock on the book at {@code book}, if it was ever locked. */
	public static Optional<Lock> lockAt(ServerLevel level, BlockPos book) {
		return Optional.ofNullable(of(level).locksByBook.get(book));
	}

	private static BookLocks of(ServerLevel level) {
		recordPlanned(level.getServer());

		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private static void recordPlanned(MinecraftServer server) {
		for (Planned planned = PLANNED.poll(); planned != null; planned = PLANNED.poll()) {
			ServerLevel level = server.getLevel(planned.dimension());

			// A dimension removed since, such as a datapack's, has no book to lock.
			if (level == null) continue;

			BookLocks locks = level.getDataStorage().computeIfAbsent(TYPE);

			if (locks.locksByBook.containsKey(planned.book())) continue;

			locks.put(new Lock(planned.book(), planned.spawners(), Set.of()));
			locks.setDirty();
		}
	}

	private static void countCleared(ServerLevel level, BlockPos spawner) {
		BookLocks locks = of(level);
		BlockPos book = locks.booksBySpawner.get(spawner);

		if (book == null) return;

		Lock cleared = locks.locksByBook.get(book).withCleared(spawner);

		locks.put(cleared);
		locks.setDirty();

		if (!cleared.isLocked() && level.isLoaded(book)) showUnlocked(level, book);
	}

	// Sends the book's block entity again, so clients stop drawing it locked. An unloaded book is sent unlocked when
	// its chunk is.
	private static void showUnlocked(ServerLevel level, BlockPos book) {
		BlockState state = level.getBlockState(book);

		level.sendBlockUpdated(book, state, state, Block.UPDATE_CLIENTS);
	}

	private List<Lock> locks() {
		return List.copyOf(locksByBook.values());
	}

	private void put(Lock lock) {
		locksByBook.put(lock.book(), lock);
		lock.spawners().forEach(spawner -> booksBySpawner.put(spawner, lock.book()));
	}

	/**
	 * One locked book.
	 *
	 * @param book where the book is
	 * @param spawners the spawners that lock it, as planned with its structure
	 * @param cleared the spawners among them that have been Cleared, broken or not
	 */
	public record Lock(BlockPos book, List<BlockPos> spawners, Set<BlockPos> cleared) {
		static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("book").forGetter(Lock::book),
			BlockPos.CODEC.listOf().fieldOf("spawners").forGetter(Lock::spawners),
			BlockPos.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("cleared").forGetter(Lock::cleared)
		).apply(instance, Lock::new));

		public Lock {
			spawners = List.copyOf(spawners);
			cleared = Set.copyOf(cleared);
		}

		/** Whether a spawner is still to be Cleared. */
		public boolean isLocked() {
			return !cleared.containsAll(spawners);
		}

		/** The spawners still to be Cleared, in planned order. */
		public List<BlockPos> remainingSpawners() {
			return spawners.stream().filter(spawner -> !cleared.contains(spawner)).toList();
		}

		Lock withCleared(BlockPos spawner) {
			Set<BlockPos> withSpawner = new HashSet<>(cleared);

			withSpawner.add(spawner.immutable());

			return new Lock(book, spawners, withSpawner);
		}
	}

	private record Planned(ResourceKey<Level> dimension, BlockPos book, List<BlockPos> spawners) {
	}
}

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
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every locked structure book in a level: the keys that lock it and which of them are done. A book can be taken once
 * all of its keys are done; a book with no lock here was never locked. The Nether Fortress's book is locked by the Blaze
 * spawners its fortress generated with, each done once Cleared; a Bastion's by its keeper Brutes and a Stronghold's by
 * its Evokers, each done once it's gone for good; and an Ancient City's by its keeper shriekers, each done once broken.
 * A keeper its structure couldn't place is done from the start.
 *
 * <p>A lock is planned with its structure and recorded here before any read, whether or not the book's chunk has been
 * built. A done key stays done: a Cleared spawner still counts once it's broken.
 *
 * <p>A level that's Peaceful opens its Brute and Evoker locks for good. Peaceful has neither: it never spawns a keeper,
 * and drops a saved one as its chunk loads, both without a death to count.
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
	// Keepers a structure couldn't place as it was built, on worldgen threads. Their keys are done once recorded, after
	// the locks planned before them.
	private static final Queue<Forgiven> FORGIVEN = new ConcurrentLinkedQueue<>();

	private final Map<BlockPos, Lock> locksByBook = new HashMap<>();
	// The books locked by blocks, Blaze spawners and keeper shriekers, by each key's position.
	private final Map<BlockPos, BlockPos> booksByBlockKey = new HashMap<>();

	private BookLocks() {
	}

	private BookLocks(List<Lock> locks) {
		locks.forEach(this::put);
	}

	/** Records planned locks and counts Cleared spawners toward the spawner locks. Call once from mod init. */
	static void register() {
		// A world opened after another in the same game must never receive the first world's locks.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			PLANNED.clear();
			FORGIVEN.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(BookLocks::recordPlanned);
		ServerTickEvents.END_SERVER_TICK.register(BookLocks::openMobLocksWhilePeaceful);
		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> recordPlanned(server));
		Spawners.CLEARED.register(BookLocks::countCleared);
	}

	/**
	 * Locks the book planned at {@code book} in {@code dimension} until every one of {@code keys} is done. Safe to call
	 * from any thread. Planning the same book again never resets its done keys, and a book with no keys stays unlocked.
	 */
	static void plan(ResourceKey<Level> dimension, BlockPos book, Keys keys) {
		if (keys.positions().isEmpty()) return;

		PLANNED.add(new Planned(dimension, book.immutable(), keys));
	}

	/**
	 * Marks a keeper mob's key done: the keeper is gone for good. Updates the book's clients once that unlocks a loaded
	 * book.
	 *
	 * @param level the book's level
	 */
	static void markKeeperGone(ServerLevel level, BlockPos book, BlockPos key) {
		BookLocks locks = of(level);
		Lock lock = locks.locksByBook.get(book);

		if (lock == null || !lock.kind().isMob() || !lock.keys().contains(key) || lock.done().contains(key)) return;

		locks.markDone(level, lock, key);
	}

	/** Marks the key of a keeper mob its structure couldn't place done. Safe to call from any thread. */
	static void forgiveKeeper(GlobalPos book, BlockPos key) {
		FORGIVEN.add(new Forgiven(book, key.immutable()));
	}

	/**
	 * Marks a keeper shrieker's key done, if {@code key} is one: the shrieker is broken. Updates the book's clients once
	 * that unlocks a loaded book.
	 *
	 * @param level the keeper's level, which is its book's
	 */
	static void markShriekerBroken(ServerLevel level, BlockPos key) {
		BookLocks locks = of(level);
		Lock lock = Optional.ofNullable(locks.booksByBlockKey.get(key)).map(locks.locksByBook::get).orElse(null);

		if (lock == null || lock.kind() != Kind.SHRIEKERS || lock.done().contains(key)) return;

		locks.markDone(level, lock, key);
	}

	/** Every lock of the kind in the level, locked or not. */
	public static List<Lock> locksOf(ServerLevel level, Kind kind) {
		return of(level).locksByBook.values().stream().filter(lock -> lock.kind() == kind).toList();
	}

	/** Whether the book at {@code book} can't be taken yet. */
	public static boolean isLocked(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::isLocked).orElse(false);
	}

	/** The keys still to be done before the book at {@code book} can be taken, in planned order. */
	public static List<BlockPos> remainingKeys(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::remainingKeys).orElse(List.of());
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

			locks.put(new Lock(planned.book(), planned.keys().kind(), planned.keys().positions(), Set.of()));
			locks.setDirty();
		}

		for (Forgiven forgiven = FORGIVEN.poll(); forgiven != null; forgiven = FORGIVEN.poll()) {
			ServerLevel level = server.getLevel(forgiven.book().dimension());

			if (level != null) markKeeperGone(level, forgiven.book().pos(), forgiven.key());
		}
	}

	private static void openMobLocksWhilePeaceful(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			BookLocks locks = level.getDataStorage().get(TYPE);

			if (locks == null || level.getDifficulty() != Difficulty.PEACEFUL) continue;

			List<Lock> lockedByMobs = locks.locksByBook.values().stream()
				.filter(lock -> lock.kind().isMob() && lock.isLocked())
				.toList();

			for (Lock lock : lockedByMobs) {
				lock.remainingKeys().forEach(key -> locks.markDone(level, locks.locksByBook.get(lock.book()), key));
			}
		}
	}

	private static void countCleared(ServerLevel level, BlockPos spawner) {
		BookLocks locks = of(level);
		BlockPos book = locks.booksByBlockKey.get(spawner);

		if (book == null || locks.locksByBook.get(book).kind() != Kind.SPAWNERS) return;

		locks.markDone(level, locks.locksByBook.get(book), spawner);
	}

	private void markDone(ServerLevel level, Lock lock, BlockPos key) {
		Lock withKey = lock.withDone(key);

		put(withKey);
		setDirty();

		if (!withKey.isLocked() && level.isLoaded(lock.book())) showUnlocked(level, lock.book());
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

		if (lock.kind().isBlock()) lock.keys().forEach(key -> booksByBlockKey.put(key, lock.book()));
	}

	/** What locks a book, and when one of its keys is done. */
	public enum Kind {
		/** Blaze spawners, each done once Cleared. */
		SPAWNERS,
		/** Keeper Piglin Brutes, keyed by where each spawned, each done once it's gone for good. */
		BRUTES,
		/**
		 * Keeper sculk shriekers, keyed by the origin of the sculk patch each stands at, each done once broken. A keeper
		 * stands at its origin, or on the patch's catalyst just above it.
		 */
		SHRIEKERS,
		/** A Stronghold's Evokers, keyed by the origin of the piece that placed each, each done once it's gone for good. */
		EVOKERS;

		static final Codec<Kind> CODEC = Codec.STRING.xmap(Kind::valueOf, Kind::name);

		/** Whether each key is a block at the key's position, found again by that position when it changes. */
		boolean isBlock() {
			return !isMob();
		}

		/** Whether each key is a keeper mob, which counts itself once it's gone for good. */
		boolean isMob() {
			return this == BRUTES || this == EVOKERS;
		}

		/**
		 * Whether the keys are keepers: guards one of the structure's pieces places, which the book's piece saves so that
		 * building the piece can find them.
		 */
		public boolean isKeeper() {
			return this != SPAWNERS;
		}
	}

	/**
	 * What a planned book's lock is made of. No positions means the book isn't locked.
	 *
	 * @param positions each key's position, in planned order
	 */
	public record Keys(Kind kind, List<BlockPos> positions) {
		/** Locks nothing. */
		public static final Keys NONE = new Keys(Kind.SPAWNERS, List.of());

		public Keys {
			positions = List.copyOf(positions);
		}
	}

	/**
	 * One locked book.
	 *
	 * @param book where the book is
	 * @param kind what its keys are
	 * @param keys the keys that lock it, as planned with its structure
	 * @param done the keys among them that are done
	 */
	public record Lock(BlockPos book, Kind kind, List<BlockPos> keys, Set<BlockPos> done) {
		// Saved before Brute locks, as spawners and their Cleared set, so those names and the default kind stay.
		static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("book").forGetter(Lock::book),
			Kind.CODEC.optionalFieldOf("kind", Kind.SPAWNERS).forGetter(Lock::kind),
			BlockPos.CODEC.listOf().fieldOf("spawners").forGetter(Lock::keys),
			BlockPos.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("cleared").forGetter(Lock::done)
		).apply(instance, Lock::new));

		public Lock {
			keys = List.copyOf(keys);
			done = Set.copyOf(done);
		}

		/** Whether a key is still to be done. */
		public boolean isLocked() {
			return !done.containsAll(keys);
		}

		/** The keys still to be done, in planned order. */
		public List<BlockPos> remainingKeys() {
			return keys.stream().filter(key -> !done.contains(key)).toList();
		}

		Lock withDone(BlockPos key) {
			Set<BlockPos> withKey = new HashSet<>(done);

			withKey.add(key.immutable());

			return new Lock(book, kind, keys, withKey);
		}
	}

	private record Planned(ResourceKey<Level> dimension, BlockPos book, Keys keys) {
	}

	private record Forgiven(GlobalPos book, BlockPos key) {
	}
}

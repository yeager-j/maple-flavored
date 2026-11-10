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
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
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
 * built. The structure's book piece saves what locks the book, so a lock missing here, as after a crash that saved the
 * structure's chunk and not this, is planned again from the piece when the book is next read. A done key stays done: a
 * Cleared spawner still counts once it's broken.
 *
 * <p>The world says whether a spawner or shrieker key is done, so a take checks those against it and counts any this
 * missed. Only this knows a keeper mob is gone for good, since one out of sight may have wandered off, so a save of
 * the locks is scheduled as soon as a mob's key is done.
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
	// The books with no lock here whose structures have been asked for one. Never saved: each is asked once a session.
	private final Set<BlockPos> booksWithoutPlannedLock = new HashSet<>();

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
		// Worldgen that finishes while the chunks are saved queues after BEFORE_SAVE. Recorded here, it's written when
		// the level closes, which saves this once more.
		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> recordPlanned(server));
		Spawners.CLEARED.register(BookLocks::countCleared);
	}

	/**
	 * Locks the book among a structure's planned pieces until every key its piece saves is done. Safe to call from any
	 * thread. Planning the same book again never resets its done keys, and a book with no keys stays unlocked.
	 *
	 * @param dimension the dimension the structure is planned in
	 */
	public static void plan(ResourceKey<Level> dimension, List<StructurePiece> pieces) {
		Optional<StructureBookPiece> book = StructureBookPiece.among(pieces);

		if (book.isEmpty() || book.get().lock().positions().isEmpty()) return;

		PLANNED.add(new Planned(dimension, book.get().plannedSpot(), book.get().lock()));
	}

	/**
	 * Marks a keeper mob's key done: the keeper is gone for good. Updates the book's clients once that unlocks a loaded
	 * book.
	 *
	 * @param level the book's level
	 */
	static void markKeeperGone(ServerLevel level, BlockPos book, BlockPos key) {
		BookLocks locks = of(level);
		Optional<Lock> lock = locks.lockOrPlanAgain(level, book);

		if (lock.isEmpty() || !lock.get().kind().isMob() || !lock.get().keys().contains(key) || lock.get().done().contains(key)) return;

		locks.markDone(level, book, key);
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

		locks.markDone(level, lock.book(), key);
	}

	/** Every lock of the kind in the level, locked or not. */
	public static List<Lock> locksOf(ServerLevel level, Kind kind) {
		return of(level).locksByBook.values().stream().filter(lock -> lock.kind() == kind).toList();
	}

	/** Whether the book at {@code book} can't be taken yet. Reads the lock as {@link #lockAt} does. */
	public static boolean isLocked(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::isLocked).orElse(false);
	}

	/**
	 * The keys still to be done before the book at {@code book} can be taken, in planned order. Reads the lock as
	 * {@link #lockAt} does.
	 */
	public static List<BlockPos> remainingKeys(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::remainingKeys).orElse(List.of());
	}

	/**
	 * The lock on the book at {@code book}, if it was ever locked.
	 *
	 * <p>A book with no lock recorded has its saved structure asked for one, the first time it's read in a session, and
	 * a lock found that way is recorded with nothing done. Asking loads the book's chunk and the chunks its structures
	 * start in, as far as their structures. Call on the server thread.
	 */
	public static Optional<Lock> lockAt(ServerLevel level, BlockPos book) {
		return of(level).lockOrPlanAgain(level, book);
	}

	/**
	 * The lock on the book at {@code book}, read as {@link #lockAt} does, once its remaining keys are checked against
	 * the world. A spawner's key is done when its spawner is Cleared or gone, and a keeper shrieker's when it's gone,
	 * however that came about. A key is only checked in a chunk that's already loaded and fully generated. A keeper
	 * mob's key is never done this way: a keeper that isn't there may have wandered off. Updates the book's clients once
	 * this unlocks a loaded book.
	 */
	public static Optional<Lock> lockCheckedAgainstWorld(ServerLevel level, BlockPos book) {
		BookLocks locks = of(level);
		Optional<Lock> lock = locks.lockOrPlanAgain(level, book);

		if (lock.isEmpty()) return lock;

		lock.get().remainingKeys().stream()
			.filter(key -> isDoneInWorld(level, lock.get().kind(), key))
			.forEach(key -> locks.markDone(level, book, key));

		return Optional.of(locks.locksByBook.get(book));
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
				lock.remainingKeys().forEach(key -> locks.markDone(level, lock.book(), key));
			}
		}
	}

	private static void countCleared(ServerLevel level, BlockPos spawner) {
		BookLocks locks = of(level);
		BlockPos book = locks.booksByBlockKey.get(spawner);

		if (book == null || locks.locksByBook.get(book).kind() != Kind.SPAWNERS) return;

		locks.markDone(level, book, spawner);
	}

	private static boolean isDoneInWorld(ServerLevel level, Kind kind, BlockPos key) {
		return switch (kind) {
			case SPAWNERS -> isChunkBuilt(level, key) && (!level.getBlockState(key).is(Blocks.SPAWNER) || Spawners.isCleared(level, key));
			case SHRIEKERS -> ShriekerKeepers.isGone(level, key);
			case BRUTES, EVOKERS -> false;
		};
	}

	/**
	 * Whether the chunk at {@code pos} is loaded and fully generated, so everything its structures place is there.
	 * Never loads or generates it.
	 */
	static boolean isChunkBuilt(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
	}

	// The book's lock. A book with none asks its saved structure for the lock its piece saves, once.
	private Optional<Lock> lockOrPlanAgain(ServerLevel level, BlockPos book) {
		Lock lock = locksByBook.get(book);

		if (lock != null) return Optional.of(lock);
		if (!booksWithoutPlannedLock.add(book.immutable())) return Optional.empty();

		Optional<Lock> planned = plannedLock(level, book);

		planned.ifPresent(plannedAgain -> {
			put(plannedAgain);
			setDirty();
		});

		return planned;
	}

	// Reads the structures at the book through its chunk, loading that chunk and theirs as far as their structures.
	private static Optional<Lock> plannedLock(ServerLevel level, BlockPos book) {
		int sectionX = SectionPos.blockToSectionCoord(book.getX());
		int sectionZ = SectionPos.blockToSectionCoord(book.getZ());

		return level.structureManager().startsForStructure(sectionX, sectionZ, structure -> true).stream()
			.flatMap(start -> StructureBookPiece.among(start.getPieces()).stream())
			.filter(piece -> piece.plannedSpot().equals(book) && !piece.lock().positions().isEmpty())
			.findFirst()
			.map(piece -> new Lock(book, piece.lock().kind(), piece.lock().positions(), Set.of()));
	}

	private void markDone(ServerLevel level, BlockPos book, BlockPos key) {
		Lock withKey = locksByBook.get(book).withDone(key);

		put(withKey);
		setDirty();

		// Nothing else can say a keeper mob is gone, so its key doesn't wait for the next autosave, which the keeper's
		// own chunk can reach the disk before.
		if (withKey.kind().isMob()) level.getDataStorage().scheduleSave();

		if (!withKey.isLocked() && level.isLoaded(book)) showUnlocked(level, book);
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

		static final Codec<Keys> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Kind.CODEC.fieldOf("kind").forGetter(Keys::kind),
			BlockPos.CODEC.listOf().fieldOf("keys").forGetter(Keys::positions)
		).apply(instance, Keys::new));

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

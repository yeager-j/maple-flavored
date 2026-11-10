package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every locked structure book in a level: the keys that lock it and which of them are done. A book can be taken once
 * all of its keys are done; a book with no lock here was never locked. The Nether Fortress's book is locked by the Blaze
 * spawners its fortress generated with, each done once Cleared; a Bastion's by its keeper Brutes and a Stronghold's by
 * its Evokers, each done once it's gone for good; and an Ancient City's by its keeper shriekers, each done once broken.
 * A keeper its structure couldn't place is done from the start. A structure that looked for keys and found none, as an
 * Ancient City with no sculk patch far enough out, still has a lock here: one with no keys, which never locks its book.
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
	// The books whose keys are found again by position, Blaze spawners and keeper shriekers, by each key's position.
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
	 * thread. Planning the same book again never resets its done keys. A book whose piece saves no lock gets none, and
	 * one whose lock has no keys gets a lock that's never locked.
	 *
	 * @param dimension the dimension the structure is planned in
	 */
	public static void plan(ResourceKey<Level> dimension, List<StructurePiece> pieces) {
		Optional<StructureBookPiece> book = StructureBookPiece.among(pieces);

		if (book.isEmpty() || book.get().lock().isEmpty()) return;

		PLANNED.add(new Planned(dimension, book.get().plannedSpot(), book.get().lock().get()));
	}

	/**
	 * Marks a keeper mob's key done: the keeper is gone for good. Updates the book's clients when the book is loaded.
	 *
	 * @param level the book's level
	 */
	static void markKeeperGone(ServerLevel level, BlockPos book, BlockPos key) {
		of(level).countKeeperGone(level, book, key);
	}

	/** Marks the key of a keeper mob its structure couldn't place done. Safe to call from any thread. */
	static void forgiveKeeper(GlobalPos book, BlockPos key) {
		FORGIVEN.add(new Forgiven(book, key.immutable()));
	}

	/**
	 * Marks a keeper shrieker's key done, if {@code key} is one: the shrieker is broken. Updates the book's clients
	 * when the book is loaded.
	 *
	 * @param level the keeper's level, which is its book's
	 */
	static void markShriekerBroken(ServerLevel level, BlockPos key) {
		of(level).countBlockKeyDone(level, key, Kind.SHRIEKERS);
	}

	/** Every lock of the kind in the level, locked or not. */
	public static List<Lock> locksOf(ServerLevel level, Kind kind) {
		return of(level).locksByBook.values().stream().filter(lock -> lock.kind() == kind).toList();
	}

	/** Every lock in the level whose keys are keepers, locked or not, a lock that found no keepers included. */
	public static List<Lock> keeperLocks(ServerLevel level) {
		return of(level).locksByBook.values().stream().filter(lock -> lock.kind().isKeeper()).toList();
	}

	/** Whether the book at {@code book} can't be taken yet. Reads the lock as {@link #lockAt} does. */
	public static boolean isLocked(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::isLocked).orElse(false);
	}

	/**
	 * The share of its keys still to be done before the book at {@code book} can be taken: 1 with none done, and 0 for a
	 * book that can be taken or was never locked. Reads the lock as {@link #lockAt} does.
	 */
	public static float remainingShare(ServerLevel level, BlockPos book) {
		return lockAt(level, book).map(Lock::remainingShare).orElse(0.0F);
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
	 * mob's key is never done this way: a keeper that isn't there may have wandered off. Updates the book's clients
	 * for each key this finds done, when the book is loaded.
	 */
	public static Optional<Lock> lockCheckedAgainstWorld(ServerLevel level, BlockPos book) {
		BookLocks locks = of(level);
		Optional<Lock> lock = locks.lockOrPlanAgain(level, book);

		if (lock.isEmpty()) return lock;

		lock.get().remainingKeys().stream()
			.filter(key -> lock.get().kind().isDoneInWorld(level, key))
			.forEach(key -> locks.markDone(level, book, key));

		return Optional.of(locks.locksByBook.get(book));
	}

	private static BookLocks of(ServerLevel level) {
		recordPlanned(level.getServer());

		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private static void recordPlanned(MinecraftServer server) {
		// A keeper is forgiven after its lock was planned, so every lock these need is already waiting to be recorded.
		List<Forgiven> forgivenSoFar = new ArrayList<>();

		for (Forgiven forgiven = FORGIVEN.poll(); forgiven != null; forgiven = FORGIVEN.poll()) {
			forgivenSoFar.add(forgiven);
		}

		for (Planned planned = PLANNED.poll(); planned != null; planned = PLANNED.poll()) {
			ServerLevel level = server.getLevel(planned.dimension());

			// A dimension removed since, such as a datapack's, has no book to lock.
			if (level == null) continue;

			BookLocks locks = level.getDataStorage().computeIfAbsent(TYPE);

			if (locks.locksByBook.containsKey(planned.book())) continue;

			locks.put(new Lock(planned.book(), planned.keys().kind(), planned.keys().positions(), Set.of()));
			locks.setDirty();
		}

		for (Forgiven forgiven : forgivenSoFar) {
			ServerLevel level = server.getLevel(forgiven.book().dimension());

			if (level == null) continue;

			level.getDataStorage().computeIfAbsent(TYPE).countKeeperGone(level, forgiven.book().pos(), forgiven.key());
		}
	}

	private static void openMobLocksWhilePeaceful(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			BookLocks locks = level.getDataStorage().get(TYPE);

			if (locks == null || level.getDifficulty() != Difficulty.PEACEFUL) continue;

			List<Lock> opened = locks.locksByBook.values().stream()
				.filter(lock -> lock.kind().opensInPeaceful() && lock.isLocked())
				.toList();

			for (Lock lock : opened) {
				lock.remainingKeys().forEach(key -> locks.markDone(level, lock.book(), key));
			}
		}
	}

	private static void countCleared(ServerLevel level, BlockPos spawner) {
		of(level).countBlockKeyDone(level, spawner, Kind.SPAWNERS);
	}

	/**
	 * Whether the chunk at {@code pos} is loaded and fully generated, so everything its structures place is there.
	 * Never loads or generates it.
	 */
	static boolean isChunkBuilt(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
	}

	// Counts the block at the key done for the book it locks, if it's a key of the kind.
	private void countBlockKeyDone(ServerLevel level, BlockPos key, Kind kind) {
		Lock lock = Optional.ofNullable(booksByBlockKey.get(key)).map(locksByBook::get).orElse(null);

		if (lock == null || lock.kind() != kind || lock.done().contains(key)) return;

		markDone(level, lock.book(), key);
	}

	private void countKeeperGone(ServerLevel level, BlockPos book, BlockPos key) {
		Optional<Lock> lock = lockOrPlanAgain(level, book);

		if (lock.isEmpty() || lock.get().kind().form() != KeyForm.KEEPER_MOB) return;
		if (!lock.get().keys().contains(key) || lock.get().done().contains(key)) return;

		markDone(level, book, key);
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
			.filter(piece -> piece.plannedSpot().equals(book))
			.findFirst()
			.flatMap(StructureBookPiece::lock)
			.map(keys -> new Lock(book, keys.kind(), keys.positions(), Set.of()));
	}

	private void markDone(ServerLevel level, BlockPos book, BlockPos key) {
		Lock withKey = locksByBook.get(book).withDone(key);

		put(withKey);
		setDirty();

		// Nothing else can say such a key is done, so it doesn't wait for the next autosave, which the keeper's own chunk
		// can reach the disk before.
		if (withKey.kind().isKnownDoneOnlyByTheLock()) level.getDataStorage().scheduleSave();

		// An unloaded book is sent with what's left of its lock when its chunk is.
		if (level.isLoaded(book)) StructureBooks.showKeyDone(level, book);
	}

	private List<Lock> locks() {
		return List.copyOf(locksByBook.values());
	}

	private void put(Lock lock) {
		locksByBook.put(lock.book(), lock);

		if (lock.kind().isFoundByPosition()) lock.keys().forEach(key -> booksByBlockKey.put(key, lock.book()));
	}

	/**
	 * What locks a book. Each kind gives every answer the locks and the book need about its keys, so a kind can't be
	 * added without them.
	 */
	public enum Kind {
		/** Blaze spawners, each done once Cleared. */
		SPAWNERS(KeyForm.STRUCTURE_BLOCK, "block.maple_flavored.structure_book.locked") {
			@Override
			boolean isDoneInWorld(ServerLevel level, BlockPos key) {
				return isChunkBuilt(level, key) && (!level.getBlockState(key).is(Blocks.SPAWNER) || Spawners.isCleared(level, key));
			}
		},
		/** Keeper Piglin Brutes, keyed by where each spawned, each done once it's gone for good. */
		BRUTES(KeyForm.KEEPER_MOB, "block.maple_flavored.structure_book.locked_by_keepers") {
			@Override
			boolean isDoneInWorld(ServerLevel level, BlockPos key) {
				return false;
			}
		},
		/**
		 * Keeper sculk shriekers, keyed by the origin of the sculk patch each stands at, each done once broken. A keeper
		 * stands at its origin, or on the patch's catalyst just above it.
		 */
		SHRIEKERS(KeyForm.KEEPER_BLOCK, "block.maple_flavored.structure_book.locked_by_shriekers") {
			@Override
			boolean isDoneInWorld(ServerLevel level, BlockPos key) {
				return ShriekerKeepers.isGone(level, key);
			}
		},
		/** A Stronghold's Evokers, keyed by the origin of the piece that placed each, each done once it's gone for good. */
		EVOKERS(KeyForm.KEEPER_MOB, "block.maple_flavored.structure_book.locked_by_evokers") {
			@Override
			boolean isDoneInWorld(ServerLevel level, BlockPos key) {
				return false;
			}
		};

		static final Codec<Kind> CODEC = Codec.STRING.xmap(Kind::valueOf, Kind::name);

		private final KeyForm form;
		private final String refusal;

		Kind(KeyForm form, String refusal) {
			this.form = form;
			this.refusal = refusal;
		}

		/** What each key is. */
		public KeyForm form() {
			return form;
		}

		/**
		 * The translation key of the message a player gets when the book is refused. It takes the count of keys done and
		 * the count of keys in all.
		 */
		public String refusal() {
			return refusal;
		}

		/**
		 * Whether the keys are keepers: guards one of the structure's pieces places, which the book's piece saves so that
		 * building the piece can find them.
		 */
		public boolean isKeeper() {
			return switch (form) {
				case STRUCTURE_BLOCK -> false;
				case KEEPER_BLOCK, KEEPER_MOB -> true;
			};
		}

		/** Whether a key is found again by its position when the block there changes. */
		boolean isFoundByPosition() {
			return switch (form) {
				case STRUCTURE_BLOCK, KEEPER_BLOCK -> true;
				case KEEPER_MOB -> false;
			};
		}

		/** Whether a Peaceful level opens the lock for good, since it has none of the mobs that are the keys. */
		boolean opensInPeaceful() {
			return switch (form) {
				case STRUCTURE_BLOCK, KEEPER_BLOCK -> false;
				case KEEPER_MOB -> true;
			};
		}

		/** Whether only the locks can say a key is done, since the world never shows it. */
		boolean isKnownDoneOnlyByTheLock() {
			return switch (form) {
				case STRUCTURE_BLOCK, KEEPER_BLOCK -> false;
				case KEEPER_MOB -> true;
			};
		}

		/**
		 * Whether the world shows the key is done. Only reads a chunk that's already loaded and fully generated. Never
		 * true for a key only the locks can say is done.
		 */
		abstract boolean isDoneInWorld(ServerLevel level, BlockPos key);
	}

	/** What a key is, which decides how it's found again and who can say it's done. */
	public enum KeyForm {
		/** A block the structure generates anyway, at the key's position. */
		STRUCTURE_BLOCK,
		/** A keeper block, placed for the lock at or just above the key's position. */
		KEEPER_BLOCK,
		/** A keeper mob, which counts itself once it's gone for good. One that isn't there may have wandered off. */
		KEEPER_MOB
	}

	/**
	 * What a planned book's lock is made of. With no positions, its structure looked for keys of the kind and found
	 * none, so the book isn't locked.
	 *
	 * @param positions each key's position, in planned order
	 */
	public record Keys(Kind kind, List<BlockPos> positions) {
		static final Codec<Keys> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Kind.CODEC.fieldOf("kind").forGetter(Keys::kind),
			BlockPos.CODEC.listOf().fieldOf("keys").forGetter(Keys::positions)
		).apply(instance, Keys::new));

		public Keys {
			positions = List.copyOf(positions);
		}
	}

	/**
	 * One book's lock. A lock with no keys is one whose structure found none to plan, and never locks its book.
	 *
	 * @param book where the book is
	 * @param kind what its keys are
	 * @param keys the keys that lock it, as planned with its structure
	 * @param done the keys among them that are done
	 */
	public record Lock(BlockPos book, Kind kind, List<BlockPos> keys, Set<BlockPos> done) {
		static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("book").forGetter(Lock::book),
			Kind.CODEC.fieldOf("kind").forGetter(Lock::kind),
			BlockPos.CODEC.listOf().fieldOf("keys").forGetter(Lock::keys),
			BlockPos.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("done").forGetter(Lock::done)
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

		/** The share of the keys still to be done: 1 with none done, down to 0 once the book can be taken. */
		public float remainingShare() {
			if (keys.isEmpty()) return 0.0F;

			return (float) remainingKeys().size() / keys.size();
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

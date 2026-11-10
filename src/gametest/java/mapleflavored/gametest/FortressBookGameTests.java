package mapleflavored.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import mapleflavored.mixin.StructurePieceInvoker;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.StructureBookBlockEntity;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.structurebooks.TerrainPreview;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.jspecify.annotations.Nullable;

/**
 * Every new Nether Fortress has a Blaze spawner and one Fire Protection I book, above its lava well or at its start
 * crossing. The book stays locked until every Blaze spawner the fortress generated with is Cleared, wherever the book
 * is, and a Cleared spawner stays counted once broken.
 */
public class FortressBookGameTests {
	// Far from every other test's area. Each test plans along its own row, so no two share a fortress or its lock.
	private static final int FAR_CHUNK = 4000;
	private static final int CHUNKS_APART = 40;
	private static final int SCAN_ATTEMPTS = 40;
	private static final int GUARANTEE_PLANS = 200;
	private static final int DEATHS_TO_CLEAR = 12;
	// Where vanilla's fortress pieces put things, in piece-local coordinates.
	private static final BlockPos LAVA_WELL_BOOK = new BlockPos(6, 7, 6);
	private static final BlockPos START_CROSSING_BOOK = new BlockPos(9, 6, 9);
	private static final BlockPos BLAZE_SPAWNER = new BlockPos(3, 5, 5);
	private static final BlockPos HAND_SET_BOOK = new BlockPos(3, 2, 3);
	private static final BlockPos LONE_SPAWNER = new BlockPos(5, 2, 3);

	// Fabric events can't be unregistered, so one listener counts every take for the tests, by absolute position.
	private static final Map<BlockPos, AtomicInteger> TAKES = new ConcurrentHashMap<>();

	static {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) ->
			TAKES.computeIfAbsent(pos.immutable(), unused -> new AtomicInteger()).incrementAndGet()
		);
	}

	@GameTest(maxTicks = 400)
	public void everyFortressIsPlannedWithABlazeSpawnerTheSameWayEachTime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < GUARANTEE_PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(0, i));

			helper.assertTrue(!blazeSpawners(start).isEmpty(), "the fortress planned at " + start.getChunkPos() + " has a Blaze spawner");
		}

		List<String> first = layout(Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(0, 0)));
		List<String> second = layout(Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(0, 0)));

		helper.assertValueEqual(second, first, "the same fortress planned twice");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theBookFloatsLockedAboveTheLavaWell(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 1, start -> castleEntrance(start) != null, "a fortress with a Castle Entrance");
		List<BlockPos> spawners = blazeSpawners(planned);
		BlockPos expectedBook = worldPos(castleEntrance(planned), LAVA_WELL_BOOK);

		helper.assertValueEqual(StructureBookRules.enchantmentsFor(BuiltinStructures.FORTRESS), List.of(Enchantments.FIRE_PROTECTION), "the Fortress's books");

		Structures.chunks(planned).forEach(chunk -> Structures.build(level, planned, chunk));

		List<BlockPos> books = Structures.books(level, planned);

		helper.assertValueEqual(books, List.of(expectedBook), "the fortress's books");
		helper.assertValueEqual(bookEnchantment(level, expectedBook), Enchantments.FIRE_PROTECTION, "the book's enchantment");
		helper.assertValueEqual(level.getBlockState(expectedBook.below()).isAir(), true, "the book floats over open air above the well");
		helper.assertValueEqual(lockedSpawners(level, expectedBook), spawners, "the spawners locking the book");

		for (BlockPos spawner : spawners) {
			helper.assertTrue(
				level.getBlockEntity(spawner) instanceof SpawnerBlockEntity blockEntity && spawnsBlazes(level, blockEntity),
				"a Blaze spawner at " + spawner
			);
		}

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(!StructureBooks.take(player, expectedBook), "a locked book is refused");
		helper.assertValueEqual(player.getInventory().countItem(Items.ENCHANTED_BOOK), 0, "books given");
		helper.assertValueEqual(level.getBlockState(expectedBook).is(StructureBooks.BLOCK), true, "the book is still there");
		helper.assertValueEqual(takes(expectedBook), 0, "takes");
		helper.assertValueEqual(sentLocked(level, expectedBook), true, "the book is sent locked");
		helper.assertValueEqual(BookLocks.remainingKeys(level, expectedBook), spawners, "the spawners left to Clear");
		helper.succeed();
	}

	// Nearly every fortress has a Castle Entrance, so the rule is given a planned fortress's pieces without it.
	@GameTest
	public void withoutACastleEntranceTheBookFloatsAtTheStartCrossing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 2, start -> castleEntrance(start) != null, "a fortress with a Castle Entrance");
		List<StructurePiece> withoutEntrance = planned.getPieces().stream()
			.filter(piece -> !(piece instanceof NetherFortressPieces.CastleEntrance) && !(piece instanceof StructureBookPiece))
			.toList();
		StructurePiece startCrossing = withoutEntrance.getFirst();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		TerrainPreview terrain = TerrainPreview.of(
			generator, generator.getBiomeSource(), randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED), randomState, level
		);
		Holder.Reference<Structure> fortress = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.FORTRESS);
		PiecesContainer withBook = StructureBookRules.withBook(
			fortress,
			new PiecesContainer(withoutEntrance),
			level.getSeed(),
			planned.getChunkPos(),
			terrain,
			level.getStructureTemplateManager()
		);
		StructureStart replanned = new StructureStart(fortress.value(), planned.getChunkPos(), 0, withBook);

		helper.assertTrue(startCrossing instanceof NetherFortressPieces.StartPiece, "the first piece is the start crossing");
		helper.assertValueEqual(bookPiece(replanned).spot(), worldPos(startCrossing, START_CROSSING_BOOK), "the book's spot");
		helper.assertValueEqual(BookLocks.lockAt(level, bookPiece(replanned).spot()), Optional.empty(), "the lock of a book that was only added to its pieces");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void spawnersClearedWhileTheBookIsUnbuiltUnlockItEvenOnceOneIsBroken(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(
			level,
			3,
			start -> blazeSpawners(start).stream().noneMatch(spawner -> sameChunk(spawner, bookPiece(start).spot())),
			"a fortress whose book's chunk holds no Blaze spawner"
		);
		BlockPos book = bookPiece(planned).spot();
		ChunkPos bookChunk = ChunkPos.containing(book);
		List<BlockPos> spawners = blazeSpawners(planned);

		Structures.chunks(planned).stream().filter(chunk -> !chunk.equals(bookChunk)).forEach(chunk -> Structures.build(level, planned, chunk));
		helper.assertValueEqual(level.getBlockState(book).is(StructureBooks.BLOCK), false, "the book isn't built yet");

		for (BlockPos spawner : spawners.subList(0, spawners.size() - 1)) {
			clear(helper, spawner);
		}

		BlockPos last = spawners.getLast();

		helper.assertValueEqual(BookLocks.isLocked(level, book), true, "locked with one spawner left");
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(last), "the spawner left");

		Structures.plan(level, BuiltinStructures.FORTRESS, planned.getChunkPos());
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(last), "the spawner left after planning the fortress again");

		clear(helper, last);

		ServerPlayer breaker = SpawnerGameTests.survivalPlayer(helper, Items.DIAMOND_PICKAXE);

		breaker.gameMode.destroyBlock(spawners.getFirst());
		helper.assertValueEqual(level.getBlockState(spawners.getFirst()).isAir(), true, "the broken spawner is gone");

		helper.assertValueEqual(BookLocks.isLocked(level, book), false, "unlocked, the broken spawner still counted");

		reloadBookLocks(level);
		Structures.build(level, planned, bookChunk);

		helper.assertValueEqual(BookLocks.isLocked(level, book), false, "still unlocked after a reload");
		helper.assertValueEqual(sentLocked(level, book), false, "the book is sent unlocked");

		ServerPlayer taker = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(StructureBooks.take(taker, book), "the unlocked book is taken");
		helper.assertValueEqual(storedEnchantments(taker), Map.of(Enchantments.FIRE_PROTECTION, 1), "the book taken");
		helper.assertValueEqual(takes(book), 1, "takes");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aBuiltBookIsSentLessOfItsLockAsEachSpawnerIsCleared(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 4, start -> true, "any fortress");
		BlockPos book = bookPiece(planned).spot();
		List<BlockPos> spawners = blazeSpawners(planned);

		Structures.chunks(planned).forEach(chunk -> Structures.build(level, planned, chunk));
		helper.assertValueEqual(sentLockRemaining(level, book), 1.0F, "the share of the lock sent before any spawner is Cleared");

		for (int cleared = 1; cleared <= spawners.size(); cleared++) {
			clear(helper, spawners.get(cleared - 1));

			float remaining = (float) (spawners.size() - cleared) / spawners.size();

			helper.assertValueEqual(sentLockRemaining(level, book), remaining, "the share of the lock sent with " + cleared + " Cleared");
		}

		helper.assertValueEqual(BookLocks.isLocked(level, book), false, "unlocked");
		helper.assertValueEqual(sentLocked(level, book), false, "the book is sent unlocked");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aSavedFortressBuildsItsBookWhereItsLockIs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 5, start -> true, "any fortress");
		StructureStart loaded = Structures.saveAndLoad(level, planned);
		BlockPos book = bookPiece(planned).spot();

		Structures.chunks(loaded).forEach(chunk -> Structures.build(level, loaded, chunk));

		helper.assertValueEqual(Structures.books(level, loaded), List.of(book), "the loaded fortress's books");
		helper.assertValueEqual(sentLocked(level, book), true, "the book is sent locked");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aLockPlannedJustBeforeASaveIsSavedWithIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(6, 0));
		BlockPos book = bookPiece(planned).spot();

		// As at shutdown, when the last chunks are planned after the last tick: nothing reads the locks before the save.
		ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(level.getServer(), false, false);

		helper.assertTrue(savedBooks(level).contains(book), "the planned lock is in the saved locks");
		helper.succeed();
	}

	// Worldgen that finishes while the chunks are saved plans its locks after the save has begun.
	@GameTest(maxTicks = 400)
	public void aLockPlannedDuringASaveIsRecordedAfterIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(7, 0));
		BlockPos book = bookPiece(planned).spot();

		ServerLifecycleEvents.AFTER_SAVE.invoker().onAfterSave(level.getServer(), true, false);

		helper.assertTrue(savedBooks(level).contains(book), "the planned lock is in the locks the level saves as it closes");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aSavedBookPieceKeepsItsWholeLock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 8, start -> true, "any fortress");
		StructureBookPiece book = bookPiece(planned);
		StructureBookPiece loaded = bookPiece(Structures.saveAndLoad(level, planned));
		List<BlockPos> spawners = blazeSpawners(planned);

		helper.assertValueEqual(book.lock(), new BookLocks.Keys(BookLocks.Kind.SPAWNERS, spawners), "the lock the planned piece carries");
		helper.assertValueEqual(loaded.lock(), book.lock(), "the lock the loaded piece carries");
		helper.assertValueEqual(loaded.keepers(), List.of(), "the keepers of a book that spawners lock");

		// Saved before the piece saved its lock: a keeper lock's keys alone, which must still place its keepers.
		StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level);
		CompoundTag legacy = book.createTag(context);

		legacy.remove("Lock");
		legacy.store("Keepers", BlockPos.CODEC.listOf(), spawners);

		StructureBookPiece old = new StructureBookPiece(legacy);

		helper.assertValueEqual(old.keepers(), spawners, "the keepers of a piece saved before locks");
		helper.assertValueEqual(old.lock(), BookLocks.Keys.NONE, "the lock of a piece saved before locks");
		helper.assertValueEqual(
			old.createTag(context).read("Keepers", BlockPos.CODEC.listOf()),
			Optional.of(spawners),
			"the keepers saved again"
		);
		helper.succeed();
	}

	// A crash can save a fortress's chunks without its lock: chunks are written as they change, the locks only on a save.
	@GameTest(maxTicks = 400)
	public void aLockMissingFromTheSavedLocksIsPlannedAgainFromItsSavedFortress(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 9, start -> true, "any fortress");
		BlockPos book = bookPiece(planned).spot();
		BookLocks.Lock lock = BookLocks.lockAt(level, book).orElseThrow();

		Structures.chunks(planned).forEach(chunk -> Structures.build(level, planned, chunk));
		forgetLock(level, book);
		helper.assertValueEqual(BookLocks.lockAt(level, book), Optional.empty(), "the forgotten lock of a fortress no chunk holds");

		Structures.register(level, planned);
		forgetLock(level, book);
		helper.assertTrue(!savedBooks(level).contains(book), "the lock is missing from the saved locks");
		helper.assertValueEqual(sentLocked(level, book), true, "the book is sent locked");
		helper.assertValueEqual(BookLocks.lockAt(level, book), Optional.of(lock), "the lock planned again");
		helper.assertTrue(savedBooks(level).contains(book), "the lock planned again is saved");

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(!StructureBooks.take(player, book), "the book is refused");
		helper.succeed();
	}

	// The same crash can save a spawner Cleared or removed without the lock having counted it.
	@GameTest(maxTicks = 400)
	public void aTakeCountsTheSpawnersTheLockMissedBeingClearedOrRemoved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = planFortress(level, 10, start -> blazeSpawners(start).size() == 2, "a fortress with two Blaze spawners");
		BlockPos book = bookPiece(planned).spot();
		List<BlockPos> spawners = blazeSpawners(planned);
		BlockPos standing = spawners.get(0);
		BlockPos removed = spawners.get(1);
		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		Structures.chunks(planned).forEach(chunk -> Structures.build(level, planned, chunk));
		Structures.register(level, planned);
		level.setBlockAndUpdate(removed, Blocks.AIR.defaultBlockState());
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), spawners, "the spawners left with one removed unseen");

		helper.assertTrue(!StructureBooks.take(player, book), "the book is refused with a spawner standing");
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(standing), "the spawners left after the refused take");

		clear(helper, standing);
		forgetLock(level, book);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), spawners, "the spawners left once the lock is planned again");

		helper.assertTrue(StructureBooks.take(player, book), "the book is taken with one spawner Cleared and one gone");
		helper.succeed();
	}

	@GameTest
	public void aBookNoSpawnerLocksIsTakenStraightAway(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SpawnerBlockEntity loneSpawner = SpawnerGameTests.placeSpawner(helper, LONE_SPAWNER, new CompoundTag());

		// Any read records the locks other tests have planned, so only the Clearing below could change them.
		BookLocks.lockAt(level, loneSpawner.getBlockPos());

		Tag locksBefore = savedBookLocks(level);

		SpawnerGameTests.killMarkedMobs(helper, loneSpawner, DEATHS_TO_CLEAR);
		helper.assertValueEqual(savedBookLocks(level), locksBefore, "the locks after Clearing a spawner outside any fortress");
		helper.setBlock(HAND_SET_BOOK, StructureBooks.BLOCK);

		BlockPos book = helper.absolutePos(HAND_SET_BOOK);
		Holder<Enchantment> fireProtection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);

		((StructureBookBlockEntity) level.getBlockEntity(book)).setEnchantment(fireProtection);

		helper.assertValueEqual(sentLocked(level, book), false, "the book is sent unlocked");

		ServerPlayer taker = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(StructureBooks.take(taker, book), "the book is taken");
		helper.assertValueEqual(storedEnchantments(taker), Map.of(Enchantments.FIRE_PROTECTION, 1), "the book taken");
		helper.succeed();
	}

	// Plans fortresses along the test's own row until one is what the test needs.
	private static StructureStart planFortress(ServerLevel level, int row, Predicate<StructureStart> wanted, String what) {
		for (int attempt = 0; attempt < SCAN_ATTEMPTS; attempt++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.FORTRESS, farChunk(row, attempt));

			if (wanted.test(start)) return start;
		}

		throw new IllegalStateException("no " + what + " in " + SCAN_ATTEMPTS + " plans");
	}

	private static ChunkPos farChunk(int row, int attempt) {
		return new ChunkPos(-FAR_CHUNK + attempt * CHUNKS_APART, FAR_CHUNK + row * CHUNKS_APART);
	}

	private static NetherFortressPieces.@Nullable CastleEntrance castleEntrance(StructureStart start) {
		return start.getPieces().stream()
			.filter(NetherFortressPieces.CastleEntrance.class::isInstance)
			.map(NetherFortressPieces.CastleEntrance.class::cast)
			.findFirst()
			.orElse(null);
	}

	private static List<BlockPos> blazeSpawners(StructureStart start) {
		return start.getPieces().stream()
			.filter(NetherFortressPieces.MonsterThrone.class::isInstance)
			.map(throne -> worldPos(throne, BLAZE_SPAWNER))
			.toList();
	}

	private static StructureBookPiece bookPiece(StructureStart start) {
		return start.getPieces().stream()
			.filter(StructureBookPiece.class::isInstance)
			.map(StructureBookPiece.class::cast)
			.findFirst()
			.orElseThrow();
	}

	private static BlockPos worldPos(StructurePiece piece, BlockPos local) {
		StructurePieceInvoker world = (StructurePieceInvoker) piece;

		return new BlockPos(world.worldX(local.getX(), local.getZ()), world.worldY(local.getY()), world.worldZ(local.getX(), local.getZ()));
	}

	// Each piece's type and box, enough to tell two layouts apart.
	private static List<String> layout(StructureStart start) {
		return start.getPieces().stream().map(piece -> piece.getClass().getSimpleName() + " " + piece.getBoundingBox()).toList();
	}

	private static boolean sameChunk(BlockPos a, BlockPos b) {
		return ChunkPos.containing(a).equals(ChunkPos.containing(b));
	}

	private static List<BlockPos> lockedSpawners(ServerLevel level, BlockPos book) {
		return BookLocks.lockAt(level, book).map(BookLocks.Lock::keys).orElse(List.of());
	}

	private static void clear(GameTestHelper helper, BlockPos spawner) {
		SpawnerBlockEntity blockEntity = (SpawnerBlockEntity) helper.getLevel().getBlockEntity(spawner);

		SpawnerGameTests.killMarkedMobs(helper, blockEntity, DEATHS_TO_CLEAR);
		helper.assertValueEqual(SpawnerGameTests.clearing(blockEntity).isCleared(), true, "the spawner at " + spawner + " is Cleared");
	}

	private static boolean spawnsBlazes(ServerLevel level, SpawnerBlockEntity spawner) {
		Entity shown = spawner.getSpawner().getOrCreateDisplayEntity(level, spawner.getBlockPos());

		return shown != null && shown.getType() == EntityTypes.BLAZE;
	}

	// The level's locks as they'd be saved.
	private static Tag savedBookLocks(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		return BookLocks.TYPE.codec().encodeStart(ops, level.getDataStorage().computeIfAbsent(BookLocks.TYPE)).getOrThrow();
	}

	// The books the level's saved locks are for, read from the saved form without recording any planned lock.
	private static List<BlockPos> savedBooks(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		return ((ListTag) savedBookLocks(level)).stream()
			.map(lock -> BlockPos.CODEC.parse(ops, ((CompoundTag) lock).get("book")).getOrThrow())
			.toList();
	}

	// Loads the level's locks back without the book's, as a restart after a crash that saved the book's structure and not
	// its lock would. Nothing the locks counted for the book is kept.
	private static void forgetLock(ServerLevel level, BlockPos book) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		ListTag saved = (ListTag) savedBookLocks(level);

		saved.removeIf(lock -> BlockPos.CODEC.parse(ops, ((CompoundTag) lock).get("book")).getOrThrow().equals(book));
		level.getDataStorage().set(BookLocks.TYPE, BookLocks.TYPE.codec().parse(ops, saved).getOrThrow());
	}

	// Saves the level's locks and loads them back in their place, as a restart would.
	static void reloadBookLocks(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		level.getDataStorage().set(BookLocks.TYPE, BookLocks.TYPE.codec().parse(ops, savedBookLocks(level)).getOrThrow());
	}

	private static boolean sentLocked(ServerLevel level, BlockPos book) {
		return sentLockRemaining(level, book) > 0.0F;
	}

	// The share of the book's lock still on, as clients are sent it to draw the book's cage.
	private static float sentLockRemaining(ServerLevel level, BlockPos book) {
		return level.getBlockEntity(book).getUpdateTag(level.registryAccess()).getFloatOr("lock_remaining", 0.0F);
	}

	private static ResourceKey<Enchantment> bookEnchantment(ServerLevel level, BlockPos book) {
		return ((StructureBookBlockEntity) level.getBlockEntity(book)).enchantment().orElseThrow().unwrapKey().orElseThrow();
	}

	private static Map<ResourceKey<Enchantment>, Integer> storedEnchantments(ServerPlayer player) {
		Map<ResourceKey<Enchantment>, Integer> levels = new HashMap<>();

		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

			stored.entrySet().forEach(entry -> levels.put(entry.getKey().unwrapKey().orElseThrow(), entry.getIntValue()));
		}

		return levels;
	}

	private static int takes(BlockPos book) {
		return TAKES.getOrDefault(book, new AtomicInteger()).get();
	}
}

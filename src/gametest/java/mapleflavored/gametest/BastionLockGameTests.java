package mapleflavored.gametest;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.BruteKeepers;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * A Bastion's book is locked until three of its Piglin Brutes, its keepers, chosen when it's planned, are gone for good,
 * by any cause. A refused take points the keepers out. Peaceful, which has no Brutes, opens the lock.
 */
public class BastionLockGameTests {
	private static final int PLANS = 80;
	private static final int KEEPERS = 3;
	private static final int KEEPER_GLOW_TICKS = 60;
	private static final float LAVA_DAMAGE = 1000.0F;
	private static final ChunkPos SWEEP_START = new ChunkPos(-8000, 8000);
	private static final int CHUNKS_APART = 40;
	private static final List<Identifier> BRUTE_TEMPLATES = List.of(
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin"),
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin_always")
	);

	// Fabric events can't be unregistered, so one listener counts every take for the tests, by absolute position.
	private static final Map<BlockPos, AtomicInteger> TAKES = new ConcurrentHashMap<>();

	static {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) ->
			TAKES.computeIfAbsent(pos.immutable(), unused -> new AtomicInteger()).incrementAndGet()
		);
	}

	@GameTest(maxTicks = 400)
	public void everyBastionIsPlannedWithThreeOfItsBrutesAsKeepers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, new ChunkPos(SWEEP_START.x() + i * CHUNKS_APART, SWEEP_START.z()));
			StructureBookPiece book = BastionBookGameTests.bookPieces(start).getFirst();
			List<BlockPos> brutes = start.getPieces().stream()
				.filter(piece -> BastionBookGameTests.templateOf(piece).filter(BRUTE_TEMPLATES::contains).isPresent())
				.map(piece -> ((PoolElementStructurePiece) piece).getPosition())
				.toList();
			List<BlockPos> keys = BookLocks.lockAt(level, book.spot()).map(BookLocks.Lock::keys).orElse(List.of());

			helper.assertValueEqual(keys.size(), Math.min(KEEPERS, brutes.size()), "keepers of the Bastion at " + start.getChunkPos());
			helper.assertTrue(brutes.containsAll(keys), "the keepers of the Bastion at " + start.getChunkPos() + " are its Brutes");
			helper.assertValueEqual(new HashSet<>(keys).size(), keys.size(), "distinct keepers at " + start.getChunkPos());
			helper.assertValueEqual(book.keepers(), keys, "the keepers the book piece saves at " + start.getChunkPos());
			helper.assertValueEqual(lockKind(level, book.spot()), Optional.of(BookLocks.Kind.BRUTES), "the lock's kind");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aPlacedBastionsKeepersLockItsBookAndARefusedTakePointsThemOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, 30));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		List<BlockPos> keys = BookLocks.remainingKeys(level, book);
		List<PiglinBrute> keepers = BruteKeepers.liveKeepers(level, book);

		helper.assertValueEqual(keys.size(), KEEPERS, "the book's keepers");
		helper.assertValueEqual(
			keepers.stream().map(keeper -> BruteKeepers.keyOf(keeper).orElseThrow()).collect(java.util.stream.Collectors.toSet()),
			Set.copyOf(keys),
			"the keys of the Brutes tagged as keepers"
		);
		helper.assertValueEqual(keepers.size(), KEEPERS, "keepers alive");
		helper.assertTrue(sentLocked(level, book), "the book is sent locked");

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(!StructureBooks.take(player, book), "a locked book is refused");
		helper.assertValueEqual(player.getInventory().countItem(Items.ENCHANTED_BOOK), 0, "books given");
		helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), "the book is still there");
		helper.assertValueEqual(takes(book), 0, "takes");

		for (PiglinBrute keeper : keepers) {
			MobEffectInstance glow = keeper.getEffect(MobEffects.GLOWING);

			helper.assertTrue(glow != null && glow.getDuration() == KEEPER_GLOW_TICKS, "the keeper at " + keeper.blockPosition() + " glows for 60 ticks");
		}

		for (AbstractPiglin piglin : BastionBookGameTests.piglinsIn(level, start)) {
			helper.assertValueEqual(BastionBookGameTests.angryAt(piglin), Optional.empty(), "a refused take angers the Piglin at " + piglin.blockPosition());
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void onlyTheKeepersDeathsCountHoweverTheyDieAndWhereverTheyGo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerLevel nether = level.getServer().getLevel(Level.NETHER);
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, 31));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);
		List<PiglinBrute> keepers = BruteKeepers.liveKeepers(level, book);
		PiglinBrute nonKeeper = BastionBookGameTests.piglinsIn(level, start).stream()
			.filter(PiglinBrute.class::isInstance)
			.map(PiglinBrute.class::cast)
			.filter(brute -> BruteKeepers.keyOf(brute).isEmpty())
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("the treasure room has a Brute that isn't a keeper"));

		nonKeeper.kill(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS, "keepers left after a non-keeper dies");

		Entity moved = keepers.get(0).teleport(new TeleportTransition(nether, keepers.get(0).position(), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));

		helper.assertTrue(moved instanceof PiglinBrute, "the keeper went through to the Nether");
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS, "keepers left after one changes dimension");

		((PiglinBrute) moved).kill(nether);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS - 1, "keepers left after one dies in another dimension");

		keepers.get(1).hurtServer(level, level.damageSources().lava(), LAVA_DAMAGE);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS - 2, "keepers left after one burns in lava");
		helper.assertTrue(sentLocked(level, book), "the book is sent locked with a keeper left");

		keepers.get(2).discard();
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(), "keepers left after the last is removed");
		helper.assertTrue(!sentLocked(level, book), "the book is sent unlocked");

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(StructureBooks.take(player, book), "the unlocked book is taken");
		helper.assertValueEqual(takes(book), 1, "takes");

		List<AbstractPiglin> hunters = BastionBookGameTests.piglinsIn(level, start);

		helper.assertTrue(!hunters.isEmpty(), "the Bastion still has Piglins");
		helper.assertValueEqual(BastionBookGameTests.angryAt(hunters.getFirst()), Optional.of(player.getUUID()), "a Piglin hunts the taker");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theKeepersAndTheirProgressSurviveSavingAndLoading(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, 32);
		StructureStart reloaded = Structures.saveAndLoad(level, planned);
		StructureBookPiece book = BastionBookGameTests.bookPieces(planned).getFirst();

		helper.assertValueEqual(BastionBookGameTests.bookPieces(reloaded).getFirst().keepers(), book.keepers(), "the keepers the reloaded book piece saves");

		BastionBookGameTests.placeRegistered(level, reloaded);

		List<PiglinBrute> keepers = BruteKeepers.liveKeepers(level, book.spot());

		helper.assertValueEqual(keepers.size(), KEEPERS, "keepers tagged as the reloaded Bastion is built");

		keepers.getFirst().kill(level);

		BookLocks locks = level.getDataStorage().computeIfAbsent(BookLocks.TYPE);
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Codec<BookLocks> codec = BookLocks.TYPE.codec();
		BookLocks roundTripped = codec.parse(ops, codec.encodeStart(ops, locks).getOrThrow()).getOrThrow();

		level.getDataStorage().set(BookLocks.TYPE, roundTripped);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book.spot()).size(), KEEPERS - 1, "keepers left once the locks are reloaded");
		helper.assertValueEqual(lockKind(level, book.spot()), Optional.of(BookLocks.Kind.BRUTES), "the reloaded lock's kind");

		keepers.get(1).kill(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book.spot()).size(), KEEPERS - 2, "keepers left after another dies");
		helper.succeed();
	}

	// Saved before Brute locks existed, a lock has no kind and is a spawner lock.
	@GameTest
	public void aLockSavedWithoutAKindIsASpawnerLock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos book = helper.absolutePos(new BlockPos(1, 2, 3));
		BlockPos spawner = helper.absolutePos(new BlockPos(4, 5, 6));
		BlockPos cleared = helper.absolutePos(new BlockPos(7, 8, 9));
		CompoundTag legacy = new CompoundTag();
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Codec<BookLocks> codec = BookLocks.TYPE.codec();

		legacy.store("book", BlockPos.CODEC, book);
		legacy.store("spawners", BlockPos.CODEC.listOf(), List.of(spawner, cleared));
		legacy.store("cleared", BlockPos.CODEC.listOf(), List.of(cleared));

		// Loaded alongside the level's own locks, so no other test's lock is lost.
		BookLocks.lockAt(level, book);

		ListTag saved = (ListTag) codec.encodeStart(ops, level.getDataStorage().computeIfAbsent(BookLocks.TYPE)).getOrThrow();

		saved.add(legacy);
		level.getDataStorage().set(BookLocks.TYPE, codec.parse(ops, saved).getOrThrow());

		BookLocks.Lock lock = BookLocks.lockAt(level, book).orElseThrow();

		helper.assertValueEqual(lock.kind(), BookLocks.Kind.SPAWNERS, "the legacy lock's kind");
		helper.assertValueEqual(lock.remainingKeys(), List.of(spawner), "the legacy lock's spawners left to Clear");
		helper.succeed();
	}

	// Peaceful never spawns the keepers, so the lock opens for good.
	@GameTest(maxTicks = 400, environment = "maple_flavored_gametest:peaceful")
	public void peacefulOpensABastionsLock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.HOUSING_UNITS, 33));
		BlockPos book = BastionBookGameTests.onlyBook(helper, start);

		helper.assertValueEqual(BruteKeepers.liveKeepers(level, book), List.of(), "keepers alive in Peaceful");

		helper.succeedWhen(() -> {
			helper.assertTrue(!BookLocks.isLocked(level, book), "the book is unlocked in Peaceful");
			helper.assertValueEqual(
				BookLocks.lockAt(level, book).map(lock -> lock.done().containsAll(lock.keys())).orElse(false),
				true,
				"every key is done for good"
			);
		});
	}

	private static Optional<BookLocks.Kind> lockKind(ServerLevel level, BlockPos book) {
		return BookLocks.lockAt(level, book).map(BookLocks.Lock::kind);
	}

	private static boolean sentLocked(ServerLevel level, BlockPos book) {
		return level.getBlockEntity(book).getUpdateTag(level.registryAccess()).getBooleanOr("locked", false);
	}

	private static int takes(BlockPos book) {
		return TAKES.getOrDefault(book, new AtomicInteger()).get();
	}
}

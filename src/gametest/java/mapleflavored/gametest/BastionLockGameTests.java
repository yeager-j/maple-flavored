package mapleflavored.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.MobKeepers;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * A Bastion's book is locked until three of its Piglin Brutes, its keepers, chosen when it's planned, are gone for good,
 * by any cause. A refused take points the keepers out, and {@code /keepers} lists them. Peaceful, which has no Brutes,
 * opens the lock.
 */
public class BastionLockGameTests {
	private static final int PLANS = 80;
	private static final int KEEPERS = 3;
	private static final int KEEPER_GLOW_TICKS = 60;
	private static final float LAVA_DAMAGE = 1000.0F;
	private static final List<Identifier> BRUTE_TEMPLATES = List.of(
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin"),
		Identifier.withDefaultNamespace("bastion/mobs/melee_piglin_always")
	);

	static {
		Books.recordTakes();
	}

	@GameTest(maxTicks = 400)
	public void everyBastionIsPlannedWithThreeOfItsBrutesAsKeepers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.BASTION_REMNANT, TestSites.of(helper).chunk(i));
			StructureBookPiece book = Books.piece(start);
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
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
		List<BlockPos> keys = BookLocks.remainingKeys(level, book);
		List<Mob> keepers = MobKeepers.liveKeepers(level, book);

		helper.assertValueEqual(keys.size(), KEEPERS, "the book's keepers");
		helper.assertValueEqual(
			keepers.stream().map(keeper -> MobKeepers.keyOf(keeper).orElseThrow()).collect(Collectors.toSet()),
			Set.copyOf(keys),
			"the keys of the Brutes tagged as keepers"
		);
		helper.assertValueEqual(keepers.size(), KEEPERS, "keepers alive");
		helper.assertTrue(Locks.sentLocked(level, book), "the book is sent locked");

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(!StructureBooks.take(player, book), "a locked book is refused");
		helper.assertValueEqual(player.getInventory().countItem(Items.ENCHANTED_BOOK), 0, "books given");
		helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), "the book is still there");
		helper.assertValueEqual(Books.takes(book).size(), 0, "takes");

		for (Mob keeper : keepers) {
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
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
		List<Mob> keepers = MobKeepers.liveKeepers(level, book);
		PiglinBrute nonKeeper = BastionBookGameTests.piglinsIn(level, start).stream()
			.filter(PiglinBrute.class::isInstance)
			.map(PiglinBrute.class::cast)
			.filter(brute -> MobKeepers.keyOf(brute).isEmpty())
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("the treasure room has a Brute that isn't a keeper"));

		nonKeeper.kill(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS, "keepers left after a non-keeper dies");

		Entity moved = keepers.get(0).teleport(new TeleportTransition(nether, keepers.get(0).position(), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));

		helper.assertTrue(moved instanceof PiglinBrute, "the keeper went through to the Nether");
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS, "keepers left after one changes dimension");

		((PiglinBrute) moved).kill(nether);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS - 1, "keepers left after one dies in another dimension");
		// Only the locks know a keeper is gone, so they don't wait for the next autosave to be written.
		helper.assertFalse(level.getDataStorage().computeIfAbsent(BookLocks.TYPE).isDirty(), "The locks are still unsaved after a keeper dies");

		keepers.get(1).hurtServer(level, level.damageSources().lava(), LAVA_DAMAGE);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book).size(), KEEPERS - 2, "keepers left after one burns in lava");
		helper.assertTrue(Locks.sentLocked(level, book), "the book is sent locked with a keeper left");

		keepers.get(2).discard();
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(), "keepers left after the last is removed");
		helper.assertTrue(!Locks.sentLocked(level, book), "the book is sent unlocked");

		ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.STONE);

		helper.assertTrue(StructureBooks.take(player, book), "the unlocked book is taken");
		helper.assertValueEqual(Books.takes(book).size(), 1, "takes");

		List<AbstractPiglin> hunters = BastionBookGameTests.piglinsIn(level, start);

		helper.assertTrue(!hunters.isEmpty(), "the Bastion still has Piglins");
		helper.assertValueEqual(BastionBookGameTests.angryAt(hunters.getFirst()), Optional.of(player.getUUID()), "a Piglin hunts the taker");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theKeepersAndTheirProgressSurviveSavingAndLoading(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, TestSites.of(helper));
		StructureStart reloaded = Structures.saveAndLoad(level, planned);
		StructureBookPiece book = Books.piece(planned);

		helper.assertValueEqual(Books.piece(reloaded).keepers(), book.keepers(), "the keepers the reloaded book piece saves");

		BastionBookGameTests.placeRegistered(level, reloaded);

		List<Mob> keepers = MobKeepers.liveKeepers(level, book.spot());

		helper.assertValueEqual(keepers.size(), KEEPERS, "keepers tagged as the reloaded Bastion is built");

		keepers.getFirst().kill(level);

		Locks.reload(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book.spot()).size(), KEEPERS - 1, "keepers left once the locks are reloaded");
		helper.assertValueEqual(lockKind(level, book.spot()), Optional.of(BookLocks.Kind.BRUTES), "the reloaded lock's kind");

		keepers.get(1).kill(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book.spot()).size(), KEEPERS - 2, "keepers left after another dies");
		helper.succeed();
	}

	// A keeper that isn't where it spawned may have wandered off, so only its death counts.
	@GameTest(maxTicks = 400)
	public void checkingALockNeverCountsAKeeperThatIsntThere(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart planned = BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, TestSites.of(helper));
		BlockPos book = Books.piece(planned).spot();
		List<BlockPos> keys = BookLocks.remainingKeys(level, book);

		helper.assertValueEqual(keys.size(), KEEPERS, "the book's keepers");
		keys.forEach(level::getChunk);
		helper.assertValueEqual(
			BookLocks.lockCheckedAgainstWorld(level, book).map(BookLocks.Lock::remainingKeys),
			Optional.of(keys),
			"keepers left once the lock is checked with their chunks loaded and empty"
		);
		helper.succeed();
	}

	// A keeper that unloads, as its chunk does when players leave, comes back with it, so only one gone for good counts.
	@GameTest(maxTicks = 400)
	public void aKeeperThatOnlyUnloadsStillLocksTheBookAndKeepersSaysHowEachOneIs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.TREASURE_ROOM, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);
		List<BlockPos> keys = BookLocks.remainingKeys(level, book);
		Map<BlockPos, Mob> keepers = MobKeepers.liveKeepers(level, book).stream()
			.collect(Collectors.toMap(keeper -> MobKeepers.keyOf(keeper).orElseThrow(), keeper -> keeper));

		helper.assertValueEqual(keepers.keySet(), Set.copyOf(keys), "the keys of the Brutes tagged as keepers");
		helper.assertValueEqual(keys.size(), KEEPERS, "the book's keepers");

		keepers.get(keys.get(0)).kill(level);
		keepers.get(keys.get(1)).remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), keys.subList(1, KEEPERS), "the keepers left after one dies and one unloads");

		List<Component> lines = Locks.keepersAt(level, Vec3.atCenterOf(book));

		helper.assertValueEqual(
			lines.stream().map(Locks::key).toList(),
			List.of(Locks.KEEPERS_BOOK, Locks.KEEPERS_KEEPER, Locks.KEEPERS_KEEPER, Locks.KEEPERS_KEEPER),
			"the lines /keepers wrote"
		);
		helper.assertValueEqual(Locks.args(lines.get(0)).subList(1, 3), List.<Object>of(1, KEEPERS), "the keepers /keepers counted gone");
		helper.assertValueEqual(
			lines.stream().skip(1).map(Locks::keeperStatus).toList(),
			List.of("commands.maple_flavored.keepers.gone", "commands.maple_flavored.keepers.mob_unloaded", "commands.maple_flavored.keepers.alive"),
			"each keeper's status"
		);
		helper.succeed();
	}

	// Peaceful never spawns the keepers, so the lock opens for good.
	@GameTest(maxTicks = 400, environment = "maple_flavored_gametest:peaceful")
	public void peacefulOpensABastionsLock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = BastionBookGameTests.placeRegistered(level, BastionBookGameTests.planLayout(level, BastionBookGameTests.HOUSING_UNITS, TestSites.of(helper)));
		BlockPos book = Books.only(helper, start);

		helper.assertValueEqual(MobKeepers.liveKeepers(level, book), List.of(), "keepers alive in Peaceful");

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

}

package mapleflavored.gametest;

import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import mapleflavored.grounded.Grounded;
import mapleflavored.structurebooks.BookLocks;
import mapleflavored.structurebooks.ShriekerKeepers;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.structurebooks.TakenBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Ancient City's book: Swift Sneak over the altar in front of the portal, locked until the city's keeper shriekers
 * are broken, and the city keeps players Grounded until it's taken. Taking it summons nothing.
 *
 * <p>A city's centre is built into terrain that's already generated, so its piece is first cleared, as worldgen's
 * terrain adaptation clears it. Each city is planned at its own far chunk.
 */
public class AncientCityBookGameTests {
	// Away from the other structure tests' areas, and far enough apart that no two cities overlap.
	private static final int FAR_CHUNK = 5000;
	private static final int CHUNKS_APART = 40;
	private static final int PLANS = 12;
	private static final int SCAN_ATTEMPTS = 40;
	private static final List<Identifier> CENTRES = List.of(
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_1"),
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_2"),
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_3")
	);
	// How far out from the portal's plane the altar's centre block is.
	private static final int ALTAR_OUT = 6;
	// The centre whose altar is marked by a chiseled block in the plaza floor.
	private static final Identifier CHISELED_ALTAR_CENTRE = Identifier.withDefaultNamespace("ancient_city/city_center/city_center_3");

	// The ring rule's values.
	private static final double MIN_KEEPER_DISTANCE = 30.0;
	private static final double WIDEST_WINDOW = 60.0;
	private static final int MAX_KEEPERS = 3;
	private static final int FIRST_PICKS = 200;

	private static final String LOCKED_BY_SHRIEKERS = "block.maple_flavored.structure_book.locked_by_shriekers";
	private static final String KEEPERS_BOOK = "commands.maple_flavored.keepers.book";
	private static final String KEEPERS_KEEPER = "commands.maple_flavored.keepers.keeper";
	private static final double SUBSPACE_RADIUS = SubspaceArena.RADIUS;
	// Past the Grounded the city last topped up, which lasts 60 ticks.
	private static final int GROUNDED_WEARS_OFF = 70;
	private static final int GROUNDING_TICKS = 2;
	private static final double WARDENS_SEARCHED = 120.0;

	private static final Set<BlockPos> TAKEN = ConcurrentHashMap.newKeySet();

	static {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> TAKEN.add(pos.immutable()));
	}

	@GameTest(maxTicks = 400)
	public void everyAncientCityIsPlannedWithOneSwiftSneakBookInItsCentre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		helper.assertValueEqual(StructureBookRules.enchantmentsFor(BuiltinStructures.ANCIENT_CITY), List.of(Enchantments.SWIFT_SNEAK), "the city's books");

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(0, i));
			List<StructureBookPiece> books = BastionBookGameTests.bookPieces(start);

			helper.assertValueEqual(books.size(), 1, "books planned in the city at " + start.getChunkPos());
			helper.assertValueEqual(books.getFirst().enchantment(), Enchantments.SWIFT_SNEAK, "the book planned at " + start.getChunkPos());
			helper.assertTrue(
				start.getPieces().getFirst().getBoundingBox().isInside(books.getFirst().spot()),
				"the book at " + books.getFirst().spot() + " is in the city's centre"
			);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 1200)
	public void everyCityCentreHasItsBookAboveTheAltar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Set<Identifier> seen = new HashSet<>();

		for (int i = 0; i < SCAN_ATTEMPTS && seen.size() < CENTRES.size(); i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(1, i));
			Identifier centre = BastionBookGameTests.templateOf(start.getPieces().getFirst()).orElseThrow();

			if (!seen.add(centre)) continue;

			BlockPos book = buildCentre(level, start);
			String where = "the book at " + book + " in " + centre;

			helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), where + " is placed");
			assertAboveTheAltar(helper, level, start.getPieces().getFirst(), book, where);

			if (centre.equals(CHISELED_ALTAR_CENTRE)) {
				helper.assertTrue(level.getBlockState(book.below(2)).is(Blocks.CHISELED_DEEPSLATE), where + " is above the altar's chiseled block");
			}
		}

		helper.assertValueEqual(seen, Set.copyOf(CENTRES), "the city centres checked");
		helper.succeed();
	}

	@GameTest
	public void onlyOriginsAtLeast30BlocksFromTheBookCanBeKeepers(GameTestHelper helper) {
		BlockPos book = BlockPos.ZERO;
		List<BlockPos> tooClose = List.of(at(book, 29.0, 0.0), at(book, 20.0, 120.0), at(book, 5.0, 240.0));
		List<BlockPos> withOneFar = new ArrayList<>(tooClose);

		withOneFar.add(at(book, 31.0, 90.0));

		helper.assertValueEqual(ShriekerKeepers.choose(book, tooClose, RandomSource.create(1L)), List.of(), "keepers among origins too close");
		helper.assertValueEqual(ShriekerKeepers.choose(book, withOneFar, RandomSource.create(1L)), List.of(withOneFar.getLast()), "keepers with one far origin");
		helper.assertValueEqual(ShriekerKeepers.choose(book, List.of(), RandomSource.create(1L)), List.of(), "keepers with no origins");
		helper.succeed();
	}

	@GameTest
	public void theFirstKeeperIsPickedFromTheFartherHalfMiddleIncluded(GameTestHelper helper) {
		BlockPos book = BlockPos.ZERO;
		// Five eligible origins, so the farther half is the three farthest.
		List<BlockPos> origins = IntStream.range(0, 5).mapToObj(i -> at(book, 40.0 + i * 10.0, i * 72.0)).toList();
		Set<BlockPos> fartherHalf = Set.of(origins.get(4), origins.get(3), origins.get(2));
		Set<BlockPos> picked = new HashSet<>();
		RandomSource random = RandomSource.create(28L);

		for (int i = 0; i < FIRST_PICKS; i++) {
			picked.add(ShriekerKeepers.choose(book, origins, random).getFirst());
		}

		helper.assertValueEqual(picked, fartherHalf, "the first keepers picked");
		helper.succeed();
	}

	@GameTest
	public void theOtherKeepersAreTheFarthestInTheirThirdsOfTheRing(GameTestHelper helper) {
		BlockPos book = BlockPos.ZERO;
		BlockPos first = at(book, 100.0, 0.0);
		BlockPos nearSecond = at(book, 50.0, 140.0);
		BlockPos fartherOutsideSecond = at(book, 90.0, 160.0);
		BlockPos nearThird = at(book, 40.0, 230.0);
		BlockPos fartherOutsideThird = at(book, 80.0, 290.0);
		List<BlockPos> origins = List.of(first, nearSecond, fartherOutsideSecond, nearThird, fartherOutsideThird);

		helper.assertValueEqual(
			ShriekerKeepers.choose(book, origins, farthestFirst()),
			List.of(first, nearSecond, nearThird),
			"keepers within 30° of their directions"
		);
		helper.succeed();
	}

	@GameTest
	public void aWindowWithNoOriginWidensTo45Then60ThenStaysEmpty(GameTestHelper helper) {
		BlockPos book = BlockPos.ZERO;
		BlockPos first = at(book, 100.0, 0.0);
		BlockPos within45 = at(book, 50.0, 120.0 + 44.0);
		BlockPos within60OfBoth = at(book, 50.0, 180.0);
		BlockPos past60 = at(book, 90.0, 240.0 + 70.0);

		helper.assertValueEqual(
			ShriekerKeepers.choose(book, List.of(first, within45, past60), farthestFirst()),
			List.of(first, within45),
			"keepers when the third's window finds none within 60°"
		);
		helper.assertValueEqual(
			ShriekerKeepers.choose(book, List.of(first, within60OfBoth, past60), farthestFirst()),
			List.of(first, within60OfBoth),
			"keepers when one origin fits both windows, used once"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void plannedKeepersArePatchOriginsSpreadAroundTheBook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int citiesWithThree = 0;

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(2, i));
			StructureBookPiece book = BastionBookGameTests.bookPieces(start).getFirst();
			List<BlockPos> keepers = book.keepers();
			List<BlockPos> origins = ShriekerKeepers.origins(start.getPieces());
			String where = "the city at " + start.getChunkPos();

			helper.assertTrue(origins.containsAll(keepers), "the keepers of " + where + " are sculk patch origins");
			helper.assertTrue(keepers.size() <= MAX_KEEPERS, where + " has at most three keepers");
			helper.assertValueEqual(BookLocks.remainingKeys(level, book.spot()), keepers, "the locked keys of " + where);

			for (BlockPos keeper : keepers) {
				helper.assertTrue(distance(book.spot(), keeper) >= MIN_KEEPER_DISTANCE, "the keeper at " + keeper + " is 30 blocks out in " + where);
			}

			for (int k = 1; k < keepers.size(); k++) {
				double turn = turnBetween(direction(book.spot(), keepers.get(k)), direction(book.spot(), keepers.getFirst()));

				helper.assertTrue(Math.abs(turn - 120.0) <= WIDEST_WINDOW, "the keeper at " + keepers.get(k) + " is a third of the way round in " + where);
			}

			if (keepers.size() == MAX_KEEPERS) citiesWithThree++;
		}

		helper.assertTrue(citiesWithThree > 0, "No planned city had three keepers");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aKeeperGoesOnTopOfItsPatchsCatalystOrAtItsOrigin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 3, 2);
		List<StructurePiece> pieces = start.getPieces();
		StructureBookPiece book = BastionBookGameTests.bookPieces(start).getFirst();
		BlockPos withCatalyst = book.keepers().get(0);
		BlockPos without = book.keepers().get(1);

		clearAround(level, withCatalyst);
		clearAround(level, without);
		level.setBlock(withCatalyst, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_CLIENTS);

		// Each piece is built as nothing, so only the keepers that follow a patch are placed.
		for (StructurePiece piece : pieces) {
			StructureBooks.buildPiece(piece, pieces, level, () -> {
			});
		}

		helper.assertTrue(level.getBlockState(withCatalyst).is(Blocks.SCULK_CATALYST), "The catalyst was replaced");
		assertKeeperAt(helper, level, withCatalyst.above(), "on top of the catalyst");
		assertKeeperAt(helper, level, without, "at the origin");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aBuiltKeeperPatchHasItsKeeper(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 4, 1);
		BlockPos keeper = BastionBookGameTests.bookPieces(start).getFirst().keepers().getFirst();

		clearAround(level, keeper);
		Structures.build(level, start, new ChunkPos(SectionPos.blockToSectionCoord(keeper.getX()), SectionPos.blockToSectionCoord(keeper.getZ())));

		BlockPos spot = level.getBlockState(keeper).is(Blocks.SCULK_CATALYST) ? keeper.above() : keeper;

		assertKeeperAt(helper, level, spot, "at the built patch");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void breakingKeepersUnlocksTheBookWhileItsChunkIsUnloaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 5, MAX_KEEPERS);
		BlockPos book = BastionBookGameTests.bookPieces(start).getFirst().spot();
		List<BlockPos> keepers = BookLocks.remainingKeys(level, book);
		BlockPos onCatalyst = keepers.get(1);
		BlockPos underPlayersShrieker = keepers.get(2);

		placeKeeper(level, keepers.get(0));
		level.setBlock(onCatalyst, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
		placeKeeper(level, onCatalyst.above());
		placeKeeper(level, underPlayersShrieker);
		helper.assertTrue(level.getChunkSource().getChunkNow(book.getX() >> 4, book.getZ() >> 4) == null, "The book's chunk is loaded");

		level.destroyBlock(keepers.get(0), false);
		placeKeeper(level, keepers.get(0));
		level.destroyBlock(keepers.get(0), false);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), keepers.subList(1, 3), "the keepers left after one breaks, twice");

		// A shrieker a player placed can't summon, so breaking it over a keeper isn't breaking the keeper.
		level.setBlock(underPlayersShrieker.above(), Blocks.SCULK_SHRIEKER.defaultBlockState(), Block.UPDATE_ALL);
		level.destroyBlock(underPlayersShrieker.above(), false);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), keepers.subList(1, 3), "the keepers left after a player's shrieker breaks");

		// The catalyst goes first, so nothing marks where the keeper on it stands.
		level.destroyBlock(onCatalyst, false);
		level.destroyBlock(onCatalyst.above(), false);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(underPlayersShrieker), "the keepers left after the one on a catalyst breaks");

		FortressBookGameTests.reloadBookLocks(level);
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), List.of(underPlayersShrieker), "the keepers left after a reload");

		level.destroyBlock(underPlayersShrieker, false);
		helper.assertFalse(BookLocks.isLocked(level, book), "The book is still locked with every keeper broken");
		helper.succeed();
	}

	// A keeper can go without the lock counting it: removed while its chunk was being generated, or broken just before a
	// crash that saved its chunk and not the lock.
	@GameTest(maxTicks = 400)
	public void checkingALockCountsTheKeepersMissingFromLoadedChunksOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 10, MAX_KEEPERS);
		BlockPos book = BastionBookGameTests.bookPieces(start).getFirst().spot();
		List<BlockPos> keepers = BookLocks.remainingKeys(level, book);
		BlockPos standing = keepers.get(0);
		BlockPos missing = keepers.get(1);
		BlockPos unloaded = keepers.get(2);

		keepers.forEach(keeper -> helper.assertFalse(isChunkLoaded(level, keeper), "The chunk of the keeper at " + keeper + " is loaded"));
		helper.assertValueEqual(remainingOnceChecked(level, book), keepers, "the keepers left with none of their chunks loaded");

		placeKeeper(level, standing);
		level.getChunk(missing);
		helper.assertFalse(isChunkLoaded(level, unloaded), "The last keeper's chunk is loaded");
		helper.assertValueEqual(BookLocks.remainingKeys(level, book), keepers, "the keepers left before the lock is checked");
		helper.assertValueEqual(remainingOnceChecked(level, book), List.of(standing, unloaded), "the keepers left once the lock is checked");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aLockedBookCountsItsBrokenKeepersAndTakingItSummonsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 6, 2);
		BlockPos book = buildCentre(level, start);
		List<BlockPos> keepers = BookLocks.remainingKeys(level, book);
		List<Component> overlay = new ArrayList<>();
		ServerPlayer player = MockPlayers.survivalPlayerReadingOverlay(helper, overlay);

		keepers.forEach(keeper -> placeKeeper(level, keeper));
		level.destroyBlock(keepers.getFirst(), false);

		helper.assertFalse(StructureBooks.take(player, book), "The locked book was taken");
		helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), "The locked book isn't there");
		helper.assertFalse(TAKEN.contains(book), "A taken event fired for the locked book");
		helper.assertValueEqual(overlay.size(), 1, "the messages shown");

		TranslatableContents message = (TranslatableContents) overlay.getFirst().getContents();

		helper.assertValueEqual(message.getKey(), LOCKED_BY_SHRIEKERS, "the message shown");
		helper.assertValueEqual(List.of(message.getArgs()), List.<Object>of(1, keepers.size()), "the broken keepers counted");

		keepers.stream().skip(1).forEach(keeper -> level.destroyBlock(keeper, false));

		helper.assertTrue(StructureBooks.take(player, book), "The unlocked book wasn't taken");
		helper.assertTrue(holdsSwiftSneakBook(helper, player), "The player doesn't hold Swift Sneak I");
		helper.assertTrue(TAKEN.contains(book), "No taken event fired");
		helper.assertValueEqual(wardensAround(level, book), List.of(), "the Wardens around the city's book");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void keepersListsTheNearestCitysKeepersAndWhetherEachStands(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 9, MAX_KEEPERS);
		BlockPos book = BastionBookGameTests.bookPieces(start).getFirst().spot();
		List<BlockPos> keepers = BookLocks.remainingKeys(level, book);
		List<Component> output = new ArrayList<>();

		placeKeeper(level, keepers.get(0));
		placeKeeper(level, keepers.get(1));
		level.destroyBlock(keepers.get(1), false);
		level.setBlock(keepers.get(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		runAt(level, Vec3.atCenterOf(book), "keepers", output);

		helper.assertValueEqual(
			output.stream().map(AncientCityBookGameTests::key).toList(),
			List.of(KEEPERS_BOOK, KEEPERS_KEEPER, KEEPERS_KEEPER, KEEPERS_KEEPER),
			"the lines /keepers wrote"
		);
		helper.assertValueEqual(args(output.get(0)).subList(1, 3), List.<Object>of(1, MAX_KEEPERS), "the broken keepers counted");
		helper.assertValueEqual(
			output.stream().skip(1).map(line -> key((Component) args(line).get(3))).toList(),
			List.of("commands.maple_flavored.keepers.standing", "commands.maple_flavored.keepers.broken", "commands.maple_flavored.keepers.missing"),
			"each keeper's status"
		);

		level.setBlock(keepers.get(2), Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
		placeKeeper(level, keepers.get(2).above());
		helper.assertValueEqual(ShriekerKeepers.standingSpot(level, keepers.get(2)), Optional.of(keepers.get(2).above()), "where the keeper on a catalyst stands");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_grounding", maxTicks = 400)
	public void theCityGroundsPlayersUntilItsBookIsTaken(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerLevel level = helper.getLevel();
		StructureStart start = planWithKeepers(level, 7, 1);
		BlockPos book = buildCentre(level, start);
		BlockPos farInside = farthestCorner(start.getBoundingBox(), book);
		ServerPlayer nearBook = standingAt(helper, arena, book.east(2), GameType.SURVIVAL);
		ServerPlayer farFromBook = standingAt(helper, arena, farInside, GameType.SURVIVAL);
		ServerPlayer creative = standingAt(helper, arena, book.west(2), GameType.CREATIVE);
		ServerPlayer spectator = standingAt(helper, arena, book.north(2), GameType.SPECTATOR);

		Structures.register(level, start);
		helper.assertTrue(distance(book, farInside) > SUBSPACE_RADIUS + 5.0, "The far player is within a Warden's Subspace of the book");

		helper.startSequence()
			.thenExecuteAfter(GROUNDING_TICKS, () -> {
				helper.assertTrue(Grounded.isGrounded(nearBook), "A survival player by the book isn't Grounded");
				helper.assertTrue(Grounded.isGrounded(farFromBook), "A survival player at the city's edge isn't Grounded");
				helper.assertFalse(Grounded.isGrounded(creative), "A creative player is Grounded");
				helper.assertFalse(Grounded.isGrounded(spectator), "A spectator is Grounded");

				BookLocks.remainingKeys(level, book).forEach(keeper -> {
					placeKeeper(level, keeper);
					level.destroyBlock(keeper, false);
				});
				helper.assertTrue(StructureBooks.take(nearBook, book), "The unlocked book wasn't taken");
				reloadTakenBooks(level);
				helper.assertTrue(TakenBooks.isTaken(level, book), "The taken book was forgotten on a reload");
				arena.track(stillWarden(level, nearBook.position()));
			})
			.thenExecuteAfter(GROUNDED_WEARS_OFF, () -> {
				helper.assertTrue(Grounded.isGrounded(nearBook), "A player in a Warden's Subspace stopped being Grounded");
				helper.assertFalse(Grounded.isGrounded(farFromBook), "A player outside every Subspace is still Grounded once the book is taken");
			})
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_grounding_bookless", maxTicks = 200)
	public void aCityWithoutABookGroundsNobody(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerLevel level = helper.getLevel();
		StructureStart planned = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(8, 0));
		List<StructurePiece> withoutBook = planned.getPieces().stream().filter(piece -> !(piece instanceof StructureBookPiece)).toList();
		StructureStart bookless = new StructureStart(planned.getStructure(), planned.getChunkPos(), 0, new PiecesContainer(withoutBook));
		BlockPos centre = planned.getPieces().getFirst().getBoundingBox().getCenter();
		ServerPlayer player = standingAt(helper, arena, centre, GameType.SURVIVAL);

		Structures.register(level, bookless);

		helper.startSequence()
			.thenExecuteAfter(GROUNDING_TICKS, () -> helper.assertFalse(Grounded.isGrounded(player), "A city without a book Grounded a player"))
			.thenSucceed();
	}

	// The portal frame stands in one vertical plane. The altar is out from its middle, on the axis the city's entrance
	// lines up with; the frame is an even number of blocks wide, so that axis is half a block off its exact middle.
	private static void assertAboveTheAltar(GameTestHelper helper, ServerLevel level, StructurePiece centre, BlockPos book, String where) {
		List<BlockPos> frame = BlockPos.betweenClosedStream(centre.getBoundingBox())
			.filter(pos -> level.getBlockState(pos).is(Blocks.REINFORCED_DEEPSLATE))
			.map(BlockPos::immutable)
			.toList();
		BoundingBox span = BoundingBox.encapsulatingPositions(frame).orElseThrow(() -> new IllegalStateException("no portal in " + where));
		boolean facesX = span.getXSpan() == 1;
		int out = facesX ? book.getX() - span.minX() : book.getZ() - span.minZ();
		int along = facesX ? book.getZ() : book.getX();
		int alongMin = facesX ? span.minZ() : span.minX();
		int alongMax = facesX ? span.maxZ() : span.maxX();

		helper.assertTrue(facesX || span.getZSpan() == 1, "the portal in " + where + " stands in one plane");
		helper.assertValueEqual(Math.abs(out), ALTAR_OUT, "how far " + where + " is out from the portal");
		helper.assertValueEqual(Math.abs(2 * along - (alongMin + alongMax)), 1, "how far, in half blocks, " + where + " is off the portal's middle");
		helper.assertTrue(book.getY() < span.minY(), where + " is below the portal, over the plaza");
	}

	private static void assertKeeperAt(GameTestHelper helper, ServerLevel level, BlockPos spot, String where) {
		BlockState keeper = level.getBlockState(spot);

		helper.assertTrue(keeper.is(Blocks.SCULK_SHRIEKER), "No shrieker " + where + ", but " + keeper);
		helper.assertTrue(keeper.getValue(SculkShriekerBlock.CAN_SUMMON), "The keeper " + where + " can't summon");
	}

	// Plans cities along a row of their own until one has at least this many keepers.
	private static StructureStart planWithKeepers(ServerLevel level, int row, int keepers) {
		for (int i = 0; i < SCAN_ATTEMPTS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(row, i));

			if (BastionBookGameTests.bookPieces(start).getFirst().keepers().size() >= keepers) return start;
		}

		throw new IllegalStateException("no city with " + keepers + " keepers in " + SCAN_ATTEMPTS + " plans");
	}

	// Clears the city's centre piece and builds the chunks it covers, and returns its book. Worldgen's beard_box terrain
	// adaptation clears it of terrain that way, leaving open what the piece's templates don't fill.
	private static BlockPos buildCentre(ServerLevel level, StructureStart start) {
		BoundingBox centre = start.getPieces().getFirst().getBoundingBox();
		ChunkPos min = new ChunkPos(centre.minX() >> 4, centre.minZ() >> 4);
		ChunkPos max = new ChunkPos(centre.maxX() >> 4, centre.maxZ() >> 4);
		List<ChunkPos> chunks = ChunkPos.rangeClosed(min, max).toList();

		chunks.forEach(chunk -> level.getChunk(chunk.x(), chunk.z()));
		BlockPos.betweenClosedStream(centre).forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
		chunks.forEach(chunk -> Structures.build(level, start, chunk));

		return BastionBookGameTests.bookPieces(start).getFirst().spot();
	}

	// Opens the space a sculk patch origin has in a generated city, where the terrain was carved away.
	private static void clearAround(ServerLevel level, BlockPos origin) {
		BlockPos.betweenClosedStream(origin.offset(-2, 0, -2), origin.offset(2, 3, 2))
			.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
	}

	private static List<BlockPos> remainingOnceChecked(ServerLevel level, BlockPos book) {
		return BookLocks.lockCheckedAgainstWorld(level, book).map(BookLocks.Lock::remainingKeys).orElseThrow();
	}

	private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}

	private static void placeKeeper(ServerLevel level, BlockPos spot) {
		level.setBlock(spot, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), Block.UPDATE_ALL);
	}

	// A ticked player standing on a block in an open space at the spot, with its chunk kept loaded.
	private static ServerPlayer standingAt(GameTestHelper helper, SubspaceArena arena, BlockPos feet, GameType gameType) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = arena.track(MockPlayers.survivalPlayer(helper));

		level.setChunkForced(feet.getX() >> 4, feet.getZ() >> 4, true);
		level.setBlock(feet.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(feet, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		player.setGameMode(gameType);
		SubspaceArena.teleport(player, Vec3.atBottomCenterOf(feet));

		return player;
	}

	// A Warden that stays put and opens its Subspace there.
	private static Warden stillWarden(ServerLevel level, Vec3 position) {
		Warden warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.MOB_SUMMONED);

		warden.setPos(position);
		warden.setNoAi(true);
		level.addFreshEntity(warden);

		return warden;
	}

	// The corner of the city's bounds at the book's height farthest from the book, a little inside.
	private static BlockPos farthestCorner(BoundingBox bounds, BlockPos book) {
		int x = book.getX() - bounds.minX() > bounds.maxX() - book.getX() ? bounds.minX() + 2 : bounds.maxX() - 2;
		int z = book.getZ() - bounds.minZ() > bounds.maxZ() - book.getZ() ? bounds.minZ() + 2 : bounds.maxZ() - 2;

		return new BlockPos(x, book.getY(), z);
	}

	// Runs the command as an operator at the position, adding each line it writes to the output.
	private static void runAt(ServerLevel level, Vec3 position, String command, List<Component> output) {
		CommandSource capture = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) {
				output.add(message);
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return false;
			}
		};
		MinecraftServer server = level.getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(capture).withLevel(level).withPosition(position), command);
	}

	private static String key(Component message) {
		return ((TranslatableContents) message.getContents()).getKey();
	}

	private static List<Object> args(Component message) {
		return List.of(((TranslatableContents) message.getContents()).getArgs());
	}

	private static void reloadTakenBooks(ServerLevel level) {
		DynamicOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Tag saved = TakenBooks.TYPE.codec().encodeStart(ops, level.getDataStorage().computeIfAbsent(TakenBooks.TYPE)).getOrThrow();

		level.getDataStorage().set(TakenBooks.TYPE, TakenBooks.TYPE.codec().parse(ops, saved).getOrThrow());
	}

	private static boolean holdsSwiftSneakBook(GameTestHelper helper, ServerPlayer player) {
		Holder<Enchantment> swiftSneak = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWIFT_SNEAK);
		ItemStack book = StructureBooks.levelOneBook(swiftSneak);

		return player.getInventory().contains(stack -> ItemStack.matches(stack, book));
	}

	private static List<Warden> wardensAround(ServerLevel level, BlockPos book) {
		AABB around = new AABB(book.getX(), level.getMinY(), book.getZ(), book.getX(), level.getMaxY(), book.getZ())
			.inflate(WARDENS_SEARCHED, 0.0, WARDENS_SEARCHED);

		return level.getEntitiesOfClass(Warden.class, around);
	}

	// A random source that always picks the first choice, so the farthest eligible origin is the first keeper.
	private static RandomSource farthestFirst() {
		return new LegacyRandomSource(0L) {
			@Override
			public int nextInt(int bound) {
				return 0;
			}
		};
	}

	// The block the given distance and direction (degrees, on the horizontal plane) from the book.
	private static BlockPos at(BlockPos book, double distance, double direction) {
		double radians = Math.toRadians(direction);

		return book.offset((int) Math.round(distance * Math.cos(radians)), 0, (int) Math.round(distance * Math.sin(radians)));
	}

	private static double distance(BlockPos book, BlockPos pos) {
		return Math.hypot(pos.getX() - book.getX(), pos.getZ() - book.getZ());
	}

	private static double direction(BlockPos book, BlockPos pos) {
		return Math.toDegrees(Math.atan2(pos.getZ() - book.getZ(), pos.getX() - book.getX()));
	}

	// The size of the turn between two directions, from 0 to 180 degrees.
	private static double turnBetween(double a, double b) {
		double turn = (((a - b) % 360.0) + 360.0) % 360.0;

		return turn > 180.0 ? 360.0 - turn : turn;
	}

	private static ChunkPos farChunk(int row, int index) {
		return new ChunkPos(FAR_CHUNK + index * CHUNKS_APART, FAR_CHUNK + row * CHUNKS_APART);
	}
}

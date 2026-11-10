package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import mapleflavored.structurebooks.StructureBookBlock;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluids;

/**
 * Every structure that doesn't wake holds exactly one book from its list, in its most important spot, and the book
 * follows its piece as the structure settles onto the terrain. Ocean Ruins and Pillager Outposts pick their enchantment
 * with equal weight.
 */
public class OrdinaryStructureBookGameTests {
	private static final int END_CITY_PLANS = 64;
	private static final int CHOICE_PLANS = 200;
	// A seeded count must land within this many binomial standard deviations of the expected count.
	private static final int END_CITY_HEIGHT = 100;
	private static final int FLOOD_TOP = 120;
	private static final int FLOODED_RUINS = 8;

	private static final List<ResourceKey<Structure>> PLACED = List.of(
		BuiltinStructures.DESERT_PYRAMID,
		BuiltinStructures.JUNGLE_TEMPLE,
		BuiltinStructures.IGLOO,
		BuiltinStructures.SHIPWRECK,
		BuiltinStructures.SHIPWRECK_BEACHED,
		BuiltinStructures.OCEAN_RUIN_COLD,
		BuiltinStructures.OCEAN_RUIN_WARM,
		BuiltinStructures.PILLAGER_OUTPOST,
		BuiltinStructures.ABANDONED_CAMP_FOREST,
		BuiltinStructures.ABANDONED_CAMP_SNOWY_TAIGA,
		BuiltinStructures.ABANDONED_CAMP_PALE_GARDEN
	);
	// These settle onto the terrain as they're built, so their books have to move with them.
	private static final List<ResourceKey<Structure>> SETTLING = List.of(
		BuiltinStructures.SHIPWRECK, BuiltinStructures.OCEAN_RUIN_WARM, BuiltinStructures.IGLOO, BuiltinStructures.JUNGLE_TEMPLE
	);

	@GameTest(maxTicks = 400)
	public void everyOrdinaryStructureHoldsOneBookFromItsList(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < PLACED.size(); i++) {
			ResourceKey<Structure> key = PLACED.get(i);
			StructureStart start = Structures.place(level, key, TestSites.of(helper).chunk(i));
			BlockPos book = Books.only(helper, start);
			ResourceKey<Enchantment> enchantment = Books.enchantmentAt(level, book);

			helper.assertTrue(StructureBookRules.enchantmentsFor(key).contains(enchantment), key.identifier() + " book holds " + enchantment.identifier());
			helper.assertValueEqual(book, Books.piece(start).spot(), key.identifier() + " book at its planned spot as built");

			// A beached wreck's chests, and so its book, can be buried in the beach like vanilla's.
			if (key != BuiltinStructures.SHIPWRECK_BEACHED) {
				helper.assertTrue(isOpen(level, book), key.identifier() + " book at " + book + " has open space beside it");
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aBookFollowsItsPieceAsTheStructureSettles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < SETTLING.size(); i++) {
			ResourceKey<Structure> key = SETTLING.get(i);
			StructureStart start = Structures.plan(level, key, TestSites.of(helper).chunk(i));
			int plannedY = Books.piece(start).spot().getY();

			Structures.chunks(start).forEach(chunk -> Structures.build(level, start, chunk));

			BlockPos built = Books.only(helper, start);
			// An Igloo saves where it was planned, so once loaded again its structure can't say where its book went.
			int loadedY = Books.piece(Structures.saveAndLoad(level, start)).spot().getY();

			helper.assertTrue(built.getY() != plannedY, key.identifier() + " book moved from its planned height");
			helper.assertTrue(isAtLandmark(level, key, built), key.identifier() + " book at " + built + " sits at its landmark");
			helper.assertValueEqual(Books.piece(start).spot(), built, key.identifier() + " book's spot once its structure is built");
			helper.assertValueEqual(loadedY, key == BuiltinStructures.IGLOO ? plannedY : built.getY(), key.identifier() + " book's height once its built structure is loaded again");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theBookIsPlacedRightWhicheverChunkIsBuiltFirstAndAcrossAReload(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (int i = 0; i < SETTLING.size(); i++) {
			ResourceKey<Structure> key = SETTLING.get(i);
			BlockPos bookChunkFirst = buildInOrder(helper, Structures.plan(level, key, TestSites.of(helper).chunk(2 * i)), true, key);
			BlockPos bookChunkLast = buildInOrder(helper, Structures.plan(level, key, TestSites.of(helper).chunk(2 * i + 1)), false, key);

			helper.assertTrue(isAtLandmark(level, key, bookChunkFirst), key.identifier() + " book built first sits at its landmark");
			helper.assertTrue(isAtLandmark(level, key, bookChunkLast), key.identifier() + " book built last, after a reload, sits at its landmark");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void anIglooBookIsInTheBasementWhenThereIsOne(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplateManager templates = level.getStructureTemplateManager();
		// An igloo with a basement has its lab and ladder pieces besides the igloo and the book.
		Optional<ChunkPos> withBasement = Structures.firstSiteWhere(level, BuiltinStructures.IGLOO, TestSites.of(helper), 0, start -> start.getPieces().size() > 2);
		Optional<ChunkPos> withoutBasement = Structures.firstSiteWhere(level, BuiltinStructures.IGLOO, TestSites.of(helper), 1, start -> start.getPieces().size() == 2);

		helper.assertTrue(withBasement.isPresent() && withoutBasement.isPresent(), "found igloos with and without a basement");

		StructureStart basementIgloo = Structures.place(level, BuiltinStructures.IGLOO, withBasement.orElseThrow());
		StructureStart plainIgloo = Structures.place(level, BuiltinStructures.IGLOO, withoutBasement.orElseThrow());

		helper.assertTrue(
			templatePiece(basementIgloo, templates, "igloo/bottom").getBoundingBox().isInside(Books.only(helper, basementIgloo)),
			"the book is in the basement"
		);
		helper.assertTrue(
			templatePiece(plainIgloo, templates, "igloo/top").getBoundingBox().isInside(Books.only(helper, plainIgloo)),
			"the book is in the igloo"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aJungleTempleBookFloatsAboveItsHiddenChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.JUNGLE_TEMPLE, TestSites.of(helper).chunk(0));
		BlockPos book = Books.only(helper, start);

		helper.assertTrue(start.getPieces().getFirst() instanceof JungleTemplePiece, "a jungle temple");
		helper.assertTrue(level.getBlockState(book.below()).is(Blocks.CHEST), "the hidden chest is under the book");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aDesertPyramidBookFloatsAboveTheTrap(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.place(level, BuiltinStructures.DESERT_PYRAMID, TestSites.of(helper).chunk(0));
		BlockPos book = Books.only(helper, start);

		helper.assertTrue(level.getBlockState(book.below()).is(Blocks.STONE_PRESSURE_PLATE), "the pressure plate is under the book");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void shipwreckAndOceanRuinBooksFloatAboveAChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ResourceKey<Structure>> keys = List.of(BuiltinStructures.SHIPWRECK, BuiltinStructures.OCEAN_RUIN_COLD, BuiltinStructures.OCEAN_RUIN_WARM);

		for (int i = 0; i < keys.size(); i++) {
			StructureStart start = Structures.place(level, keys.get(i), TestSites.of(helper).chunk(i));
			BlockPos book = Books.only(helper, start);

			helper.assertTrue(level.getBlockState(book.below()).is(Blocks.CHEST), keys.get(i).identifier() + " chest under the book");
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void outpostAndCampBooksFloatAtTheirLandmark(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart outpost = Structures.place(level, BuiltinStructures.PILLAGER_OUTPOST, TestSites.of(helper).chunk(0));
		BlockPos outpostBook = Books.only(helper, outpost);
		StructureStart camp = Structures.place(level, BuiltinStructures.ABANDONED_CAMP_FOREST, TestSites.of(helper).chunk(1));
		BlockPos campBook = Books.only(helper, camp);

		helper.assertTrue(level.getBlockState(campBook.below()).is(Blocks.CAMPFIRE), "the camp's book floats above its campfire");
		helper.assertTrue(isNear(level, outpostBook, 4, Blocks.CHEST), "the outpost's book is on the deck with the chest");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void underwaterBooksAreWaterloggedAndLeaveWaterWhenTaken(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestSites sites = TestSites.of(helper);
		// A ruin's book takes the place of whatever the ruin puts over its chest, often sand, so only some stand in water.
		Optional<BlockPos> inWater = IntStream.range(0, FLOODED_RUINS)
			.mapToObj(index -> buildFlooded(level, Structures.plan(level, BuiltinStructures.OCEAN_RUIN_WARM, sites.chunk(index))))
			.map(ruin -> Books.only(helper, ruin))
			.filter(built -> level.getBlockState(built).getValue(StructureBookBlock.WATERLOGGED))
			.findFirst();

		helper.assertTrue(inWater.isPresent(), "a book in " + FLOODED_RUINS + " flooded ruins is waterlogged");

		BlockPos book = inWater.orElseThrow();

		helper.assertTrue(StructureBooks.take(helper.makeMockServerPlayerInLevel(), book), "the book can be taken");
		helper.assertTrue(level.getFluidState(book).is(Fluids.WATER), "taking it leaves water");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void oceanRuinsAndOutpostsChooseTheirEnchantmentWithEqualWeight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		for (ResourceKey<Structure> key : List.of(BuiltinStructures.OCEAN_RUIN_WARM, BuiltinStructures.PILLAGER_OUTPOST)) {
			List<ResourceKey<Enchantment>> choices = StructureBookRules.enchantmentsFor(key);
			Map<ResourceKey<Enchantment>, Integer> counts = new HashMap<>();

			for (int i = 0; i < CHOICE_PLANS; i++) {
				ResourceKey<Enchantment> chosen = Books.piece(Structures.plan(level, key, TestSites.of(helper).chunk(i))).enchantment();

				counts.merge(chosen, 1, Integer::sum);
			}

			helper.assertTrue(choices.containsAll(counts.keySet()), key.identifier() + " chose only from its list: " + counts);

			for (ResourceKey<Enchantment> choice : choices) {
				Books.assertCount(helper, key.identifier() + " " + choice.identifier(), counts.getOrDefault(choice, 0), CHOICE_PLANS, 1.0 / choices.size());
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void anEndCityBookIsAtTheTopOfATower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplateManager templates = level.getStructureTemplateManager();
		Identifier lootRoom = Identifier.withDefaultNamespace("end_city/fat_tower_top");
		Identifier towerTop = Identifier.withDefaultNamespace("end_city/tower_top");
		// A loot room higher than the first one planned, so the book has to pick the highest.
		Predicate<StructureStart> hasAHigherLaterLootRoom = city -> {
			List<StructurePiece> lootRooms = piecesOf(city, templates, lootRoom);

			return !lootRooms.isEmpty() && highest(lootRooms) != lootRooms.getFirst();
		};
		Predicate<StructureStart> hasNoLootRoom = city -> piecesOf(city, templates, lootRoom).isEmpty();
		// Planned at different sites, so the two cities don't overlap.
		Optional<StructureStart> withLootRooms = firstEndCityWhere(level, TestSites.of(helper).chunk(0), hasAHigherLaterLootRoom);
		Optional<StructureStart> withoutLootRoom = firstEndCityWhere(level, TestSites.of(helper).chunk(1), hasNoLootRoom);

		helper.assertTrue(withLootRooms.isPresent() && withoutLootRoom.isPresent(), "planned a city whose highest loot room isn't its first, and one with none");

		StructureStart lootCity = withLootRooms.orElseThrow();
		StructureStart towerCity = withoutLootRoom.orElseThrow();

		Structures.chunks(lootCity).forEach(chunk -> Structures.build(level, lootCity, chunk));
		Structures.chunks(towerCity).forEach(chunk -> Structures.build(level, towerCity, chunk));

		BlockPos lootBook = Books.only(helper, lootCity);
		BlockPos towerBook = Books.only(helper, towerCity);

		helper.assertValueEqual(Books.enchantmentAt(level, lootBook), Enchantments.FEATHER_FALLING, "the city's book");
		helper.assertTrue(highest(piecesOf(lootCity, templates, lootRoom)).getBoundingBox().isInside(lootBook), "the book is in the highest loot room");
		helper.assertTrue(highest(piecesOf(towerCity, templates, towerTop)).getBoundingBox().isInside(towerBook), "the book is in the highest tower top");
		helper.assertTrue(isOpen(level, lootBook) && isOpen(level, towerBook), "both books have open space beside them");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aBookAddsNoTerrainAdaptation(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureStart camp = Structures.plan(level, BuiltinStructures.ABANDONED_CAMP_FOREST, TestSites.of(helper).chunk(0));
		BlockPos book = Books.piece(camp).spot();
		List<StructurePiece> piecesWithoutBook = camp.getPieces().stream().filter(piece -> !(piece instanceof StructureBookPiece)).toList();
		StructureStart campWithoutBook = new StructureStart(camp.getStructure(), camp.getChunkPos(), 0, new PiecesContainer(piecesWithoutBook));
		BlockPos belowBook = book.below();

		double withBook = beardAt(level, camp, ChunkPos.containing(book), belowBook);
		double withoutBook = beardAt(level, campWithoutBook, ChunkPos.containing(book), belowBook);

		helper.assertValueEqual(withBook, withoutBook, "terrain adaptation under the book");
		helper.succeed();
	}

	// Registers the start in the chunks the way generation does, then asks for the terrain adaptation it causes.
	private static double beardAt(ServerLevel level, StructureStart start, ChunkPos chunk, BlockPos point) {
		long startChunk = start.getChunkPos().pack();

		level.getChunk(start.getChunkPos().x(), start.getChunkPos().z()).setStartForStructure(start.getStructure(), start);
		level.getChunk(chunk.x(), chunk.z()).addReferenceForStructure(start.getStructure(), startChunk);

		return Beardifier.forStructuresInChunk(level.structureManager(), chunk).sampleValue(SamplerContext.EMPTY_UNCACHED, point.getX(), point.getY(), point.getZ());
	}

	private static BlockPos buildInOrder(GameTestHelper helper, StructureStart start, boolean bookChunkFirst, ResourceKey<Structure> key) {
		ServerLevel level = helper.getLevel();
		ChunkPos bookChunk = ChunkPos.containing(Books.piece(start).spot());
		List<ChunkPos> others = Structures.chunks(start).stream().filter(chunk -> !chunk.equals(bookChunk)).toList();

		if (bookChunkFirst) {
			Structures.build(level, start, bookChunk);
			others.forEach(chunk -> Structures.build(level, start, chunk));

			return Books.only(helper, start);
		}

		// Build every other chunk, save and reload the half-built structure, then build the book's chunk.
		others.forEach(chunk -> Structures.build(level, start, chunk));

		StructureStart reloaded = Structures.saveAndLoad(level, start);

		Structures.build(level, reloaded, bookChunk);

		return Books.only(helper, reloaded);
	}

	// Floods the ruin's area up past any ground it could settle on, then builds it, so it's built under water as in the
	// ocean.
	private static StructureStart buildFlooded(ServerLevel level, StructureStart ruin) {
		BoundingBox flooded = ruin.getBoundingBox().inflatedBy(2);

		Structures.chunks(ruin).forEach(chunk -> level.getChunk(chunk.x(), chunk.z()));
		BlockPos.betweenClosedStream(flooded.minX(), level.getMinY(), flooded.minZ(), flooded.maxX(), FLOOD_TOP, flooded.maxZ())
			.filter(pos -> level.getBlockState(pos).isAir())
			.forEach(pos -> level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2));
		Structures.chunks(ruin).forEach(chunk -> Structures.build(level, ruin, chunk));

		return ruin;
	}

	private static Optional<StructureStart> firstEndCityWhere(ServerLevel level, ChunkPos chunk, Predicate<StructureStart> wanted) {
		return IntStream.range(0, END_CITY_PLANS).mapToObj(seed -> planEndCity(level, seed, chunk)).filter(wanted).findFirst();
	}

	// The test world is flat, where End Cities never find ground high enough, so the city's pieces are planned
	// directly and given their book as generation would.
	private static StructureStart planEndCity(ServerLevel level, int seed, ChunkPos chunk) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
		BlockPos origin = new BlockPos(chunk.getBlockX(7), END_CITY_HEIGHT, chunk.getBlockZ(7));
		List<StructurePiece> pieces = new ArrayList<>();

		EndCityPieces.startHouseTower(level.getStructureTemplateManager(), origin, Rotation.getRandom(random), pieces, random);

		return Books.planWithBook(level, BuiltinStructures.END_CITY, chunk, pieces);
	}

	// Checks the book against the structure's own blocks, independent of how the book finds its spot.
	private static boolean isAtLandmark(ServerLevel level, ResourceKey<Structure> key, BlockPos book) {
		if (key != BuiltinStructures.IGLOO) return level.getBlockState(book.below()).is(Blocks.CHEST);

		// At head height in a room: the floor is two below, with only carpet or air between.
		BlockPos feet = book.below();
		BlockPos floor = book.below(2);

		return !level.getBlockState(feet).isCollisionShapeFullBlock(level, feet) && level.getBlockState(floor).isCollisionShapeFullBlock(level, floor);
	}

	private static List<StructurePiece> piecesOf(StructureStart start, StructureTemplateManager templates, Identifier location) {
		return start.getPieces().stream().filter(piece -> isTemplate(piece, templates, location)).toList();
	}

	private static StructurePiece highest(List<StructurePiece> pieces) {
		return pieces.stream().max(Comparator.comparingInt(piece -> piece.getBoundingBox().maxY())).orElseThrow();
	}

	private static TemplateStructurePiece templatePiece(StructureStart start, StructureTemplateManager templates, String path) {
		Identifier location = Identifier.withDefaultNamespace(path);

		return start.getPieces().stream()
			.filter(piece -> isTemplate(piece, templates, location))
			.map(TemplateStructurePiece.class::cast)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("no " + path + " piece"));
	}

	private static boolean isTemplate(StructurePiece piece, StructureTemplateManager templates, Identifier location) {
		return piece instanceof TemplateStructurePiece template && template.template() == templates.getOrCreate(location);
	}

	// A player can see and reach the book: some side of it isn't a full block.
	private static boolean isOpen(ServerLevel level, BlockPos book) {
		return Direction.stream().anyMatch(side -> {
			BlockPos neighbour = book.relative(side);

			return !level.getBlockState(neighbour).isCollisionShapeFullBlock(level, neighbour);
		});
	}

	private static boolean isNear(ServerLevel level, BlockPos pos, int radius, Block block) {
		return BlockPos.betweenClosedStream(new BoundingBox(pos).inflatedBy(radius)).anyMatch(near -> level.getBlockState(near).is(block));
	}

}

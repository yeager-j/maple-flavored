package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mapleflavored.structurebooks.StructureBookPiece;
import mapleflavored.structurebooks.StructureBookRules;
import mapleflavored.structurebooks.StructureBooks;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Ancient City's book: Swift Sneak, in front of the portal, and taking it always summons a Warden angry at the
 * player, near them through the summoning search, else standing under the book. With room for neither, the book stays.
 *
 * <p>A city's centre is built into terrain that's already generated, so its piece is first cleared, as worldgen's
 * terrain adaptation clears it. A player held near the build limit has nothing within the search's reach, and one on a
 * floor laid up there has room all around. Every take test opens a Subspace, or would if it went wrong, so each runs
 * alone in its batch with a {@link SubspaceArena} cleaning up.
 */
public class AncientCityBookGameTests {
	// Away from the other structure tests' areas, and far enough apart that no two cities overlap.
	private static final int FAR_CHUNK = 5000;
	private static final int CHUNKS_APART = 40;
	private static final int PLANS = 12;
	private static final int CENTRE_SCAN_ATTEMPTS = 40;
	private static final List<Identifier> CENTRES = List.of(
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_1"),
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_2"),
		Identifier.withDefaultNamespace("ancient_city/city_center/city_center_3")
	);
	// How far the book stands out from the portal's plane.
	private static final int BEFORE_THE_PORTAL = 3;
	// Below the build limit, far above any terrain, by more than the search's 12-block vertical reach.
	private static final int BELOW_THE_BUILD_LIMIT = 20;
	// Covers the search's near ring, 15 blocks across.
	private static final int FLOOR_REACH = 16;
	// Covers the search's 50 blocks across from a player above the book.
	private static final double WARDENS_SEARCHED = 120.0;
	private static final int MAX_ANGER = 150;
	private static final String NO_ROOM_FOR_WARDEN = "block.maple_flavored.structure_book.no_room_for_warden";

	private static final Set<BlockPos> TAKEN = ConcurrentHashMap.newKeySet();

	static {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> TAKEN.add(pos.immutable()));
	}

	@GameTest(maxTicks = 400)
	public void everyAncientCityIsPlannedWithOneSwiftSneakBookInItsCentre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		helper.assertValueEqual(StructureBookRules.enchantmentsFor(BuiltinStructures.ANCIENT_CITY), List.of(Enchantments.SWIFT_SNEAK), "the city's books");

		for (int i = 0; i < PLANS; i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(i));
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
	public void everyCityCentreHasItsBookInFrontOfThePortalWithRoomForAWarden(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Set<Identifier> seen = new HashSet<>();

		for (int i = 0; i < CENTRE_SCAN_ATTEMPTS && seen.size() < CENTRES.size(); i++) {
			StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, farChunk(PLANS + i));
			Identifier centre = BastionBookGameTests.templateOf(start.getPieces().getFirst()).orElseThrow();

			if (!seen.add(centre)) continue;

			BlockPos book = buildCentre(level, start);
			String where = "the book at " + book + " in " + centre;

			helper.assertTrue(level.getBlockState(book).is(StructureBooks.BLOCK), where + " is placed");
			assertInFrontOfThePortal(helper, level, start.getPieces().getFirst(), book, where);
			assertRoomForAWardenUnder(helper, level, book, where);
		}

		helper.assertValueEqual(seen, Set.copyOf(CENTRES), "the city centres checked");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_taken", maxTicks = 400)
	public void takingTheBookSummonsAnAngryWardenNearThePlayer(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		City city = placeCity(helper, 0);
		ServerPlayer player = arena.track(MockPlayers.survivalPlayer(helper));

		SubspaceArena.teleport(player, city.onAFloorAbove(helper.getLevel()));

		helper.assertTrue(StructureBooks.take(player, city.book()), "The book wasn't taken");
		assertTaken(helper, city, player);

		Warden warden = arena.track(onlyWarden(helper, city));
		double distance = Math.hypot(warden.getX() - player.getX(), warden.getZ() - player.getZ());

		helper.assertTrue(distance >= 5.0 && distance <= 15.0, "The Warden appeared " + distance + " blocks across from the player");
		assertAngryAt(helper, warden, player);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_blocked_search", maxTicks = 400)
	public void withNoRoomNearThePlayerTheWardenRisesUnderTheBook(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		City city = placeCity(helper, 1);
		ServerPlayer player = arena.track(MockPlayers.survivalPlayer(helper));

		SubspaceArena.teleport(player, city.inMidairAbove());

		helper.assertTrue(StructureBooks.take(player, city.book()), "The book wasn't taken");
		assertTaken(helper, city, player);

		Warden warden = arena.track(onlyWarden(helper, city));

		helper.assertValueEqual(warden.blockPosition(), city.book().below(), "where the Warden appeared");
		assertAngryAt(helper, warden, player);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_fully_blocked", maxTicks = 400)
	public void withNoRoomAnywhereTheBookStays(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		City city = placeCity(helper, 2);
		List<Component> overlay = new ArrayList<>();
		ServerPlayer player = arena.track(MockPlayers.survivalPlayerReadingOverlay(helper, overlay));

		helper.getLevel().setBlock(city.book().below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		SubspaceArena.teleport(player, city.inMidairAbove());

		helper.assertFalse(StructureBooks.take(player, city.book()), "The book was taken");
		assertRefused(helper, city, player);
		helper.assertValueEqual(overlay.stream().map(AncientCityBookGameTests::translationKey).toList(), List.of(NO_ROOM_FOR_WARDEN), "the messages shown");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_full_inventory", maxTicks = 400)
	public void aFullInventoryRefusesTheBookWithoutSummoning(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		City city = placeCity(helper, 3);
		ServerPlayer player = arena.track(MockPlayers.survivalPlayer(helper));
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			inventory.setItem(slot, new ItemStack(Items.DIRT));
		}

		SubspaceArena.teleport(player, city.onAFloorAbove(helper.getLevel()));

		helper.assertFalse(StructureBooks.take(player, city.book()), "The book was taken");
		assertRefused(helper, city, player);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_peaceful", maxTicks = 400)
	public void onPeacefulTheBookIsTakenWithoutAWarden(GameTestHelper helper) {
		takeWithoutAWarden(helper, 4);
	}

	@GameTest(environment = "maple_flavored_gametest:ancient_city_book_wardens_off", maxTicks = 400)
	public void withWardensOffTheBookIsTakenWithoutAWarden(GameTestHelper helper) {
		takeWithoutAWarden(helper, 5);
	}

	// A Warden summoned on Peaceful would despawn on its first tick, so Wardens are counted in the same tick as the take.
	private static void takeWithoutAWarden(GameTestHelper helper, int index) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		City city = placeCity(helper, index);
		ServerPlayer player = arena.track(MockPlayers.survivalPlayer(helper));

		SubspaceArena.teleport(player, city.onAFloorAbove(helper.getLevel()));

		helper.assertTrue(StructureBooks.take(player, city.book()), "The book wasn't taken");
		assertTaken(helper, city, player);
		helper.assertValueEqual(wardens(helper, city), List.of(), "the Wardens around the city's book");
		helper.succeed();
	}

	/**
	 * A built, registered Ancient City's book.
	 *
	 * @param sky the height a player is held at above the book
	 */
	private record City(BlockPos book, int sky) {
		/** High above the book, with nothing within the search's reach. */
		Vec3 inMidairAbove() {
			return Vec3.atBottomCenterOf(new BlockPos(book.getX(), sky, book.getZ()));
		}

		/** High above the book on a floor laid over the search's near ring. */
		Vec3 onAFloorAbove(ServerLevel level) {
			Vec3 standing = inMidairAbove();
			BlockPos floor = BlockPos.containing(standing).below();

			BlockPos.betweenClosed(floor.offset(-FLOOR_REACH, 0, -FLOOR_REACH), floor.offset(FLOOR_REACH, 0, FLOOR_REACH))
				.forEach(pos -> level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL));

			return standing;
		}
	}

	private static City placeCity(GameTestHelper helper, int index) {
		ServerLevel level = helper.getLevel();
		StructureStart start = Structures.plan(level, BuiltinStructures.ANCIENT_CITY, takeChunk(index));
		BlockPos book = buildCentre(level, start);

		Structures.register(level, start);

		return new City(book, level.getMaxY() - BELOW_THE_BUILD_LIMIT);
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

	// The portal frame stands in one vertical plane: the book is a few blocks out from it, within its span.
	private static void assertInFrontOfThePortal(GameTestHelper helper, ServerLevel level, StructurePiece centre, BlockPos book, String where) {
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
		helper.assertValueEqual(Math.abs(out), BEFORE_THE_PORTAL, "how far " + where + " is out from the portal");
		helper.assertTrue(along > alongMin && along < alongMax, where + " is within the portal's span");
		helper.assertTrue(book.getY() > span.minY() - BEFORE_THE_PORTAL * 3 && book.getY() < span.minY(), where + " is below the portal, on the plaza");
	}

	private static void assertRoomForAWardenUnder(GameTestHelper helper, ServerLevel level, BlockPos book, String where) {
		BlockPos feet = book.below();
		BlockState floor = level.getBlockState(feet.below());

		helper.assertTrue(Block.isFaceFull(floor.getCollisionShape(level, feet.below()), Direction.UP), "the floor under " + where + " is full");
		helper.assertTrue(
			level.noCollision(EntityTypes.WARDEN.getSpawnAABB(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5)),
			"a Warden has room under " + where
		);
	}

	private static void assertTaken(GameTestHelper helper, City city, ServerPlayer player) {
		helper.assertTrue(holdsSwiftSneakBook(helper, player), "The player doesn't hold Swift Sneak I");
		helper.assertTrue(helper.getLevel().getBlockState(city.book()).isAir(), "The book's block is still there");
		helper.assertTrue(TAKEN.contains(city.book()), "No taken event fired");
	}

	private static void assertRefused(GameTestHelper helper, City city, ServerPlayer player) {
		helper.assertFalse(holdsSwiftSneakBook(helper, player), "The player holds Swift Sneak I");
		helper.assertTrue(helper.getLevel().getBlockState(city.book()).is(StructureBooks.BLOCK), "The book isn't there");
		helper.assertFalse(TAKEN.contains(city.book()), "A taken event fired");
		helper.assertValueEqual(wardens(helper, city), List.of(), "the Wardens around the city's book");
		helper.assertValueEqual(Subspaces.inLevel(helper.getLevel()), List.of(), "the Subspaces in the level");
	}

	private static void assertAngryAt(GameTestHelper helper, Warden warden, ServerPlayer player) {
		Optional<LivingEntity> angryAt = warden.getEntityAngryAt();
		boolean ownsSubspace = Subspaces.inLevel(helper.getLevel()).stream().anyMatch(subspace -> subspace.boss().equals(warden.getUUID()));

		helper.assertValueEqual(angryAt, Optional.<LivingEntity>of(player), "whom the Warden is angry at");
		helper.assertValueEqual(warden.getAngerManagement().getActiveAnger(player), MAX_ANGER, "the Warden's anger at the player");
		helper.assertTrue(ownsSubspace, "The Warden opened no Subspace");
	}

	private static boolean holdsSwiftSneakBook(GameTestHelper helper, ServerPlayer player) {
		Holder<Enchantment> swiftSneak = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWIFT_SNEAK);
		ItemStack book = StructureBooks.levelOneBook(swiftSneak);

		return player.getInventory().contains(stack -> ItemStack.matches(stack, book));
	}

	private static Warden onlyWarden(GameTestHelper helper, City city) {
		List<Warden> wardens = wardens(helper, city);

		helper.assertValueEqual(wardens.size(), 1, "the Wardens around the city's book");

		return wardens.getFirst();
	}

	private static List<Warden> wardens(GameTestHelper helper, City city) {
		ServerLevel level = helper.getLevel();
		BlockPos book = city.book();
		AABB around = new AABB(book.getX(), level.getMinY(), book.getZ(), book.getX(), level.getMaxY(), book.getZ())
			.inflate(WARDENS_SEARCHED, 0.0, WARDENS_SEARCHED);

		return helper.getLevel().getEntitiesOfClass(Warden.class, around);
	}

	private static String translationKey(Component message) {
		return message.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : message.getString();
	}

	private static ChunkPos farChunk(int index) {
		return new ChunkPos(FAR_CHUNK + index * CHUNKS_APART, FAR_CHUNK);
	}

	// A row apart from the planning tests', one city per take test.
	private static ChunkPos takeChunk(int index) {
		return new ChunkPos(FAR_CHUNK + index * CHUNKS_APART, FAR_CHUNK + CHUNKS_APART);
	}
}

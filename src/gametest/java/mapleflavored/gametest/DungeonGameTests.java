package mapleflavored.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mapleflavored.items.MapleItems;
import mapleflavored.mixin.MineShaftCorridorAccessor;
import mapleflavored.spawners.Spawners;
import mapleflavored.structurebooks.SpawnerBooks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.phys.Vec3;

/**
 * Dungeons are tried 15 times per chunk above Y 0 and 8 below, and each dungeon's spawner holds the book that fights its
 * mob, Smite for zombies and skeletons and Bane of Arthropods for spiders. Every Mineshaft cave spider spawner holds Bane
 * of Arthropods too. The book drops with the spawner's Pith once the spawner is Cleared and broken.
 */
public class DungeonGameTests {
	// High in the air of the flat test world.
	private static final int ROOM_Y = 120;
	private static final int ROOM_SEEDS = 8;
	private static final int ZOMBIE_HEIGHT = 20;
	// Not every Mineshaft has a spider corridor, so several are planned.
	private static final int MINESHAFT_PLANS = 40;
	private static final int CORRIDOR_Y = 100;
	private static final long CORRIDOR_SEED = 21L;
	private static final int BOOK_ROLLS = 200;
	private static final long BOOK_SEED = 21L;

	@GameTest(maxTicks = 400)
	public void aDungeonsClearedSpawnerDropsTheBookForItsMobWithItsPith(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Set<ResourceKey<Enchantment>> booksSeen = new HashSet<>();

		for (int seed = 0; seed < ROOM_SEEDS; seed++) {
			BlockPos origin = TestSites.of(helper).chunk(seed).getMiddleBlockPosition(ROOM_Y);

			buildRoomShell(level, origin);
			helper.assertTrue(
				new MonsterRoomFeature().place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin),
				"the dungeon is placed"
			);

			SpawnerBlockEntity spawner = (SpawnerBlockEntity) level.getBlockEntity(origin);
			Optional<Holder<Enchantment>> book = heldBook(level, origin);

			EntityType<?> mob = spawnedMob(level, spawner);
			ResourceKey<Enchantment> expectedBook = mob == EntityTypes.SPIDER ? Enchantments.BANE_OF_ARTHROPODS : Enchantments.SMITE;

			helper.assertTrue(book.isPresent(), "the dungeon's spawner holds a book");
			helper.assertTrue(book.get().is(expectedBook), "a " + mob + " dungeon holds " + expectedBook.identifier());
			booksSeen.add(expectedBook);

			// Well above the room, so the zombies' own loot lands away from the spawner's drops.
			Vec3 aboveRoom = Vec3.atBottomCenterOf(origin.above(ZOMBIE_HEIGHT));

			SpawnerGameTests.killMarkedZombies(level, spawner, aboveRoom, 12);
			helper.assertTrue(Spawners.isCleared(level, origin), "the dungeon's spawner is Cleared");

			ServerPlayer player = MockPlayers.survivalPlayerHolding(helper, Items.IRON_PICKAXE);
			// Rolled as a pickaxe break rolls them. Item entities can't be found this far out, where no entities load.
			List<ItemStack> drops = Block.getDrops(level.getBlockState(origin), level, origin, spawner, player, player.getMainHandItem());
			ItemEnchantments expected = levelOne(book.get());

			helper.assertValueEqual(drops.size(), 2, "drops");
			helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(MapleItems.PITH)), "the Pith drops");
			helper.assertTrue(
				drops.stream().anyMatch(stack -> stack.is(Items.ENCHANTED_BOOK) && expected.equals(stack.get(DataComponents.STORED_ENCHANTMENTS))),
				"the held book drops at level I"
			);
		}

		helper.assertValueEqual(booksSeen.size(), 2, "kinds of dungeon book placed");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void aMineshaftCaveSpiderSpawnerHoldsBaneOfArthropods(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestSites sites = TestSites.of(helper);
		StructurePiece corridor = firstSpiderCorridor(level, sites);
		BoundingBox planned = corridor.getBoundingBox();
		BlockPos corner = sites.chunk(0).getMiddleBlockPosition(CORRIDOR_Y);

		corridor.move(corner.getX() - planned.minX(), corner.getY() - planned.minY(), corner.getZ() - planned.minZ());

		BoundingBox site = corridor.getBoundingBox();

		// The corridor places its spawner only underground, below the worldgen heightmap, which the thin test world
		// doesn't reach. Solid stone around and above it stands in for the ground.
		for (BlockPos pos : BlockPos.betweenClosed(site.minX() - 2, site.minY() - 2, site.minZ() - 2, site.maxX() + 2, site.maxY() + 4, site.maxZ() + 2)) {
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		}

		for (ChunkPos chunk : ChunkPos.rangeClosed(new ChunkPos(site.minX() >> 4, site.minZ() >> 4), new ChunkPos(site.maxX() >> 4, site.maxZ() >> 4)).toList()) {
			BoundingBox chunkBox = new BoundingBox(
				chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(), chunk.getMaxBlockZ()
			);

			corridor.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(CORRIDOR_SEED), chunkBox, chunk, site.getCenter());
		}

		List<SpawnerBlockEntity> spawners = BlockPos.betweenClosedStream(site)
			.map(level::getBlockEntity)
			.filter(SpawnerBlockEntity.class::isInstance)
			.map(SpawnerBlockEntity.class::cast)
			.toList();

		helper.assertValueEqual(spawners.size(), 1, "cave spider spawners in the corridor");

		SpawnerBlockEntity spawner = spawners.getFirst();
		Optional<Holder<Enchantment>> book = heldBook(level, spawner.getBlockPos());

		helper.assertValueEqual(spawnedMob(level, spawner), EntityTypes.CAVE_SPIDER, "the Mineshaft spawner's mob");
		helper.assertTrue(book.isPresent() && book.get().is(Enchantments.BANE_OF_ARTHROPODS), "the cave spider spawner holds Bane of Arthropods");
		helper.succeed();
	}

	@GameTest
	public void theDungeonBookFightsTheDungeonsMob(GameTestHelper helper) {
		RandomSource random = RandomSource.create(BOOK_SEED);

		helper.assertValueEqual(SpawnerBooks.forMob(EntityTypes.ZOMBIE, random), Enchantments.SMITE, "a zombie dungeon's book");
		helper.assertValueEqual(SpawnerBooks.forMob(EntityTypes.SKELETON, random), Enchantments.SMITE, "a skeleton dungeon's book");
		helper.assertValueEqual(SpawnerBooks.forMob(EntityTypes.SPIDER, random), Enchantments.BANE_OF_ARTHROPODS, "a spider dungeon's book");
		helper.assertValueEqual(SpawnerBooks.forMob(EntityTypes.CAVE_SPIDER, random), Enchantments.BANE_OF_ARTHROPODS, "a cave spider dungeon's book");
		helper.succeed();
	}

	@GameTest
	public void aMobNeitherBookFightsGetsEitherWithEqualWeight(GameTestHelper helper) {
		ResourceKey<Enchantment> first = SpawnerBooks.ENCHANTMENTS.getFirst();
		int firstCount = 0;

		RandomSource random = RandomSource.create(BOOK_SEED);

		for (int roll = 0; roll < BOOK_ROLLS; roll++) {
			ResourceKey<Enchantment> chosen = SpawnerBooks.forMob(EntityTypes.BLAZE, random);

			helper.assertTrue(SpawnerBooks.ENCHANTMENTS.contains(chosen), chosen + " is a dungeon enchantment");

			if (chosen == first) firstCount++;
		}

		LootRolls.assertRate(helper, first + " chosen", firstCount, BOOK_ROLLS, 0.5);
		helper.succeed();
	}

	@GameTest
	public void dungeonsAreTriedFifteenTimesAboveYZeroAndEightBelow(GameTestHelper helper) {
		assertDungeonPlacement(helper, CavePlacements.MONSTER_ROOM, 15, UniformHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.top()));
		assertDungeonPlacement(
			helper, CavePlacements.MONSTER_ROOM_DEEP, 8, UniformHeight.of(VerticalAnchor.aboveBottom(6), VerticalAnchor.absolute(-1))
		);
		helper.succeed();
	}

	private static void assertDungeonPlacement(GameTestHelper helper, ResourceKey<PlacedFeature> key, int attempts, HeightProvider height) {
		PlacedFeature placed = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(key).value();
		List<CountPlacement> counts = placed.placement().stream().filter(CountPlacement.class::isInstance).map(CountPlacement.class::cast).toList();
		List<HeightRangePlacement> heights = placed.placement().stream()
			.filter(HeightRangePlacement.class::isInstance)
			.map(HeightRangePlacement.class::cast)
			.toList();

		helper.assertValueEqual(counts.size(), 1, key.identifier() + " count placements");
		helper.assertValueEqual(counts.getFirst().count().minInclusive(), attempts, key.identifier() + " fewest attempts");
		helper.assertValueEqual(counts.getFirst().count().maxInclusive(), attempts, key.identifier() + " most attempts");
		helper.assertValueEqual(heights.size(), 1, key.identifier() + " height placements");
		helper.assertValueEqual(encode(heights.getFirst().height()), encode(height), key.identifier() + " height range");
	}

	private static StructurePiece firstSpiderCorridor(ServerLevel level, TestSites sites) {
		for (int i = 0; i < MINESHAFT_PLANS; i++) {
			Optional<StructurePiece> corridor = Structures.plan(level, BuiltinStructures.MINESHAFT, sites.chunk(i)).getPieces().stream()
				.filter(piece -> piece instanceof MineshaftPieces.MineShaftCorridor && ((MineShaftCorridorAccessor) piece).isSpiderCorridor())
				.findFirst();

			if (corridor.isPresent()) return corridor.get();
		}

		throw new IllegalStateException("no spider corridor in " + MINESHAFT_PLANS + " planned Mineshafts");
	}

	// The enchantment of the book the spawner holds as its extra drop.
	private static Optional<Holder<Enchantment>> heldBook(ServerLevel level, BlockPos pos) {
		return Spawners.clearing(level, pos)
			.flatMap(clearing -> clearing.heldDrop())
			.filter(drop -> drop.is(Items.ENCHANTED_BOOK))
			.flatMap(drop -> drop.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream().findFirst());
	}

	private static EntityType<?> spawnedMob(ServerLevel level, SpawnerBlockEntity spawner) {
		CompoundTag spawnData = spawner.saveCustomOnly(level.registryAccess()).getCompoundOrEmpty("SpawnData");

		return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(spawnData.getCompoundOrEmpty("entity").getStringOr("id", "")));
	}

	private static JsonElement encode(HeightProvider height) {
		return HeightProvider.CODEC.encodeStart(JsonOps.INSTANCE, height).getOrThrow();
	}

	// A solid box with a corridor through it, which the room needs: a floor resting on solid ground, a solid ceiling,
	// and 1 to 5 openings in its walls.
	private static void buildRoomShell(ServerLevel level, BlockPos origin) {
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 4, 4))) {
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}

		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, 0), origin.offset(4, 1, 0))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	private static ItemEnchantments levelOne(Holder<Enchantment> enchantment) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		enchantments.set(enchantment, 1);

		return enchantments.toImmutable();
	}
}

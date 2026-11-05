package mapleflavored.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mapleflavored.items.MapleItems;
import mapleflavored.spawners.Spawners;
import mapleflavored.structurebooks.DungeonBooks;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.Vec3;

/**
 * Dungeons are tried 15 times per chunk above Y 0 and 8 below, and each dungeon's spawner holds the book that fights its
 * mob, Smite for zombies and skeletons and Bane of Arthropods for spiders. It drops with the spawner's Pith once the
 * spawner is Cleared and broken.
 */
public class DungeonGameTests {
	// Far from every test's own area and from the other far-away tests, high in the air of the flat test world.
	private static final BlockPos ROOM_ORIGIN = new BlockPos(-48_000, 120, -48_000);
	private static final int ROOM_SEEDS = 8;
	private static final int ROOMS_APART = 32;
	private static final int ZOMBIE_HEIGHT = 20;
	private static final int BOOK_ROLLS = 200;
	private static final long BOOK_SEED = 21L;
	private static final double STANDARD_DEVIATIONS = 4.0;

	@GameTest(maxTicks = 400)
	public void aDungeonsClearedSpawnerDropsTheBookForItsMobWithItsPith(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Set<ResourceKey<Enchantment>> booksSeen = new HashSet<>();

		for (int seed = 0; seed < ROOM_SEEDS; seed++) {
			BlockPos origin = ROOM_ORIGIN.east(seed * ROOMS_APART);

			buildRoomShell(level, origin);
			helper.assertTrue(
				new MonsterRoomFeature().place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin),
				"the dungeon is placed"
			);

			SpawnerBlockEntity spawner = (SpawnerBlockEntity) level.getBlockEntity(origin);
			Optional<Holder<Enchantment>> book = Spawners.clearing(level, origin).flatMap(clearing -> clearing.heldBook());

			EntityType<?> mob = spawnedMob(level, spawner);
			ResourceKey<Enchantment> expectedBook = mob == EntityTypes.SPIDER ? Enchantments.BANE_OF_ARTHROPODS : Enchantments.SMITE;

			helper.assertTrue(book.isPresent(), "the dungeon's spawner holds a book");
			helper.assertTrue(book.get().is(expectedBook), "a " + mob + " dungeon holds " + expectedBook.identifier());
			booksSeen.add(expectedBook);

			killMarkedZombies(level, spawner, 12);
			helper.assertTrue(Spawners.isCleared(level, origin), "the dungeon's spawner is Cleared");

			ServerPlayer player = SpawnerGameTests.survivalPlayer(helper, Items.IRON_PICKAXE);
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

	@GameTest
	public void theDungeonBookFightsTheDungeonsMob(GameTestHelper helper) {
		RandomSource random = RandomSource.create(BOOK_SEED);

		helper.assertValueEqual(DungeonBooks.forMob(EntityTypes.ZOMBIE, random), Enchantments.SMITE, "a zombie dungeon's book");
		helper.assertValueEqual(DungeonBooks.forMob(EntityTypes.SKELETON, random), Enchantments.SMITE, "a skeleton dungeon's book");
		helper.assertValueEqual(DungeonBooks.forMob(EntityTypes.SPIDER, random), Enchantments.BANE_OF_ARTHROPODS, "a spider dungeon's book");
		helper.assertValueEqual(DungeonBooks.forMob(EntityTypes.CAVE_SPIDER, random), Enchantments.BANE_OF_ARTHROPODS, "a cave spider dungeon's book");
		helper.succeed();
	}

	@GameTest
	public void aMobNeitherBookFightsGetsEitherWithEqualWeight(GameTestHelper helper) {
		ResourceKey<Enchantment> first = DungeonBooks.ENCHANTMENTS.getFirst();
		int firstCount = 0;

		RandomSource random = RandomSource.create(BOOK_SEED);

		for (int roll = 0; roll < BOOK_ROLLS; roll++) {
			ResourceKey<Enchantment> chosen = DungeonBooks.forMob(EntityTypes.BLAZE, random);

			helper.assertTrue(DungeonBooks.ENCHANTMENTS.contains(chosen), chosen + " is a dungeon enchantment");

			if (chosen == first) firstCount++;
		}

		double half = BOOK_ROLLS / 2.0;
		double tolerance = STANDARD_DEVIATIONS * Math.sqrt(BOOK_ROLLS * 0.25);

		helper.assertTrue(Math.abs(firstCount - half) <= tolerance, first + " was chosen " + firstCount + " of " + BOOK_ROLLS + " times");
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

	private static void killMarkedZombies(ServerLevel level, SpawnerBlockEntity spawner, int count) {
		for (int i = 0; i < count; i++) {
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

			// Well above the room, so the zombie's own loot lands away from the spawner's drops.
			zombie.snapTo(Vec3.atBottomCenterOf(spawner.getBlockPos().above(ZOMBIE_HEIGHT)));
			level.addFreshEntity(zombie);
			Spawners.markSpawned(level, spawner.getBlockPos(), spawner.getSpawner(), zombie);
			zombie.kill(level);
		}
	}

	private static ItemEnchantments levelOne(Holder<Enchantment> enchantment) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		enchantments.set(enchantment, 1);

		return enchantments.toImmutable();
	}
}

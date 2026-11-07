package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import mapleflavored.mixin.SinglePoolElementAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.NoOpFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Every village is an Abandoned Village: vanilla's zombie village, rarer, with zombie villagers and no iron golems, no
 * hay bales or brewing stands, and chests without diamonds, obsidian or iron gear. Crops, beds, bells and workstations
 * stay. The igloo basement's villager is a zombie villager too.
 */
public class AbandonedVillageGameTests {
	// Clear of every other test's far-away chunks.
	private static final int FAR_CHUNK = 7000;
	private static final int CHUNKS_APART = 40;
	private static final int PLANNING_TRIES = 40;
	private static final BlockPos TEMPLATE_SITE = new BlockPos(-56_000, 100, 56_000);
	private static final int TEMPLATES_APART = 48;
	private static final int LOOT_ROLLS = 2000;
	// About 4 standard deviations of the mean over LOOT_ROLLS chests, and well under the gap to a table that replaces
	// the taken loot with other loot.
	private static final double STACKS_TOLERANCE = 0.15;
	// Each table's mean rolls times the share of vanilla's entry weight that is still loot. Toolsmith: 3 to 8 rolls,
	// 11 of 53 weight taken. Weaponsmith: 3 to 8 rolls with 43 of 107 taken, plus a 1 in 3 bundle. Armorer: 1 to 5
	// rolls, 1 of 8 taken.
	private static final List<ChestOdds> PICKED_OVER_CHESTS = List.of(
		new ChestOdds(BuiltInLootTables.VILLAGE_TOOLSMITH, 5.5 * 42 / 53),
		new ChestOdds(BuiltInLootTables.VILLAGE_WEAPONSMITH, 5.5 * 64 / 107 + 1.0 / 3),
		new ChestOdds(BuiltInLootTables.VILLAGE_ARMORER, 3.0 * 7 / 8)
	);
	private static final List<String> BIOMES = List.of("plains", "desert", "savanna", "snowy", "taiga");
	private static final List<ResourceKey<Structure>> VILLAGES = List.of(
		BuiltinStructures.VILLAGE_PLAINS,
		BuiltinStructures.VILLAGE_DESERT,
		BuiltinStructures.VILLAGE_SAVANNA,
		BuiltinStructures.VILLAGE_SNOWY,
		BuiltinStructures.VILLAGE_TAIGA
	);
	private static final Set<Item> TAKEN_LOOT = Set.of(
		Items.DIAMOND,
		Items.OBSIDIAN,
		Items.IRON_SWORD,
		Items.IRON_SHOVEL,
		Items.IRON_PICKAXE,
		Items.IRON_AXE,
		Items.IRON_HOE,
		Items.IRON_SPEAR,
		Items.IRON_HELMET,
		Items.IRON_CHESTPLATE,
		Items.IRON_LEGGINGS,
		Items.IRON_BOOTS
	);

	@GameTest
	public void villagesAreSpacedEightyChunksWithSeparationFifty(GameTestHelper helper) {
		StructurePlacement placement = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.STRUCTURE_SET)
			.getOrThrow(BuiltinStructureSets.VILLAGES)
			.value()
			.placement();

		helper.assertTrue(placement instanceof RandomSpreadStructurePlacement, "villages are placed by random spread");
		helper.assertValueEqual(((RandomSpreadStructurePlacement) placement).spacing(), 80, "village spacing");
		helper.assertValueEqual(((RandomSpreadStructurePlacement) placement).separation(), 50, "village separation");
		helper.succeed();
	}

	@GameTest
	public void villagesStartFromZombieTownCentresAndPlaceZombieVillagers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();

		// Plains zombie villages reach the plains villager pool through the houses they share with living villages.
		for (String biome : BIOMES) {
			assertOnlyZombieTemplates(helper, "village/" + biome + "/town_centers");
			assertOnlyZombieTemplates(helper, "village/" + biome + "/villagers");
		}

		Feature hayPile = level.registryAccess()
			.lookupOrThrow(Registries.FEATURE)
			.getOrThrow(ResourceKey.create(Registries.FEATURE, Identifier.withDefaultNamespace("pile_hay")))
			.value();

		helper.assertTrue(hayPile instanceof NoOpFeature, "the village hay pile feature places nothing, but is " + hayPile);
		helper.succeed();
	}

	@GameTest(maxTicks = 1200)
	public void everyVillageIsAbandonedButKeepsItsCropsBedsBellsAndWorkstations(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<BlockState> allBlocks = new ArrayList<>();

		for (int i = 0; i < VILLAGES.size(); i++) {
			ResourceKey<Structure> key = VILLAGES.get(i);
			ChunkPos chunk = new ChunkPos(FAR_CHUNK + i * CHUNKS_APART, FAR_CHUNK);
			StructureStart village = Structures.place(level, key, chunk);
			AABB box = AABB.of(village.getBoundingBox());
			List<BlockState> blocks = blocksIn(level, village);
			String name = key.identifier() + " at " + chunk;

			helper.assertTrue(isZombieVillage(village), name + " starts from a zombie town centre");
			helper.assertTrue(!level.getEntitiesOfClass(ZombieVillager.class, box).isEmpty(), name + " has zombie villagers");
			helper.assertValueEqual(level.getEntitiesOfClass(Villager.class, box).size(), 0, "villagers in " + name);
			helper.assertValueEqual(level.getEntitiesOfClass(IronGolem.class, box).size(), 0, "iron golems in " + name);
			helper.assertTrue(blocks.stream().noneMatch(state -> state.is(Blocks.HAY_BLOCK)), name + " has no hay bales");
			helper.assertTrue(blocks.stream().noneMatch(state -> state.is(Blocks.BREWING_STAND)), name + " has no brewing stands");
			allBlocks.addAll(blocks);
		}

		// Checked across the villages: vanilla's zombie taiga_meeting_point_2 has no bell, and not every village has a farm.
		helper.assertTrue(allBlocks.stream().anyMatch(state -> state.is(Blocks.BELL)), "the villages have bells");
		helper.assertTrue(allBlocks.stream().anyMatch(state -> state.is(BlockTags.BEDS)), "the villages have beds");
		helper.assertTrue(allBlocks.stream().anyMatch(state -> state.is(BlockTags.CROPS)), "the villages have crops");
		helper.assertTrue(allBlocks.stream().anyMatch(AbandonedVillageGameTests::isWorkstation), "the villages have workstations");
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void villagePiecesLoseHayAndBrewingStandsButOtherStructuresKeepThem(GameTestHelper helper) {
		assertPlacedWithout(helper, 0, "village/plains/houses/plains_stable_1", Blocks.HAY_BLOCK, false);
		assertPlacedWithout(helper, 1, "village/plains/houses/plains_temple_3", Blocks.BREWING_STAND, false);
		assertPlacedWithout(helper, 2, "pillager_outpost/feature_targets", Blocks.HAY_BLOCK, true);
		helper.succeed();
	}

	@GameTest
	public void villageChestsHoldNoDiamondsObsidianOrIronGear(GameTestHelper helper) {
		for (ChestOdds chest : PICKED_OVER_CHESTS) {
			LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(chest.table());
			RandomSource random = RandomSource.create(chest.table().identifier().hashCode());
			Set<Item> rolled = new HashSet<>();
			int stacks = 0;

			for (int i = 0; i < LOOT_ROLLS; i++) {
				List<ItemStack> drops = table.getRandomItems(chestParams(helper, table), random);

				drops.forEach(drop -> rolled.add(drop.getItem()));
				stacks += drops.size();
			}

			String name = chest.table().identifier().toString();
			double stacksPerChest = (double) stacks / LOOT_ROLLS;

			helper.assertTrue(rolled.stream().noneMatch(TAKEN_LOOT::contains), name + " gives none of " + TAKEN_LOOT + ", rolled " + rolled);
			helper.assertTrue(
				Math.abs(stacksPerChest - chest.stacksPerChest()) <= STACKS_TOLERANCE,
				name + " gives " + stacksPerChest + " stacks per chest, not the " + chest.stacksPerChest() + " of a roll that gives nothing in place of the taken loot"
			);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void theIglooBasementHoldsTwoZombieVillagersAndKeepsItsLab(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// An igloo with a basement has its lab and ladder pieces besides the igloo and the book.
		ChunkPos chunk = firstChunkWhere(level, BuiltinStructures.IGLOO, new ChunkPos(FAR_CHUNK, -FAR_CHUNK), start -> start.getPieces().size() > 2)
			.orElseThrow(() -> new IllegalStateException("no igloo with a basement in " + PLANNING_TRIES + " plans"));
		StructureStart igloo = Structures.place(level, BuiltinStructures.IGLOO, chunk);
		// The igloo settles onto the terrain as it's built, away from its planned height, so its whole column is read.
		BoundingBox planned = igloo.getBoundingBox();
		BoundingBox column = new BoundingBox(planned.minX(), level.getMinY(), planned.minZ(), planned.maxX(), level.getMaxY(), planned.maxZ());
		AABB box = AABB.of(column);
		List<ZombieVillager> zombies = level.getEntitiesOfClass(ZombieVillager.class, box);
		double zombieSpeed = DefaultAttributes.getSupplier(EntityTypes.ZOMBIE_VILLAGER).getBaseValue(Attributes.MOVEMENT_SPEED);

		List<BlockState> blocks = BlockPos.betweenClosedStream(column).map(level::getBlockState).toList();

		helper.assertValueEqual(zombies.size(), 2, "zombie villagers in the igloo basement");
		helper.assertValueEqual(level.getEntitiesOfClass(Villager.class, box).size(), 0, "villagers in the igloo basement");

		for (ZombieVillager zombie : zombies) {
			helper.assertValueEqual(zombie.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), zombieSpeed, "a basement zombie villager's speed");
			helper.assertTrue(zombie.isPersistenceRequired(), "a basement zombie villager stays");
		}

		helper.assertTrue(blocks.stream().anyMatch(state -> state.is(Blocks.BREWING_STAND)), "the igloo basement keeps its brewing stand");
		helper.assertTrue(blocks.stream().anyMatch(state -> state.is(Blocks.CHEST)), "the igloo basement keeps its chest");

		helper.succeed();
	}

	// Places the template as a jigsaw piece places it and checks the block against the raw template.
	private static void assertPlacedWithout(GameTestHelper helper, int index, String template, Block block, boolean kept) {
		ServerLevel level = helper.getLevel();
		StructureTemplateManager templates = level.getStructureTemplateManager();
		Identifier id = Identifier.withDefaultNamespace(template);
		SinglePoolElement element = StructurePoolElement.legacy(id.toString()).apply(StructureTemplatePool.Projection.RIGID);
		BlockPos origin = TEMPLATE_SITE.east(index * TEMPLATES_APART);
		BoundingBox box = element.getBoundingBox(templates, origin, Rotation.NONE);
		long inTemplate = templates.getOrCreate(id).filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();

		helper.assertTrue(inTemplate > 0, template + " holds " + block + " before placing");
		element.place(
			templates, level, level.structureManager(), level.getChunkSource().getGenerator(), origin, origin, Rotation.NONE,
			box, RandomSource.create(index), LiquidSettings.IGNORE_WATERLOGGING, false
		);

		long placed = BlockPos.betweenClosedStream(box).filter(pos -> level.getBlockState(pos).is(block)).count();

		helper.assertValueEqual(placed, kept ? inTemplate : 0L, block + " placed from " + template);
	}

	private static void assertOnlyZombieTemplates(GameTestHelper helper, String poolPath) {
		StructureTemplatePool pool = helper.getLevel().registryAccess()
			.lookupOrThrow(Registries.TEMPLATE_POOL)
			.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.withDefaultNamespace(poolPath)))
			.value();
		List<Optional<Identifier>> templates = pool.getTemplates().stream().map(entry -> templateOf(entry.getFirst())).toList();

		helper.assertTrue(!templates.isEmpty(), poolPath + " has templates");
		helper.assertTrue(templates.stream().allMatch(AbandonedVillageGameTests::isZombieTemplate), poolPath + " holds only zombie templates: " + templates);
	}

	private static boolean isZombieVillage(StructureStart start) {
		return start.getPieces().getFirst() instanceof PoolElementStructurePiece townCentre && isZombieTemplate(templateOf(townCentre.getElement()));
	}

	private static Optional<Identifier> templateOf(StructurePoolElement element) {
		return element instanceof SinglePoolElement single ? ((SinglePoolElementAccessor) single).template().left() : Optional.empty();
	}

	private static boolean isZombieTemplate(Optional<Identifier> template) {
		return template.filter(id -> id.getPath().contains("/zombie/")).isPresent();
	}

	private static boolean isWorkstation(BlockState state) {
		return PoiTypes.forState(state).filter(poi -> poi.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)).isPresent();
	}

	// Each piece's own blocks, so the open ground between them isn't read.
	private static List<BlockState> blocksIn(ServerLevel level, StructureStart start) {
		return start.getPieces().stream()
			.map(StructurePiece::getBoundingBox)
			.flatMap(BlockPos::betweenClosedStream)
			.map(level::getBlockState)
			.toList();
	}

	private static Optional<ChunkPos> firstChunkWhere(ServerLevel level, ResourceKey<Structure> key, ChunkPos first, Predicate<StructureStart> wanted) {
		return IntStream.range(0, PLANNING_TRIES)
			.mapToObj(i -> new ChunkPos(first.x(), first.z() + i * CHUNKS_APART))
			.filter(chunk -> wanted.test(Structures.plan(level, key, chunk)))
			.findFirst();
	}

	private static LootParams chestParams(GameTestHelper helper, LootTable table) {
		return new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.create(table.getParamSet());
	}

	private record ChestOdds(ResourceKey<LootTable> table, double stacksPerChest) {
	}
}

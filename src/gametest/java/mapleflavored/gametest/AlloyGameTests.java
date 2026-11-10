package mapleflavored.gametest;

import static mapleflavored.alloys.AlloyFurnaceSlots.ADDITION;
import static mapleflavored.alloys.AlloyFurnaceSlots.BASE;
import static mapleflavored.alloys.AlloyFurnaceSlots.COUNT;
import static mapleflavored.alloys.AlloyFurnaceSlots.FUEL;
import static mapleflavored.alloys.AlloyFurnaceSlots.RESULT;
import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyFurnaceBlockEntity;
import mapleflavored.alloys.AlloyFurnaceMenu;
import mapleflavored.alloys.AlloyingInput;
import mapleflavored.alloys.Alloys;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Materials and Alloys: the Piglin Brute's Ancient Ember, the Alloy Furnace and its hoppers, Alloy blocks and smelting an
 * Alloy ingot back, and the advancements, including But why?'s all-Alloy beacon.
 */
public class AlloyGameTests {
	private static final int LOOT_ROLLS = 2000;
	private static final long LOOT_SEED = 7L;
	private static final double DROP_RATE_TOLERANCE = 0.03;
	// Embers share every pool, count and weight with ancient debris, so their totals differ only by chance. Chests are
	// rolled more often than mobs, as their two-item entries make the totals noisier.
	private static final int CHEST_ROLLS = 10_000;
	private static final double CHEST_COUNT_TOLERANCE = 0.10;

	private static final BlockPos FURNACE = new BlockPos(1, 2, 1);

	private static final Identifier OBTAIN_ANCIENT_EMBER = Identifier.parse("maple_flavored:nether/obtain_ancient_ember");
	private static final Identifier SMELT_ALLOY_INGOT = Identifier.parse("maple_flavored:nether/smelt_alloy_ingot");
	private static final Identifier BUT_WHY = Identifier.parse("maple_flavored:nether/but_why");
	private static final Identifier BEACONATOR = Identifier.parse("minecraft:nether/create_full_beacon");

	private static final Map<Block, Alloy> STORAGE_BLOCKS = Map.of(
		Blocks.IRON_BLOCK, Alloy.STEEL,
		Blocks.GOLD_BLOCK, Alloy.ELECTRUM,
		Blocks.COPPER_BLOCK.weathering().unaffected(), Alloy.SHAKUDO,
		Blocks.DIAMOND_BLOCK, Alloy.ADAMANT,
		Blocks.EMERALD_BLOCK, Alloy.ORICHALCUM
	);

	// A beacon pyramid's layers, from the top: squares of side 3, 5, 7 and 9 under the beacon.
	private static final int PYRAMID_LEVELS = 4;
	// Outside the pyramid's 9 by 9 base, level with its bottom layer, inside vanilla's Beaconator box.
	private static final Vec3 BESIDE_THE_PYRAMID = new Vec3(6.0, 0.0, 0.0);

	@GameTest
	public void piglinBrutesDropAnAncientEmberOneTimeInFourWhenAPlayerKillsThem(GameTestHelper helper) {
		Mob brute = helper.spawnWithNoFreeWill(EntityTypes.PIGLIN_BRUTE, new BlockPos(1, 1, 1));
		ServerPlayer player = survivalPlayer(helper);

		helper.assertValueEqual(dropRate(helper, brute, null, Alloys.ANCIENT_EMBER), 0.0, "Ember drop rate with no player kill");
		assertNear(helper, dropRate(helper, brute, player, Alloys.ANCIENT_EMBER), 0.25, "Ember drop rate on a player kill");
		assertNear(helper, dropRate(helper, brute, player, MapleItems.PITH), 0.10, "Pith drop rate on a player kill");

		player.setItemInHand(InteractionHand.MAIN_HAND, lootingSword(helper, 3));
		assertNear(helper, dropRate(helper, brute, player, Alloys.ANCIENT_EMBER), 0.40, "Ember drop rate with Looting III");
		helper.succeed();
	}

	@GameTest
	public void bastionChestsHoldAncientEmbersAsOftenAsAncientDebris(GameTestHelper helper) {
		for (String chest : List.of("bastion_treasure", "bastion_other", "bastion_hoglin_stable")) {
			List<ItemStack> rolled = rollChest(helper, chest);
			int embers = count(rolled, Alloys.ANCIENT_EMBER);
			int debris = count(rolled, Items.ANCIENT_DEBRIS);

			helper.assertTrue(debris > 0, chest + " gives ancient debris");
			helper.assertTrue(
				Math.abs(embers - debris) <= debris * CHEST_COUNT_TOLERANCE,
				chest + " gives " + embers + " Embers to " + debris + " ancient debris"
			);
		}

		helper.assertValueEqual(count(rollChest(helper, "bastion_bridge"), Alloys.ANCIENT_EMBER), 0, "Embers from bastion_bridge, which has no debris");
		helper.succeed();
	}

	@GameTest
	public void eachStorageBlockAndANetheriteIngotMakeItsAlloy(GameTestHelper helper) {
		helper.assertValueEqual(Set.copyOf(STORAGE_BLOCKS.values()), Set.of(Alloy.values()), "the Alloys with a storage block");

		STORAGE_BLOCKS.forEach((block, alloy) ->
			helper.assertValueEqual(alloyOf(helper, Items.NETHERITE_INGOT, block.asItem()), alloy.ingot(), block + " makes")
		);

		for (Item addition : List.of(Items.NETHERITE_BLOCK, Items.COPPER_BLOCK.waxed().unaffected(), Items.COPPER_BLOCK.weathering().exposed(), Items.LAPIS_BLOCK, Items.RAW_IRON_BLOCK)) {
			helper.assertValueEqual(alloyOf(helper, Items.NETHERITE_INGOT, addition), Items.AIR, addition + " makes");
		}

		helper.assertValueEqual(alloyOf(helper, Items.DIAMOND, Items.IRON_BLOCK), Items.AIR, "a diamond and an iron block make");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void anAlloyIngotTakesTenSecondsAndOneOfEachInput(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 2, Items.IRON_BLOCK, 2);

		helper.runAfterDelay(198, () -> helper.assertTrue(furnace.getItem(RESULT).isEmpty(), "the ingot isn't done before 10 seconds"));
		helper.runAfterDelay(201, () -> {
			assertSlot(helper, furnace, RESULT, Alloy.STEEL.ingot(), 1);
			assertSlot(helper, furnace, BASE, Items.NETHERITE_INGOT, 1);
			assertSlot(helper, furnace, ADDITION, Items.IRON_BLOCK, 1);
			assertSlot(helper, furnace, FUEL, Alloys.ANCIENT_EMBER, 1);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 500)
	public void oneEmberMakesOneIngot(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 3, Items.GOLD_BLOCK, 1);

		helper.runAfterDelay(450, () -> {
			assertSlot(helper, furnace, RESULT, Alloy.ELECTRUM.ingot(), 1);
			assertSlot(helper, furnace, BASE, Items.NETHERITE_INGOT, 2);
			assertSlot(helper, furnace, ADDITION, Items.GOLD_BLOCK, 2);
			helper.assertTrue(furnace.getItem(FUEL).isEmpty(), "the Ember is used up");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 400)
	public void withoutAnEmberNothingSmelts(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 1, Items.IRON_BLOCK, 0);

		helper.runAfterDelay(300, () -> {
			helper.assertTrue(furnace.getItem(RESULT).isEmpty(), "nothing smelted");
			assertSlot(helper, furnace, BASE, Items.NETHERITE_INGOT, 1);
			assertSlot(helper, furnace, ADDITION, Items.IRON_BLOCK, 1);
			helper.assertFalse(helper.getBlockState(FURNACE).getValue(AbstractFurnaceBlock.LIT), "the furnace is lit");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 600)
	public void takingAnInputOutMidSmeltKeepsTheEmberAndStartsOver(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 1, Items.IRON_BLOCK, 1);

		helper.startSequence()
			.thenIdle(100)
			.thenExecute(() -> {
				furnace.removeItem(ADDITION, 1);
				helper.assertTrue(helper.getBlockState(FURNACE).getValue(AbstractFurnaceBlock.LIT), "the furnace was lit");
			})
			.thenIdle(50)
			.thenExecute(() -> {
				assertSlot(helper, furnace, FUEL, Alloys.ANCIENT_EMBER, 1);
				furnace.setItem(ADDITION, new ItemStack(Items.IRON_BLOCK));
			})
			.thenIdle(150)
			.thenExecute(() -> helper.assertTrue(furnace.getItem(RESULT).isEmpty(), "the smelt started over"))
			.thenIdle(55)
			.thenExecute(() -> assertSlot(helper, furnace, RESULT, Alloy.STEEL.ingot(), 1))
			.thenSucceed();
	}

	@GameTest(maxTicks = 600)
	public void swappingTheStorageBlockMidSmeltStartsOver(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 1, Items.IRON_BLOCK, 1);

		helper.startSequence()
			.thenIdle(100)
			.thenExecute(() -> furnace.setItem(ADDITION, new ItemStack(Items.GOLD_BLOCK)))
			.thenIdle(150)
			.thenExecute(() -> {
				helper.assertTrue(furnace.getItem(RESULT).isEmpty(), "the smelt started over");
				assertSlot(helper, furnace, FUEL, Alloys.ANCIENT_EMBER, 1);
			})
			.thenIdle(55)
			.thenExecute(() -> assertSlot(helper, furnace, RESULT, Alloy.ELECTRUM.ingot(), 1))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void aFullOrDifferentResultSlotKeepsEveryInput(GameTestHelper helper) {
		AlloyFurnaceBlockEntity full = furnace(helper);
		BlockPos otherPos = FURNACE.east(2);

		helper.setBlock(otherPos, Alloys.ALLOY_FURNACE);

		AlloyFurnaceBlockEntity different = helper.getBlockEntity(otherPos, AlloyFurnaceBlockEntity.class);

		fill(full, 1, Items.IRON_BLOCK, 1);
		full.setItem(RESULT, new ItemStack(Alloy.STEEL.ingot(), 64));
		fill(different, 1, Items.IRON_BLOCK, 1);
		different.setItem(RESULT, new ItemStack(Alloy.ELECTRUM.ingot()));

		helper.runAfterDelay(300, () -> {
			for (AlloyFurnaceBlockEntity furnace : List.of(full, different)) {
				assertSlot(helper, furnace, BASE, Items.NETHERITE_INGOT, 1);
				assertSlot(helper, furnace, ADDITION, Items.IRON_BLOCK, 1);
				assertSlot(helper, furnace, FUEL, Alloys.ANCIENT_EMBER, 1);
			}

			assertSlot(helper, full, RESULT, Alloy.STEEL.ingot(), 64);
			assertSlot(helper, different, RESULT, Alloy.ELECTRUM.ingot(), 1);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300)
	public void aSavedFurnaceKeepsItsItemsAndProgress(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(FURNACE);
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 1, Items.DIAMOND_BLOCK, 1);

		helper.startSequence()
			.thenIdle(100)
			.thenExecute(() -> {
				CompoundTag saved = furnace.saveWithoutMetadata(level.registryAccess());
				AlloyFurnaceBlockEntity loaded = new AlloyFurnaceBlockEntity(pos, level.getBlockState(pos));

				// Swapped in the way a chunk loading again makes a fresh block entity from its saved data.
				level.removeBlockEntity(pos);
				loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
				level.setBlockEntity(loaded);
			})
			.thenIdle(103)
			.thenExecute(() -> {
				AlloyFurnaceBlockEntity loaded = helper.getBlockEntity(FURNACE, AlloyFurnaceBlockEntity.class);

				helper.assertFalse(loaded == furnace, "the furnace was reloaded");
				assertSlot(helper, loaded, RESULT, Alloy.ADAMANT.ingot(), 1);
				helper.assertTrue(loaded.getItem(BASE).isEmpty(), "the Netherite Ingot is used up");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void aBrokenFurnaceDropsItselfAndItsContentsOnce(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		fill(furnace, 1, Items.IRON_BLOCK, 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));

		helper.runAfterDelay(50, () -> {
			player.gameMode.destroyBlock(helper.absolutePos(FURNACE));

			List<ItemStack> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FURNACE)).inflate(2.0))
				.stream()
				.map(ItemEntity::getItem)
				.toList();

			for (Item item : List.of(Alloys.ALLOY_FURNACE.asItem(), Items.NETHERITE_INGOT, Items.IRON_BLOCK, Alloys.ANCIENT_EMBER)) {
				helper.assertValueEqual(count(drops, item), 1, item + " dropped");
			}

			helper.succeed();
		});
	}

	@GameTest
	public void theComparatorReadsTheFurnace(GameTestHelper helper) {
		AlloyFurnaceBlockEntity furnace = furnace(helper);
		BlockPos pos = helper.absolutePos(FURNACE);

		helper.assertValueEqual(helper.getBlockState(FURNACE).getAnalogOutputSignal(helper.getLevel(), pos, Direction.NORTH), 0, "an empty furnace's signal");

		fill(furnace, 16, Items.IRON_BLOCK, 16);

		helper.assertTrue(helper.getBlockState(FURNACE).getAnalogOutputSignal(helper.getLevel(), pos, Direction.NORTH) > 0, "a filled furnace gives a signal");
		helper.succeed();
	}

	@GameTest(maxTicks = 500)
	public void hoppersFeedTheFurnaceAndTakeItsAlloy(GameTestHelper helper) {
		BlockPos chest = new BlockPos(1, 0, 1);
		BlockPos below = new BlockPos(1, 1, 1);
		BlockPos above = new BlockPos(1, 3, 1);
		BlockPos side = new BlockPos(2, 2, 1);

		helper.setBlock(chest, Blocks.CHEST);
		helper.setBlock(below, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		furnace(helper);
		helper.setBlock(above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(side, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));

		HopperBlockEntity top = helper.getBlockEntity(above, HopperBlockEntity.class);

		top.setItem(0, new ItemStack(Items.DIRT));
		top.setItem(1, new ItemStack(Items.NETHERITE_INGOT));
		top.setItem(2, new ItemStack(Items.IRON_BLOCK));
		helper.getBlockEntity(side, HopperBlockEntity.class).setItem(0, new ItemStack(Alloys.ANCIENT_EMBER));

		helper.succeedWhen(() -> {
			Container output = helper.getBlockEntity(chest, ChestBlockEntity.class);

			helper.assertValueEqual(output.countItem(Alloy.STEEL.ingot()), 1, "Steel in the chest");
			helper.assertValueEqual(top.countItem(Items.DIRT), 1, "dirt left in the top hopper");
			helper.assertValueEqual(output.countItem(Items.DIRT), 0, "dirt in the chest");
		});
	}

	@GameTest
	public void shiftClickingPutsEachInputInItsSlot(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		AlloyFurnaceBlockEntity furnace = furnace(helper);

		player.openMenu(furnace);

		if (!(player.containerMenu instanceof AlloyFurnaceMenu menu)) {
			throw helper.assertionException(Component.literal("the furnace opened " + player.containerMenu));
		}

		// Menu slots past the furnace's start with the player's main inventory, which starts at inventory slot 9.
		List<Item> items = List.of(Items.NETHERITE_INGOT, Items.EMERALD_BLOCK, Alloys.ANCIENT_EMBER, Items.DIRT);

		for (int i = 0; i < items.size(); i++) {
			player.getInventory().setItem(9 + i, new ItemStack(items.get(i)));
			menu.clicked(COUNT + i, 0, ContainerInput.QUICK_MOVE, player);
		}

		assertSlot(helper, furnace, BASE, Items.NETHERITE_INGOT, 1);
		assertSlot(helper, furnace, ADDITION, Items.EMERALD_BLOCK, 1);
		assertSlot(helper, furnace, FUEL, Alloys.ANCIENT_EMBER, 1);
		helper.assertTrue(furnace.getItem(RESULT).isEmpty(), "nothing went in the result slot");
		helper.assertValueEqual(player.getInventory().countItem(Items.DIRT), 1, "dirt kept by the player");
		helper.succeed();
	}

	@GameTest
	public void furnacesAndBlastFurnacesSmeltAlloysBackIntoNetherite(GameTestHelper helper) {
		for (Alloy alloy : Alloy.values()) {
			SingleRecipeInput input = new SingleRecipeInput(new ItemStack(alloy.ingot()));

			helper.assertValueEqual(cook(helper, RecipeType.SMELTING, input), Items.NETHERITE_INGOT, alloy + " smelts into");
			helper.assertValueEqual(cook(helper, RecipeType.BLASTING, input), Items.NETHERITE_INGOT, alloy + " blasts into");
			helper.assertValueEqual(cook(helper, RecipeType.SMOKING, input), Items.AIR, alloy + " smokes into");
		}

		helper.succeed();
	}

	@GameTest
	public void alloyBlocksAndTheFurnaceCraft(GameTestHelper helper) {
		ItemStack gold = new ItemStack(Items.GOLD_INGOT);
		ItemStack bricks = new ItemStack(Items.POLISHED_BLACKSTONE_BRICKS);
		List<ItemStack> furnaceGrid = List.of(gold, gold, gold, gold, new ItemStack(Items.BLAST_FURNACE), gold, bricks, bricks, bricks);

		assertCrafts(helper, CraftingInput.of(3, 3, furnaceGrid), Alloys.ALLOY_FURNACE.asItem(), 1);

		for (Alloy alloy : Alloy.values()) {
			assertCrafts(helper, CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(alloy.ingot()))), alloy.block().asItem(), 1);
			assertCrafts(helper, CraftingInput.of(1, 1, List.of(new ItemStack(alloy.block()))), alloy.ingot(), 9);
		}

		helper.succeed();
	}

	@GameTest
	public void gettingAnEmberAndAnAlloyIngotGrantsTheirAdvancements(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		give(player, Alloys.ANCIENT_EMBER);
		helper.assertTrue(isDone(helper, player, OBTAIN_ANCIENT_EMBER), "getting an Ember grants its advancement");
		helper.assertFalse(isDone(helper, player, SMELT_ALLOY_INGOT), "getting an Ember grants Smelt an Alloy ingot");

		give(player, Alloy.SHAKUDO.ingot());
		helper.assertTrue(isDone(helper, player, SMELT_ALLOY_INGOT), "getting an Alloy ingot grants its advancement");

		assertAdvancement(helper, OBTAIN_ANCIENT_EMBER, "minecraft:nether/find_bastion", AdvancementType.TASK, false, 0);
		assertAdvancement(helper, SMELT_ALLOY_INGOT, OBTAIN_ANCIENT_EMBER.toString(), AdvancementType.TASK, false, 0);
		assertAdvancement(helper, BUT_WHY, BEACONATOR.toString(), AdvancementType.CHALLENGE, true, 100);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:alloy_beacon_full", maxTicks = 300)
	public void aFullAlloyBeaconGrantsButWhy(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer player = arena.player(BESIDE_THE_PYRAMID, GameType.SURVIVAL);
		List<BlockPos> pyramid = buildBeacon(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(isDone(helper, player, BEACONATOR), "Beaconator is granted");
			helper.assertTrue(isDone(helper, player, BUT_WHY), "But why? is granted");
			clear(arena.level(), pyramid);
		});
	}

	@GameTest(environment = "maple_flavored_gametest:alloy_beacon_upgraded", maxTicks = 500)
	public void finishingAnAlloyPyramidUnderALitBeaconGrantsButWhy(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer player = arena.player(BESIDE_THE_PYRAMID, GameType.SURVIVAL);
		List<BlockPos> pyramid = buildBeacon(arena);
		BlockPos corner = pyramid.getLast();

		arena.level().setBlockAndUpdate(corner, Blocks.IRON_BLOCK.defaultBlockState());

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(isDone(helper, player, BEACONATOR), "Beaconator is granted"))
			.thenExecute(() -> {
				helper.assertFalse(isDone(helper, player, BUT_WHY), "But why? is granted with an iron block in the pyramid");
				arena.level().setBlockAndUpdate(corner, Alloy.STEEL.block().defaultBlockState());
			})
			.thenWaitUntil(() -> helper.assertTrue(isDone(helper, player, BUT_WHY), "But why? is granted once the pyramid is all Alloy"))
			.thenExecute(() -> clear(arena.level(), pyramid))
			.thenSucceed();
	}

	// A level 4 pyramid of every Alloy in turn, with a beacon on top at the arena's centre, open to the sky. Returns the
	// pyramid's and beacon's blocks, beacon first and the pyramid's last corner last.
	private static List<BlockPos> buildBeacon(SubspaceArena arena) {
		ServerLevel level = arena.level();
		Alloy[] alloys = Alloy.values();
		BlockPos beacon = arena.blockAt(0, PYRAMID_LEVELS, 0);
		List<BlockPos> blocks = new ArrayList<>(List.of(beacon));
		int placed = 0;

		for (int layer = 1; layer <= PYRAMID_LEVELS; layer++) {
			for (int dx = -layer; dx <= layer; dx++) {
				for (int dz = -layer; dz <= layer; dz++) {
					BlockPos pos = beacon.offset(dx, -layer, dz);

					level.setBlockAndUpdate(pos, alloys[placed++ % alloys.length].block().defaultBlockState());
					blocks.add(pos);
				}
			}
		}

		level.setBlockAndUpdate(beacon, Blocks.BEACON.defaultBlockState());

		return blocks;
	}

	private static void clear(ServerLevel level, List<BlockPos> blocks) {
		blocks.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
	}

	private static AlloyFurnaceBlockEntity furnace(GameTestHelper helper) {
		helper.setBlock(FURNACE, Alloys.ALLOY_FURNACE);

		return helper.getBlockEntity(FURNACE, AlloyFurnaceBlockEntity.class);
	}

	private static void fill(AlloyFurnaceBlockEntity furnace, int ingots, Item storageBlock, int embers) {
		furnace.setItem(BASE, new ItemStack(Items.NETHERITE_INGOT, ingots));
		furnace.setItem(ADDITION, new ItemStack(storageBlock, ingots));
		furnace.setItem(FUEL, embers == 0 ? ItemStack.EMPTY : new ItemStack(Alloys.ANCIENT_EMBER, embers));
	}

	private static void assertSlot(GameTestHelper helper, Container furnace, int slot, Item item, int count) {
		ItemStack stack = furnace.getItem(slot);

		helper.assertValueEqual(stack.getItem(), item, "the item in slot " + slot);
		helper.assertValueEqual(stack.getCount(), count, "the count in slot " + slot);
	}

	private static Item alloyOf(GameTestHelper helper, Item base, Item addition) {
		ServerLevel level = helper.getLevel();
		AlloyingInput input = new AlloyingInput(new ItemStack(base), new ItemStack(addition));

		return level.getServer().getRecipeManager().getRecipeFor(Alloys.ALLOYING, input, level)
			.map(recipe -> recipe.value().assemble(input).getItem())
			.orElse(Items.AIR);
	}

	private static Item cook(GameTestHelper helper, RecipeType<? extends AbstractCookingRecipe> type, SingleRecipeInput input) {
		ServerLevel level = helper.getLevel();

		return level.getServer().getRecipeManager().getRecipeFor(type, input, level)
			.map(recipe -> recipe.value().assemble(input).getItem())
			.orElse(Items.AIR);
	}

	private static void assertCrafts(GameTestHelper helper, CraftingInput input, Item item, int count) {
		ServerLevel level = helper.getLevel();
		ItemStack result = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(recipe -> recipe.value().assemble(input))
			.orElse(ItemStack.EMPTY);

		helper.assertValueEqual(result.getItem(), item, "crafted item");
		helper.assertValueEqual(result.getCount(), count, "crafted count of " + item);
	}

	private static ItemStack lootingSword(GameTestHelper helper, int level) {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), level);

		return sword;
	}

	// Rolls the mob's loot table the way LivingEntity.dropFromLootTable does, killed by the player (or by nothing) with
	// what they hold, from one seeded random so the result is fixed.
	private static double dropRate(GameTestHelper helper, Mob mob, ServerPlayer killer, Item item) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable().orElseThrow());
		LootParams.Builder builder = new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, mob)
			.withParameter(LootContextParams.ORIGIN, mob.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, killer == null ? level.damageSources().generic() : level.damageSources().playerAttack(killer));

		if (killer != null) {
			builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).withParameter(LootContextParams.ATTACKING_ENTITY, killer);
		}

		LootParams params = builder.create(LootContextParamSets.ENTITY);
		RandomSource random = RandomSource.create(LOOT_SEED);
		int drops = 0;

		for (int roll = 0; roll < LOOT_ROLLS; roll++) {
			if (table.getRandomItems(params, random).stream().anyMatch(stack -> stack.is(item))) drops++;
		}

		return (double) drops / LOOT_ROLLS;
	}

	// Every item a Bastion chest gives over many seeded rolls, with Maple's loot changes applied.
	private static List<ItemStack> rollChest(GameTestHelper helper, String chest) {
		ServerLevel level = helper.getLevel();
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/" + chest));
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.create(LootContextParamSets.CHEST);
		RandomSource random = RandomSource.create(LOOT_SEED);
		List<ItemStack> rolled = new ArrayList<>();

		for (int roll = 0; roll < CHEST_ROLLS; roll++) {
			rolled.addAll(table.getRandomItems(params, random));
		}

		return rolled;
	}

	private static void assertNear(GameTestHelper helper, double rate, double expected, String what) {
		helper.assertTrue(Math.abs(rate - expected) <= DROP_RATE_TOLERANCE, what + " " + rate + " is within " + DROP_RATE_TOLERANCE + " of " + expected);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static void give(ServerPlayer player, Item item) {
		player.getInventory().add(new ItemStack(item));
		// A ticking player sends inventory changes to advancement triggers each tick; a mock player is never ticked.
		player.inventoryMenu.broadcastChanges();
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, Identifier advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(advancement);

		helper.assertTrue(holder != null, advancement + " exists");

		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void assertAdvancement(GameTestHelper helper, Identifier id, String parent, AdvancementType frame, boolean hidden, int experience) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(id);

		helper.assertTrue(holder != null, id + " exists");
		helper.assertValueEqual(holder.value().parent(), Optional.of(Identifier.parse(parent)), id + " parent");
		helper.assertValueEqual(holder.value().display().map(DisplayInfo::type), Optional.of(frame), id + " frame");
		helper.assertValueEqual(holder.value().display().map(DisplayInfo::hidden), Optional.of(hidden), id + " hidden");
		helper.assertValueEqual(holder.value().rewards().experience(), experience, id + " XP reward");
	}
}

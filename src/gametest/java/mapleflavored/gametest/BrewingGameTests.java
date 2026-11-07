package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Potions, splash potions, lingering potions and water bottles stack to 16, but a brewing stand still holds one bottle
 * per slot. A player's splash and lingering throws share a 1-second cooldown that dispensers ignore. Weakness is
 * brewed only from Strength.
 */
public class BrewingGameTests {
	private static final List<Item> CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
	private static final BlockPos STAND = new BlockPos(1, 1, 1);
	// The brewing stand menu lists its 3 bottle slots, its ingredient and its fuel before the player's main inventory.
	private static final int FIRST_INVENTORY_MENU_SLOT = 5;
	private static final int FIRST_MAIN_INVENTORY_SLOT = 9;
	private static final int THROW_COOLDOWN_TICKS = 20;

	@GameTest
	public void potionsAndWaterBottlesStackToSixteen(GameTestHelper helper) {
		for (Item container : CONTAINERS) {
			ItemStack potion = PotionContents.createItemStack(container, Potions.STRENGTH);

			helper.assertValueEqual(potion.getMaxStackSize(), 16, container + " stack size");
		}

		ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
		ItemStack otherWaterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);

		helper.assertValueEqual(waterBottle.getMaxStackSize(), 16, "water bottle stack size");
		helper.assertTrue(ItemStack.isSameItemSameComponents(waterBottle, otherWaterBottle), "two water bottles stack");
		helper.succeed();
	}

	@GameTest
	public void shiftClickingAStackFillsEachBottleSlotWithOne(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		helper.setBlock(STAND, Blocks.BREWING_STAND);
		player.openMenu(helper.getBlockEntity(STAND, BrewingStandBlockEntity.class));

		if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
			throw helper.assertionException(Component.literal("the brewing stand opened " + player.containerMenu));
		}

		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, waterBottles(16));
		menu.clicked(FIRST_INVENTORY_MENU_SLOT, 0, ContainerInput.QUICK_MOVE, player);

		for (int slot = 0; slot < 3; slot++) {
			helper.assertValueEqual(menu.getSlot(slot).getItem().getCount(), 1, "bottles in slot " + slot);
		}

		helper.assertValueEqual(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT).getCount(), 13, "bottles left in the inventory");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void aHopperFillsEachBottleSlotWithOne(GameTestHelper helper) {
		BlockPos hopperPos = STAND.east();

		helper.setBlock(STAND, Blocks.BREWING_STAND);
		helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));

		HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
		BrewingStandBlockEntity stand = helper.getBlockEntity(STAND, BrewingStandBlockEntity.class);

		hopper.setItem(0, waterBottles(16));

		// Long enough for the hopper to move every bottle it could, at 8 ticks per item.
		helper.runAfterDelay(80, () -> {
			for (int slot = 0; slot < 3; slot++) {
				helper.assertValueEqual(stand.getItem(slot).getCount(), 1, "bottles in slot " + slot);
			}

			helper.assertValueEqual(hopper.getItem(0).getCount(), 13, "bottles left in the hopper");
			helper.succeed();
		});
	}

	@GameTest
	public void splashAndLingeringThrowsShareAOneSecondCooldown(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER).copyWithCount(4));
		player.setItemInHand(InteractionHand.OFF_HAND, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WATER).copyWithCount(4));

		assertThrows(helper, player, InteractionHand.MAIN_HAND, "the first splash throw");
		assertRefused(helper, player, InteractionHand.MAIN_HAND, "a splash throw right after a splash throw");
		assertRefused(helper, player, InteractionHand.OFF_HAND, "a lingering throw right after a splash throw");
		helper.assertFalse(
			player.getCooldowns().isOnCooldown(waterBottles(1)), "drinking potions share the throw cooldown"
		);

		tick(player, THROW_COOLDOWN_TICKS - 1);
		assertRefused(helper, player, InteractionHand.OFF_HAND, "a lingering throw a tick before the cooldown ends");

		tick(player, 1);
		assertThrows(helper, player, InteractionHand.OFF_HAND, "a lingering throw once the cooldown ends");
		assertRefused(helper, player, InteractionHand.MAIN_HAND, "a splash throw right after a lingering throw");

		tick(player, THROW_COOLDOWN_TICKS - 1);
		assertRefused(helper, player, InteractionHand.MAIN_HAND, "a splash throw a tick before the lingering cooldown ends");

		tick(player, 1);
		assertThrows(helper, player, InteractionHand.MAIN_HAND, "a splash throw once the lingering cooldown ends");
		helper.succeed();
	}

	@GameTest
	public void dispensersThrowPotionsWithNoCooldown(GameTestHelper helper) {
		BlockPos dispenserPos = new BlockPos(1, 1, 1);
		ServerPlayer player = survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER));
		assertThrows(helper, player, InteractionHand.MAIN_HAND, "the player's splash throw");
		helper.assertTrue(player.getCooldowns().isOnCooldown(new ItemStack(Items.SPLASH_POTION)), "the player is on cooldown");
		helper.killAllEntitiesOfClass(ThrownSplashPotion.class);

		helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));

		DispenserBlockEntity dispenser = helper.getBlockEntity(dispenserPos, DispenserBlockEntity.class);

		dispenser.setItem(0, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER).copyWithCount(2));
		dispenser.setItem(1, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WATER).copyWithCount(2));

		// Each call is the dispenser's scheduled activation, so four in one tick is four dispenses back to back.
		BlockState state = helper.getBlockState(dispenserPos);

		for (int i = 0; i < 4; i++) {
			state.tick(helper.getLevel(), helper.absolutePos(dispenserPos), helper.getLevel().getRandom());
		}

		helper.assertTrue(dispenser.isEmpty(), "the dispenser threw every potion");
		helper.assertValueEqual(helper.getEntities(EntityTypes.SPLASH_POTION).size(), 2, "thrown splash potions");
		helper.assertValueEqual(helper.getEntities(EntityTypes.LINGERING_POTION).size(), 2, "thrown lingering potions");
		helper.succeed();
	}

	@GameTest
	public void aFermentedSpiderEyeTurnsStrengthIntoWeakness(GameTestHelper helper) {
		for (Item container : CONTAINERS) {
			assertBrews(helper, container, Potions.STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
			assertBrews(helper, container, Potions.LONG_STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.LONG_WEAKNESS);
			assertBrews(helper, container, Potions.STRONG_STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
			assertBrews(helper, container, Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
		}

		helper.assertValueEqual(durationOf(Potions.WEAKNESS), 1800, "Weakness lasts 1:30, in ticks");
		helper.assertValueEqual(durationOf(Potions.LONG_WEAKNESS), 4800, "Long Weakness lasts 4:00, in ticks");
		helper.succeed();
	}

	@GameTest
	public void aFermentedSpiderEyeDoesNothingToWater(GameTestHelper helper) {
		for (Item container : CONTAINERS) {
			Optional<ItemStack> brewed = brew(helper, PotionContents.createItemStack(container, Potions.WATER), Items.FERMENTED_SPIDER_EYE);

			helper.assertTrue(brewed.isEmpty(), container + " of water brews into " + brewed);
		}

		helper.succeed();
	}

	private static void assertBrews(GameTestHelper helper, Item container, Holder<Potion> input, Item reagent, Holder<Potion> expected) {
		String label = container + " of " + input.getRegisteredName() + " with " + reagent;
		Optional<ItemStack> brewed = brew(helper, PotionContents.createItemStack(container, input), reagent);

		helper.assertTrue(brewed.isPresent(), label + " brews nothing");
		helper.assertValueEqual(brewed.get().getItem(), container, label + ": the container");
		helper.assertTrue(brewed.get().get(DataComponents.POTION_CONTENTS).is(expected), label + " brews " + brewed.get());
	}

	private static Optional<ItemStack> brew(GameTestHelper helper, ItemStack potion, Item reagent) {
		BrewingInput input = new BrewingInput(potion, new ItemStack(reagent));

		return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.BREWING, input, helper.getLevel())
			.map(recipe -> recipe.value().assemble(input));
	}

	private static int durationOf(Holder<Potion> potion) {
		return potion.value().getEffects().getFirst().getDuration();
	}

	private static ItemStack waterBottles(int count) {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER).copyWithCount(count);
	}

	private static void assertThrows(GameTestHelper helper, ServerPlayer player, InteractionHand hand, String label) {
		int countBefore = player.getItemInHand(hand).getCount();
		InteractionResult result = player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);

		helper.assertTrue(result.consumesAction(), label + " is refused: " + result);
		helper.assertValueEqual(player.getItemInHand(hand).getCount(), countBefore - 1, label + ": potions left");
	}

	private static void assertRefused(GameTestHelper helper, ServerPlayer player, InteractionHand hand, String label) {
		int countBefore = player.getItemInHand(hand).getCount();
		InteractionResult result = player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);

		helper.assertFalse(result.consumesAction(), label + " throws: " + result);
		helper.assertValueEqual(player.getItemInHand(hand).getCount(), countBefore, label + ": potions left");
	}

	// A mock player is never ticked by a connection, so its cooldowns advance only when ticked here.
	private static void tick(ServerPlayer player, int ticks) {
		for (int i = 0; i < ticks; i++) {
			player.doTick();
		}
	}
}

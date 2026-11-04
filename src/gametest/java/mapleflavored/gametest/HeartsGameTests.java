package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.assertMaxHearts;
import static mapleflavored.gametest.MockPlayers.dieAndRespawn;
import static mapleflavored.gametest.MockPlayers.playerWithMaxHearts;
import static mapleflavored.gametest.MockPlayers.relog;
import static mapleflavored.gametest.MockPlayers.runCommand;

import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

/** Max Hearts, the death cost per difficulty, the Heart Floor and the Keep Hearts and Keep Inventory game rules. */
public class HeartsGameTests {
	@GameTest
	public void freshPlayerHasTenMaxHearts(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		assertMaxHearts(helper, player, 10);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:peaceful")
	public void deathOnPeacefulCostsOneMaxHeart(GameTestHelper helper) {
		assertDeathCost(helper, 12, 11);
	}

	@GameTest(environment = "maple_flavored_gametest:easy")
	public void deathOnEasyCostsOneMaxHeart(GameTestHelper helper) {
		assertDeathCost(helper, 12, 11);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void deathOnNormalCostsOneMaxHeart(GameTestHelper helper) {
		assertDeathCost(helper, 12, 11);
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void deathOnHardCostsTwoMaxHearts(GameTestHelper helper) {
		assertDeathCost(helper, 13, 11);
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void deathOnHardStopsAtTheHeartFloor(GameTestHelper helper) {
		assertDeathCost(helper, 11, 10);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void deathAtTheHeartFloorCostsNothing(GameTestHelper helper) {
		assertDeathCost(helper, 10, 10);
	}

	@GameTest(environment = "maple_flavored_gametest:keep_hearts")
	public void keepHeartsMakesDeathCostNoMaxHearts(GameTestHelper helper) {
		assertDeathCost(helper, 12, 12);
	}

	@GameTest
	public void keepHeartsIsOffAndKeepInventoryIsOnByDefault(GameTestHelper helper) {
		GameRules gameRules = helper.getLevel().getGameRules();

		helper.assertFalse(gameRules.get(MapleGameRules.KEEP_HEARTS), "Keep Hearts is off");
		helper.assertTrue(GameRules.KEEP_INVENTORY.defaultValue(), "Keep Inventory defaults to on");
		helper.assertTrue(gameRules.get(GameRules.KEEP_INVENTORY), "Keep Inventory is on in a new world");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void keepInventoryKeepsItemsAndStillCostsMaxHearts(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 12);

		player.getInventory().add(new ItemStack(Items.DIAMOND));

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertTrue(respawned.getInventory().contains(new ItemStack(Items.DIAMOND)), "the diamond is kept");
		assertMaxHearts(helper, respawned, 11);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:keep_inventory_off")
	public void keepInventoryOffDropsItemsAndStillCostsMaxHearts(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 12);

		player.getInventory().add(new ItemStack(Items.DIAMOND));

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertFalse(respawned.getInventory().contains(new ItemStack(Items.DIAMOND)), "the diamond is dropped");
		assertMaxHearts(helper, respawned, 11);
		helper.succeed();
	}

	@GameTest
	public void maxHeartsAndHealthSurviveRelog(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 12);

		player.setHealth(23.0F);

		ServerPlayer relogged = relog(helper, player);

		assertMaxHearts(helper, relogged, 12);
		helper.assertValueEqual(relogged.getHealth(), 23.0F, "health after relog");
		helper.succeed();
	}

	@GameTest
	public void addingAMaxHeartIsRefusedAtThirty(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 30);

		helper.assertFalse(Hearts.addMaxHeart(player), "a 31st Max Heart is refused");
		assertMaxHearts(helper, player, 30);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void deathBelowTheHeartFloorNeverRaisesMaxHearts(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		runCommand(helper, player, "maxhearts set @s 8");

		ServerPlayer respawned = dieAndRespawn(helper, player);

		assertMaxHearts(helper, respawned, 8);
		helper.succeed();
	}

	@GameTest
	public void maxHeartsCommandSetsAndAddsHearts(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		runCommand(helper, player, "maxhearts set @s 15");
		assertMaxHearts(helper, player, 15);

		runCommand(helper, player, "maxhearts add @s -3");
		assertMaxHearts(helper, player, 12);
		helper.succeed();
	}

	@GameTest
	public void maxHeartsCommandKeepsHeartsBetweenOneAndThirty(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		runCommand(helper, player, "maxhearts add @s 50");
		assertMaxHearts(helper, player, 30);

		runCommand(helper, player, "maxhearts add @s -50");
		assertMaxHearts(helper, player, 1);

		runCommand(helper, player, "maxhearts set @s 0");
		assertMaxHearts(helper, player, 1);
		helper.succeed();
	}

	private static void assertDeathCost(GameTestHelper helper, int maxHeartsBefore, int maxHeartsAfter) {
		ServerPlayer player = playerWithMaxHearts(helper, maxHeartsBefore);
		ServerPlayer respawned = dieAndRespawn(helper, player);

		assertMaxHearts(helper, respawned, maxHeartsAfter);
		helper.assertValueEqual(respawned.getHealth(), respawned.getMaxHealth(), "health after respawn");
		helper.succeed();
	}
}

package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.assertMaxHearts;
import static mapleflavored.gametest.MockPlayers.dieAndRespawn;
import static mapleflavored.gametest.MockPlayers.playerReadingChat;
import static mapleflavored.gametest.MockPlayers.playerWithMaxHearts;
import static mapleflavored.gametest.MockPlayers.runCommand;

import java.util.ArrayList;
import java.util.List;
import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Milestones, the Heart Floor they lower on Normal and Hard, and what death costs around that floor. */
public class HeartFloorGameTests {
	// Written out here rather than read from Milestones, so the test also checks that each advancement is loaded.
	private static final List<String> MILESTONES = List.of(
		"maple_flavored:story/obtain_copper_ingot",
		"minecraft:story/smelt_iron",
		"minecraft:story/mine_diamond",
		"minecraft:story/enter_the_nether",
		"maple_flavored:story/obtain_ender_eye",
		"minecraft:story/enter_the_end",
		"maple_flavored:nether/obtain_netherite_ingot"
	);
	private static final FontDescription BROKEN_HEART_FONT = new FontDescription.Resource(Identifier.parse("maple_flavored:icons"));

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void eachMilestoneLowersTheHeartFloorOnNormal(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		helper.assertValueEqual(Hearts.heartFloor(player), 10, "Heart Floor with no Milestones");

		for (int held = 1; held <= MILESTONES.size(); held++) {
			grant(helper, player, MILESTONES.get(held - 1));
			helper.assertValueEqual(Hearts.heartFloor(player), 10 - held, "Heart Floor with " + held + " Milestones");
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void allMilestonesLowerTheHeartFloorToThreeOnHard(GameTestHelper helper) {
		assertHeartFloorWithAllMilestones(helper, 3);
	}

	@GameTest(environment = "maple_flavored_gametest:easy")
	public void milestonesLeaveTheHeartFloorAtTenOnEasy(GameTestHelper helper) {
		assertHeartFloorWithAllMilestones(helper, 10);
	}

	@GameTest(environment = "maple_flavored_gametest:peaceful")
	public void milestonesLeaveTheHeartFloorAtTenOnPeaceful(GameTestHelper helper) {
		assertHeartFloorWithAllMilestones(helper, 10);
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void grantingAMilestoneTwiceCountsOnce(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grant(helper, player, MILESTONES.getFirst());
		grant(helper, player, MILESTONES.getFirst());

		helper.assertValueEqual(Hearts.heartFloor(player), 9, "Heart Floor");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void revokingAMilestoneRaisesTheHeartFloor(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grant(helper, player, MILESTONES.get(0));
		grant(helper, player, MILESTONES.get(1));
		revoke(helper, player, MILESTONES.get(0));

		helper.assertValueEqual(Hearts.heartFloor(player), 9, "Heart Floor after revoking one of two Milestones");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void anItemMilestoneCountsOnceTheItemIsInTheInventory(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		player.getInventory().add(new ItemStack(Items.COPPER_INGOT));
		// A ticking player sends inventory changes to advancement triggers each tick; a mock player is never ticked.
		player.inventoryMenu.broadcastChanges();

		helper.assertTrue(isDone(helper, player, MILESTONES.getFirst()), "the copper ingot Milestone is granted");
		helper.assertValueEqual(Hearts.heartFloor(player), 9, "Heart Floor");
		helper.succeed();
	}

	@GameTest
	public void eachMilestoneDescriptionStartsWithABrokenHeart(GameTestHelper helper) {
		for (String milestone : MILESTONES) {
			Component description = advancement(helper, milestone).value().display().orElseThrow().description();
			boolean startsWithBrokenHeart = description.getSiblings().getFirst().getContents() instanceof PlainTextContents text
				&& text.text().equals("")
				&& description.getSiblings().getFirst().getStyle().getFont().equals(BROKEN_HEART_FONT);

			helper.assertTrue(startsWithBrokenHeart, milestone + "'s description starts with a broken heart");
		}

		helper.succeed();
	}

	@GameTest
	public void milestonesTakeTheParentsOfTheAdvancementsTheyLeadTo(GameTestHelper helper) {
		assertParent(helper, "minecraft:story/upgrade_tools", "maple_flavored:story/obtain_copper_ingot");
		assertParent(helper, "maple_flavored:story/obtain_copper_ingot", "minecraft:story/mine_stone");
		assertParent(helper, "minecraft:story/follow_ender_eye", "maple_flavored:story/obtain_ender_eye");
		assertParent(helper, "maple_flavored:story/obtain_ender_eye", "minecraft:story/enter_the_nether");
		assertParent(helper, "minecraft:nether/netherite_armor", "maple_flavored:nether/obtain_netherite_ingot");
		assertParent(helper, "maple_flavored:nether/obtain_netherite_ingot", "minecraft:nether/obtain_ancient_debris");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void chatAnnouncesEachLowerHeartFloor(GameTestHelper helper) {
		List<Component> chat = new ArrayList<>();
		ServerPlayer player = playerReadingChat(helper, chat);

		grant(helper, player, MILESTONES.get(0));
		grant(helper, player, "minecraft:story/mine_stone");
		grant(helper, player, MILESTONES.get(1));
		revoke(helper, player, MILESTONES.get(1));
		grant(helper, player, MILESTONES.get(1));

		helper.assertValueEqual(announcedHeartFloors(chat), List.of("9", "8", "8"), "announced Heart Floors");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:easy")
	public void chatAnnouncesNoHeartFloorOnEasy(GameTestHelper helper) {
		List<Component> chat = new ArrayList<>();
		ServerPlayer player = playerReadingChat(helper, chat);

		grant(helper, player, MILESTONES.getFirst());

		helper.assertValueEqual(announcedHeartFloors(chat), List.of(), "announced Heart Floors");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:hard")
	public void deathAfterMilestonesFallsToTheLowerHeartFloor(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grantMilestones(helper, player, 3);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		assertMaxHearts(helper, respawned, 8);

		respawned = dieAndRespawn(helper, respawned);

		assertMaxHearts(helper, respawned, 7);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void deathBelowTheHeartFloorAfterARevokedMilestoneCostsNothing(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grantMilestones(helper, player, 4);
		runCommand(helper, player, "maxhearts set @s 6");
		revoke(helper, player, MILESTONES.getFirst());

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertValueEqual(Hearts.heartFloor(respawned), 7, "Heart Floor after the revoke");
		assertMaxHearts(helper, respawned, 6);
		helper.succeed();
	}

	// Stands in for a world switched to Easy: GameTests in one environment share a level, so none changes its difficulty.
	@GameTest(environment = "maple_flavored_gametest:easy")
	public void deathBelowTheHeartFloorOnEasyCostsNothing(GameTestHelper helper) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grantMilestones(helper, player, MILESTONES.size());
		runCommand(helper, player, "maxhearts set @s 5");

		ServerPlayer respawned = dieAndRespawn(helper, player);

		assertMaxHearts(helper, respawned, 5);
		helper.succeed();
	}

	private static void assertHeartFloorWithAllMilestones(GameTestHelper helper, int heartFloor) {
		ServerPlayer player = playerWithMaxHearts(helper, 10);

		grantMilestones(helper, player, MILESTONES.size());

		helper.assertValueEqual(Hearts.heartFloor(player), heartFloor, "Heart Floor with every Milestone");
		helper.succeed();
	}

	private static List<String> announcedHeartFloors(List<Component> chat) {
		return chat.stream()
			.map(Component::getContents)
			.filter(TranslatableContents.class::isInstance)
			.map(TranslatableContents.class::cast)
			.filter(contents -> contents.getKey().equals("chat.maple_flavored.heart_floor_lowered"))
			.map(contents -> ((Component) contents.getArgs()[0]).getString())
			.toList();
	}

	private static void assertParent(GameTestHelper helper, String advancement, String parent) {
		var parentId = advancement(helper, advancement).value().parent();

		helper.assertValueEqual(parentId.map(Identifier::toString).orElse("none"), parent, advancement + "'s parent");
	}

	private static void grantMilestones(GameTestHelper helper, ServerPlayer player, int count) {
		for (String milestone : MILESTONES.subList(0, count)) {
			grant(helper, player, milestone);
		}
	}

	private static void grant(GameTestHelper helper, ServerPlayer player, String advancement) {
		runCommand(helper, player, "advancement grant @s only " + advancement);
		helper.assertTrue(isDone(helper, player, advancement), advancement + " is granted");
	}

	private static void revoke(GameTestHelper helper, ServerPlayer player, String advancement) {
		runCommand(helper, player, "advancement revoke @s only " + advancement);
		helper.assertFalse(isDone(helper, player, advancement), advancement + " is revoked");
	}

	private static boolean isDone(GameTestHelper helper, ServerPlayer player, String advancement) {
		return player.getAdvancements().getOrStartProgress(advancement(helper, advancement)).isDone();
	}

	private static AdvancementHolder advancement(GameTestHelper helper, String id) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(id));

		helper.assertTrue(holder != null, id + " is loaded");

		return holder;
	}
}

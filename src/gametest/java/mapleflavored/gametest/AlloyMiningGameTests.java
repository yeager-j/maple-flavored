package mapleflavored.gametest;

import static mapleflavored.gametest.Advancements.isDone;

import java.util.List;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Shakudo tools mine 30% faster after Efficiency, Adamant tools mine hard blocks as if they were deepslate, and an
 * Adamant pickaxe with Efficiency V breaks bedrock under Haste II.
 */
public class AlloyMiningGameTests {
	private static final BlockPos BLOCK_POS = new BlockPos(1, 1, 1);
	private static final Vec3 MINER_POS = new Vec3(1.5, 1.0, 2.5);
	private static final Identifier BREAKING_THE_RULES = Identifier.parse("maple_flavored:nether/break_bedrock");

	@GameTest
	public void shakudoWithEfficiencyFiveInstaMinesStone(GameTestHelper helper) {
		helper.setBlock(BLOCK_POS, Blocks.STONE);

		float shakudo = progress(helper, efficient(helper, AlloyGear.of(Alloy.SHAKUDO, GearKind.PICKAXE), 5), null);
		float netherite = progress(helper, efficient(helper, Items.NETHERITE_PICKAXE, 5), null);

		helper.assertTrue(shakudo >= 1.0F, "a Shakudo Efficiency V pickaxe insta-mines stone, at " + shakudo);
		helper.assertTrue(netherite < 1.0F, "a Netherite Efficiency V pickaxe insta-mines stone, at " + netherite);
		helper.succeed();
	}

	@GameTest
	public void shakudoMinesThirtyPercentFaster(GameTestHelper helper) {
		helper.setBlock(BLOCK_POS, Blocks.DEEPSLATE);

		float shakudo = progress(helper, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.PICKAXE)), null);
		float netherite = progress(helper, new ItemStack(Items.NETHERITE_PICKAXE), null);

		helper.assertValueEqual(Math.round(shakudo / netherite * 100), 130, "Shakudo's speed as a percentage of Netherite's");
		helper.succeed();
	}

	@GameTest
	public void adamantMinesHardBlocksLikeDeepslate(GameTestHelper helper) {
		ItemStack adamantPickaxe = new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE));
		ItemStack netheritePickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
		float netheriteOnDeepslate = progressOn(helper, Blocks.DEEPSLATE, netheritePickaxe);

		helper.assertValueEqual(progressOn(helper, Blocks.OBSIDIAN, adamantPickaxe), netheriteOnDeepslate, "Adamant on obsidian");
		helper.assertValueEqual(progressOn(helper, Blocks.ANCIENT_DEBRIS, adamantPickaxe), netheriteOnDeepslate, "Adamant on ancient debris");
		helper.assertValueEqual(progressOn(helper, Blocks.DEEPSLATE, adamantPickaxe), netheriteOnDeepslate, "Adamant on deepslate");
		helper.assertValueEqual(
			progressOn(helper, Blocks.STONE, adamantPickaxe), progressOn(helper, Blocks.STONE, netheritePickaxe), "Adamant on stone"
		);
		helper.succeed();
	}

	@GameTest
	public void adamantDoesntSpeedUpBlocksOutsideItsTag(GameTestHelper helper) {
		ItemStack adamantPickaxe = new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE));
		ItemStack netheritePickaxe = new ItemStack(Items.NETHERITE_PICKAXE);

		for (Block block : List.of(Blocks.REINFORCED_DEEPSLATE, Blocks.TRIAL_SPAWNER, Blocks.VAULT)) {
			helper.assertValueEqual(progressOn(helper, block, adamantPickaxe), progressOn(helper, block, netheritePickaxe), "Adamant on " + block);
		}

		helper.assertValueEqual(
			progressOn(helper, Blocks.OBSIDIAN, new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.SHOVEL))),
			progressOn(helper, Blocks.OBSIDIAN, new ItemStack(Items.NETHERITE_SHOVEL)),
			"an Adamant shovel on obsidian"
		);
		helper.assertValueEqual(progressOn(helper, Blocks.SPAWNER, adamantPickaxe), 0.0F, "Adamant on an uncleared spawner");
		helper.succeed();
	}

	@GameTest
	public void anAdamantPickaxeBreaksBedrockOnlyWithEfficiencyFiveAndHasteTwo(GameTestHelper helper) {
		ItemStack adamantFive = efficient(helper, AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE), 5);
		MobEffectInstance hasteTwo = new MobEffectInstance(MobEffects.HASTE, 6000, 1);

		helper.setBlock(BLOCK_POS, Blocks.BEDROCK);

		helper.assertValueEqual(progress(helper, adamantFive, hasteTwo), 1.0F / 300, "Adamant Efficiency V under Haste II");
		helper.assertValueEqual(progress(helper, adamantFive, new MobEffectInstance(MobEffects.HASTE, 6000, 0)), 0.0F, "under Haste I");
		helper.assertValueEqual(progress(helper, adamantFive, null), 0.0F, "with no Haste");
		helper.assertValueEqual(
			progress(helper, efficient(helper, AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE), 4), hasteTwo), 0.0F, "with Efficiency IV"
		);
		helper.assertValueEqual(progress(helper, efficient(helper, Items.NETHERITE_PICKAXE, 5), hasteTwo), 0.0F, "with a Netherite pickaxe");
		helper.assertValueEqual(
			progress(helper, efficient(helper, AlloyGear.of(Alloy.ADAMANT, GearKind.SHOVEL), 5), hasteTwo), 0.0F, "with an Adamant shovel"
		);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void breakingBedrockTakesFifteenSecondsAndEarnsBreakingTheRules(GameTestHelper helper) {
		ServerPlayer player = miner(helper, efficient(helper, AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE), 5));

		player.addEffect(new MobEffectInstance(MobEffects.HASTE, 6000, 1));
		helper.setBlock(BLOCK_POS, Blocks.BEDROCK);
		mine(helper, player, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);

		helper.runAfterDelay(100, () -> {
			mine(helper, player, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
			helper.assertBlockPresent(Blocks.BEDROCK, BLOCK_POS);
			mine(helper, player, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
		});
		helper.runAfterDelay(400, () -> {
			mine(helper, player, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
			helper.assertBlockNotPresent(Blocks.BEDROCK, BLOCK_POS);
			helper.assertItemEntityNotPresent(Items.BEDROCK);
			helper.assertTrue(isDone(helper, player, BREAKING_THE_RULES), "breaking bedrock earns Breaking the Rules");
			helper.succeed();
		});
	}

	@GameTest
	public void breakingBedrockInCreativeEarnsNothing(GameTestHelper helper) {
		ServerPlayer player = MockPlayers.creativePlayer(helper);

		besideTheBlock(helper, player);

		helper.setBlock(BLOCK_POS, Blocks.BEDROCK);
		mine(helper, player, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);

		helper.assertBlockNotPresent(Blocks.BEDROCK, BLOCK_POS);
		helper.assertFalse(isDone(helper, player, BREAKING_THE_RULES), "breaking bedrock in Creative earns Breaking the Rules");
		helper.succeed();
	}

	@GameTest
	public void breakingTheRulesIsAVisibleNetherChallenge(GameTestHelper helper) {
		AdvancementHolder holder = Advancements.advancement(helper, BREAKING_THE_RULES);

		helper.assertValueEqual(holder.value().parent().map(Identifier::toString).orElse(""), "minecraft:nether/create_full_beacon", "its parent");
		helper.assertValueEqual(holder.value().display().map(DisplayInfo::type).orElse(null), AdvancementType.CHALLENGE, "its frame");
		helper.assertFalse(holder.value().display().map(DisplayInfo::hidden).orElse(true), "it's hidden");
		helper.assertValueEqual(holder.value().rewards().experience(), 100, "its XP reward");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:alloy_mining_rules", maxTicks = 20)
	public void theMiningGameRulesApplyAtOnce(GameTestHelper helper) {
		ItemStack adamantFive = efficient(helper, AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE), 5);

		// The rules reach the level's synced copy at the end of a tick.
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(
				progressOn(helper, Blocks.DEEPSLATE, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.PICKAXE))),
				progressOn(helper, Blocks.DEEPSLATE, new ItemStack(Items.NETHERITE_PICKAXE)),
				"a Shakudo pickaxe with a 0% bonus"
			);
			helper.assertValueEqual(
				progressOn(helper, Blocks.OBSIDIAN, new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.PICKAXE))),
				progressOn(helper, Blocks.OBSIDIAN, new ItemStack(Items.NETHERITE_PICKAXE)),
				"an Adamant pickaxe on obsidian with a cap of 50"
			);
			helper.setBlock(BLOCK_POS, Blocks.BEDROCK);
			helper.assertValueEqual(progress(helper, adamantFive, new MobEffectInstance(MobEffects.HASTE, 6000, 1)), 1.0F / 100, "bedrock in 100 ticks");
			helper.succeed();
		});
	}

	private static float progressOn(GameTestHelper helper, Block block, ItemStack tool) {
		helper.setBlock(BLOCK_POS, block);

		return progress(helper, tool, null);
	}

	/** How much of the block at {@link #BLOCK_POS} a grounded Survival player holding {@code tool} mines each tick. */
	private static float progress(GameTestHelper helper, ItemStack tool, MobEffectInstance effect) {
		ServerPlayer player = miner(helper, tool.copy());

		if (effect != null) player.addEffect(new MobEffectInstance(effect));

		BlockPos pos = helper.absolutePos(BLOCK_POS);

		return helper.getLevel().getBlockState(pos).getDestroyProgress(player, helper.getLevel(), pos);
	}

	/**
	 * A Survival player beside {@link #BLOCK_POS} holding {@code tool}, ticked once so the tool's Efficiency applies,
	 * then grounded, since vanilla mines 5 times slower in the air.
	 */
	private static ServerPlayer miner(GameTestHelper helper, ItemStack tool) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		besideTheBlock(helper, player);
		player.setItemSlot(EquipmentSlot.MAINHAND, tool);
		player.doTick();
		player.setOnGround(true);

		return player;
	}

	private static void besideTheBlock(GameTestHelper helper, ServerPlayer player) {
		player.snapTo(helper.absoluteVec(MINER_POS));
	}

	private static ItemStack efficient(GameTestHelper helper, Item item, int level) {
		ItemStack tool = new ItemStack(item);

		tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), level);

		return tool;
	}

	private static void mine(GameTestHelper helper, ServerPlayer player, ServerboundPlayerActionPacket.Action action) {
		player.gameMode.handleBlockBreakAction(helper.absolutePos(BLOCK_POS), action, Direction.UP, player.level().getMaxY(), 0);
	}
}

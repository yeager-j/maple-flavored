package mapleflavored.gametest;

import java.util.List;
import java.util.function.Function;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import mapleflavored.alloys.IllagerNeutrality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Alloy armor's code traits: Steel, Electrum and Orichalcum take a share off sonic booms and explosions, fire, and magic
 * per piece; Piglins treat Electrum as gold; and Illagers and their Vexes leave Orichalcum wearers alone until a raid,
 * an attack or a book alert.
 */
public class AlloyCombatGameTests {
	private static final float HIT = 4.0F;
	private static final float TOLERANCE = 1.0E-4F;
	private static final List<EquipmentSlot> HUMANOID_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<Item> NETHERITE_SET = List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

	private static final Vec3 PLAYER_POS = new Vec3(1.5, 1.0, 4.5);
	private static final BlockPos ILLAGER_POS = new BlockPos(1, 1, 1);
	private static final BlockPos WITNESS_POS = new BlockPos(4, 1, 1);
	// Behind a wall from the hit Illager, so it can't see it.
	private static final BlockPos HIDDEN_POS = new BlockPos(6, 1, 6);
	// Long enough for an Illager's target goals to run several times.
	private static final int TARGETING_TICKS = 40;

	@GameTest
	public void steelArmorTakesTenPercentPerPieceOffExplosionsAndSonicBooms(GameTestHelper helper) {
		assertReduced(helper, alloySet(Alloy.STEEL), level -> level.damageSources().explosion(null, null), 0.4F, "a full Steel set's explosion damage");
		assertReduced(helper, alloySet(Alloy.STEEL), level -> level.damageSources().sonicBoom(helper.spawnWithNoFreeWill(EntityTypes.WARDEN, ILLAGER_POS)), 0.4F, "a full Steel set's sonic boom damage");
		assertReduced(helper, alloySet(Alloy.STEEL), level -> level.damageSources().inFire(), 0.0F, "a full Steel set's fire damage");
		assertReduced(helper, steelBootsAndNetherite(), level -> level.damageSources().explosion(null, null), 0.1F, "one Steel piece's explosion damage");
		helper.succeed();
	}

	@GameTest
	public void electrumArmorTakesFifteenPercentPerPieceOffFire(GameTestHelper helper) {
		assertReduced(helper, alloySet(Alloy.ELECTRUM), level -> level.damageSources().inFire(), 0.6F, "a full Electrum set's fire damage");
		assertReduced(helper, alloySet(Alloy.ELECTRUM), level -> level.damageSources().lava(), 0.6F, "a full Electrum set's lava damage");
		assertReduced(helper, alloySet(Alloy.ELECTRUM), level -> level.damageSources().magic(), 0.0F, "a full Electrum set's magic damage");
		helper.succeed();
	}

	@GameTest
	public void orichalcumArmorTakesTenPercentPerPieceOffMagic(GameTestHelper helper) {
		assertReduced(helper, alloySet(Alloy.ORICHALCUM), level -> level.damageSources().magic(), 0.4F, "a full Orichalcum set's magic damage");
		assertReduced(
			helper,
			alloySet(Alloy.ORICHALCUM),
			level -> level.damageSources().indirectMagic(helper.spawnWithNoFreeWill(EntityTypes.EVOKER, ILLAGER_POS), null),
			0.4F,
			"a full Orichalcum set's indirect magic damage"
		);
		assertReduced(helper, alloySet(Alloy.ORICHALCUM), level -> level.damageSources().explosion(null, null), 0.0F, "a full Orichalcum set's explosion damage");
		helper.succeed();
	}

	@GameTest
	public void steelHorseArmorTakesTenPercentOffExplosions(GameTestHelper helper) {
		float steel = horseDamageTaken(helper, AlloyGear.of(Alloy.STEEL, GearKind.HORSE_ARMOR));
		float netherite = horseDamageTaken(helper, Items.NETHERITE_HORSE_ARMOR);

		helper.assertTrue(Math.abs(steel - netherite * 0.9F) < TOLERANCE, "a Steel-armored horse took " + steel + ", 90% of Netherite's " + netherite);
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void piglinsTreatElectrumArmorAsGold(GameTestHelper helper) {
		ServerPlayer electrum = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ELECTRUM, GearKind.BOOTS));
		Piglin piglin = piglinNear(helper);

		helper.runAfterDelay(TARGETING_TICKS, () -> {
			helper.assertTrue(piglin.getBrain().getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD).isEmpty(), "the Piglin sees a player not wearing gold");
			helper.assertFalse(piglin.getTarget() == electrum, "the Piglin targets the Electrum wearer");
			helper.succeed();
		});
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void piglinsDontTreatNetheriteArmorAsGold(GameTestHelper helper) {
		ServerPlayer netherite = wearing(helper, EquipmentSlot.FEET, Items.NETHERITE_BOOTS);
		Piglin piglin = piglinNear(helper);

		helper.succeedWhen(() -> helper.assertTrue(
			piglin.getBrain().getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD).orElse(null) == netherite,
			"the Piglin sees the Netherite wearer as not wearing gold"
		));
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void illagersAttackPlayersWithoutOrichalcum(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, Items.NETHERITE_BOOTS);
		Vindicator vindicator = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);

		helper.succeedWhen(() -> assertTargets(helper, vindicator, player));
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void illagersLeaveAPlayerWithOneOrichalcumPieceAlone(GameTestHelper helper) {
		wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));

		Vindicator vindicator = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);

		helper.runAfterDelay(TARGETING_TICKS, () -> {
			helper.assertTrue(vindicator.getTarget() == null, "the Vindicator has no target");
			helper.succeed();
		});
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void attackingAnIllagerTurnsItAndThoseThatSeeItHostile(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));
		Vindicator hit = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);
		Vindicator witness = helper.spawn(EntityTypes.VINDICATOR, WITNESS_POS);

		wallOff(helper);

		Vindicator hidden = helper.spawn(EntityTypes.VINDICATOR, HIDDEN_POS);

		hit.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);

		helper.runAfterDelay(TARGETING_TICKS, () -> {
			assertTargets(helper, hit, player);
			assertTargets(helper, witness, player);
			helper.assertTrue(hidden.getTarget() == null, "the hidden Vindicator has no target");
			helper.succeed();
		});
	}

	@GameTest(environment = "maple_flavored_gametest:orichalcum_illager_anger", maxTicks = 200)
	public void illagerAngerAtAnOrichalcumWearerWearsOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));
		Vindicator vindicator = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);

		vindicator.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);

		// The environment's anger lasts 60 ticks: long enough for the Vindicator to turn on the player first.
		helper.startSequence()
			.thenWaitUntil(() -> assertTargets(helper, vindicator, player))
			.thenWaitUntil(() -> helper.assertTrue(vindicator.getTarget() == null, "the Vindicator still targets the player"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void raidingIllagersAttackOrichalcumWearers(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));
		Vindicator raider = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);

		raider.setCurrentRaid(new Raid(helper.absolutePos(ILLAGER_POS), Difficulty.NORMAL));

		helper.succeedWhen(() -> assertTargets(helper, raider, player));
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void aBookAlertTurnsAnIllagerHostileToThatPlayerOnly(GameTestHelper helper) {
		ServerPlayer alerted = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));
		Vindicator vindicator = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);
		ServerPlayer bystander = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));

		IllagerNeutrality.anger(helper.getLevel(), vindicator, alerted);

		helper.assertTrue(vindicator.canAttack(alerted), "the Vindicator can attack the alerted player");
		helper.assertFalse(vindicator.canAttack(bystander), "the Vindicator can attack the other Orichalcum wearer");
		helper.succeedWhen(() -> assertTargets(helper, vindicator, alerted));
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void anIllagerDropsAPlayerWhoPutsOnOrichalcum(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, Items.NETHERITE_BOOTS);
		Vindicator vindicator = helper.spawn(EntityTypes.VINDICATOR, ILLAGER_POS);

		helper.startSequence()
			.thenWaitUntil(() -> assertTargets(helper, vindicator, player))
			.thenExecute(() -> player.setItemSlot(EquipmentSlot.FEET, new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS))))
			.thenIdle(TARGETING_TICKS)
			.thenExecute(() -> helper.assertTrue(vindicator.getTarget() == null, "the Vindicator has no target"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void anEvokersVexesLeaveOrichalcumWearersAlone(GameTestHelper helper) {
		wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));

		Vex vex = vexOf(helper, helper.spawnWithNoFreeWill(EntityTypes.EVOKER, HIDDEN_POS));

		helper.runAfterDelay(TARGETING_TICKS, () -> {
			helper.assertTrue(vex.getTarget() == null, "the Vex has no target");
			helper.succeed();
		});
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void aVexStaysNeutralAfterItsEvokerIsGone(GameTestHelper helper) {
		wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));

		Evoker evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, HIDDEN_POS);
		Vex vex = vexOf(helper, evoker);

		evoker.discard();

		helper.runAfterDelay(TARGETING_TICKS, () -> {
			helper.assertTrue(vex.getTarget() == null, "the Vex has no target");
			helper.succeed();
		});
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void anAngryEvokersVexesAttackOrichalcumWearers(GameTestHelper helper) {
		ServerPlayer player = wearing(helper, EquipmentSlot.FEET, AlloyGear.of(Alloy.ORICHALCUM, GearKind.BOOTS));
		Evoker evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, HIDDEN_POS);
		Vex vex = vexOf(helper, evoker);

		IllagerNeutrality.anger(helper.getLevel(), evoker, player);

		helper.succeedWhen(() -> helper.assertTrue(vex.getTarget() == player, "the Vex targets the player, not " + vex.getTarget()));
	}

	private static Vex vexOf(GameTestHelper helper, Evoker evoker) {
		Vex vex = helper.spawn(EntityTypes.VEX, ILLAGER_POS.above());

		vex.setOwner(evoker);

		return vex;
	}

	private static List<Item> alloySet(Alloy alloy) {
		return List.of(
			AlloyGear.of(alloy, GearKind.HELMET),
			AlloyGear.of(alloy, GearKind.CHESTPLATE),
			AlloyGear.of(alloy, GearKind.LEGGINGS),
			AlloyGear.of(alloy, GearKind.BOOTS)
		);
	}

	private static List<Item> steelBootsAndNetherite() {
		return List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, AlloyGear.of(Alloy.STEEL, GearKind.BOOTS));
	}

	// Alloy armor has Netherite's armor points, so against a Netherite set the only difference is the reduction.
	private static void assertReduced(
		GameTestHelper helper, List<Item> armor, Function<ServerLevel, DamageSource> source, float reduction, String what
	) {
		float alloy = playerDamageTaken(helper, armor, source);
		float netherite = playerDamageTaken(helper, NETHERITE_SET, source);
		float expected = netherite * (1.0F - reduction);

		helper.assertTrue(Math.abs(alloy - expected) < TOLERANCE, what + " was " + alloy + ", expected " + expected + " from Netherite's " + netherite);
	}

	private static float playerDamageTaken(GameTestHelper helper, List<Item> armor, Function<ServerLevel, DamageSource> source) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		for (int piece = 0; piece < HUMANOID_SLOTS.size(); piece++) player.setItemSlot(HUMANOID_SLOTS.get(piece), new ItemStack(armor.get(piece)));

		return damageTaken(level, player, source.apply(level));
	}

	private static float horseDamageTaken(GameTestHelper helper, Item armor) {
		ServerLevel level = helper.getLevel();
		Horse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, ILLAGER_POS);

		horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(armor));

		return damageTaken(level, horse, level.damageSources().explosion(null, null));
	}

	private static float damageTaken(ServerLevel level, LivingEntity entity, DamageSource source) {
		float before = entity.getHealth();

		entity.hurtServer(level, source, HIT);

		return before - entity.getHealth();
	}

	private static ServerPlayer wearing(GameTestHelper helper, EquipmentSlot slot, Item item) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.snapTo(helper.absoluteVec(PLAYER_POS));
		player.setItemSlot(slot, new ItemStack(item));

		return player;
	}

	private static Piglin piglinNear(GameTestHelper helper) {
		Piglin piglin = helper.spawn(EntityTypes.PIGLIN, ILLAGER_POS);

		piglin.setImmuneToZombification(true);

		return piglin;
	}

	// A wall the height of an Illager between the hit Illager's corner and the hidden one's.
	private static void wallOff(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
		}

		for (int z = 5; z <= 7; z++) {
			for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
		}
	}

	private static void assertTargets(GameTestHelper helper, Vindicator vindicator, ServerPlayer player) {
		helper.assertTrue(vindicator.getTarget() == player, "the Vindicator targets the player, not " + vindicator.getTarget());
	}
}

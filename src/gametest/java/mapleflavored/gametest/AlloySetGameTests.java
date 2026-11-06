package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.dieAndRespawn;
import static mapleflavored.gametest.MockPlayers.relog;
import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.List;
import java.util.Optional;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.AlloySets;
import mapleflavored.alloys.GearKind;
import mapleflavored.alloys.OrichalcumSave;
import mapleflavored.gametest.mixin.EntityAccessor;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Alloy set bonuses: what counts as a full set, each Alloy's bonus, Orichalcum's save and its recharge, and the
 * advancement for wearing every set.
 *
 * <p>A mock player isn't ticked by a connection, so tests tick it themselves wherever vanilla's equipment-change check
 * must run.
 */
public class AlloySetGameTests {
	private static final List<EquipmentSlot> SET_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<GearKind> SET_KINDS = List.of(GearKind.HELMET, GearKind.CHESTPLATE, GearKind.LEGGINGS, GearKind.BOOTS);
	private static final List<Item> NETHERITE_SET = List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
	private static final float TOLERANCE = 1.0E-4F;

	private static final BlockPos FLOOR_POS = new BlockPos(1, 0, 1);
	private static final Vec3 STANDING_POS = new Vec3(1.5, 1.0, 1.5);
	private static final BlockPos TARGET_POS = new BlockPos(3, 1, 3);
	private static final BlockPos ATTACKER_POS = new BlockPos(5, 1, 3);
	private static final float MELEE_HIT = 10.0F;

	private static final int WITHERING_INTERVAL = 100;
	private static final int ELECTRUM_WITHERING_INTERVAL = 200;
	// Waits stay clear of the exact interval, where the order of the test's tick and the withering tick decides.
	private static final int MARGIN = 10;
	// The orichalcum_quick_recharge environment's recharge time.
	private static final int QUICK_RECHARGE_TICKS = 40;

	private static final String WEAR_EVERY_SET = "maple_flavored:nether/wear_every_alloy_set";

	@GameTest
	public void aFullSetIsFourPiecesOfOneAlloy(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.STEEL);
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.of(Alloy.STEEL), "four Steel pieces' set");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.empty(), "three Steel pieces and Netherite's set");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AlloyGear.of(Alloy.ADAMANT, GearKind.HELMET)));
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.empty(), "three Steel pieces and Adamant's set");

		wearSet(player, NETHERITE_SET);
		helper.assertValueEqual(AlloySets.fullSet(player), Optional.empty(), "a Netherite set");
		helper.succeed();
	}

	@GameTest
	public void aFullSteelSetTakesNoKnockback(GameTestHelper helper) {
		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.STEEL);
		player.doTick();
		helper.assertValueEqual(knockedBackDistance(helper, player), 0.0, "a full Steel set's knockback");
		helper.assertValueEqual(player.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), 1.0, "a full Steel set's explosion knockback resistance");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		player.doTick();
		helper.assertTrue(knockedBackDistance(helper, player) > 0.0, "three Steel pieces are knocked back");
		helper.assertTrue(player.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) < 1.0, "three Steel pieces' explosion knockback resistance is below 1");
		helper.succeed();
	}

	@GameTest
	public void aFullSteelSetRefusesLevitationAndEndsIt(GameTestHelper helper) {
		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.STEEL);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		player.doTick();
		helper.assertTrue(player.addEffect(levitation()), "three Steel pieces can levitate");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AlloyGear.of(Alloy.STEEL, GearKind.HELMET)));
		player.doTick();
		helper.assertFalse(player.hasEffect(MobEffects.LEVITATION), "completing the Steel set ends Levitation");
		helper.assertFalse(player.addEffect(levitation()), "a full Steel set refuses Levitation");
		helper.succeed();
	}

	@GameTest
	public void aFullElectrumSetIsImmuneToFireAndLava(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.ELECTRUM);
		helper.assertTrue(player.fireImmune(), "a full Electrum set is fire immune");
		helper.assertValueEqual(damageTaken(player, level.damageSources().lava(), 4.0F), 0.0F, "a full Electrum set's lava damage");
		helper.assertValueEqual(damageTaken(player, level.damageSources().inFire(), 4.0F), 0.0F, "a full Electrum set's fire damage");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		helper.assertFalse(player.fireImmune(), "three Electrum pieces aren't fire immune");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void aFullElectrumSetWithersAHeartEveryTenSeconds(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ELECTRUM);
		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + MARGIN, () -> helper.assertValueEqual(Hearts.witheredHearts(player), 0, "Withered Hearts after 5 seconds"))
			.thenExecuteAfter(ELECTRUM_WITHERING_INTERVAL - WITHERING_INTERVAL, () -> helper.assertValueEqual(Hearts.witheredHearts(player), 1, "Withered Hearts after 10 seconds"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 300)
	public void takingOffElectrumWithersAtOnceWhenFiveSecondsHavePassed(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ELECTRUM);
		wither(player);

		helper.startSequence()
			.thenExecuteAfter(WITHERING_INTERVAL + 5 * MARGIN, () -> {
				helper.assertValueEqual(Hearts.witheredHearts(player), 0, "Withered Hearts in Electrum");
				player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
			})
			.thenExecuteAfter(MARGIN, () -> helper.assertValueEqual(Hearts.witheredHearts(player), 1, "Withered Hearts once Electrum is off"))
			.thenSucceed();
	}

	@GameTest
	public void aFullShakudoSetIgnoresSoulSandAndHoney(GameTestHelper helper) {
		assertBlockSpeedFactor(helper, Blocks.SOUL_SAND);
		assertBlockSpeedFactor(helper, Blocks.HONEY_BLOCK);
		helper.succeed();
	}

	@GameTest
	public void aFullShakudoSetIsNeverStuckInCobwebsOrPowderSnow(GameTestHelper helper) {
		assertNeverStuck(helper, Blocks.COBWEB);
		assertNeverStuck(helper, Blocks.POWDER_SNOW);

		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.SHAKUDO);
		player.makeStuckInBlock(Blocks.SWEET_BERRY_BUSH.defaultBlockState(), new Vec3(0.8, 0.75, 0.8));
		helper.assertFalse(stuckSpeedMultiplier(player).equals(Vec3.ZERO), "a sweet berry bush still slows a full Shakudo set");
		helper.succeed();
	}

	@GameTest
	public void aFullShakudoSetIsNotSlowedByFreezing(GameTestHelper helper) {
		Identifier frostSlowdown = Identifier.withDefaultNamespace("powder_snow");

		// Vanilla adds the frost slowdown only over a solid block.
		helper.setBlock(FLOOR_POS, Blocks.STONE);

		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.SHAKUDO);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		player.setTicksFrozen(player.getTicksRequiredToFreeze());
		player.doTick();
		helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(frostSlowdown) != null, "freezing slows three Shakudo pieces");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.HELMET)));
		player.doTick();
		helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(frostSlowdown) == null, "freezing doesn't slow a full Shakudo set");
		helper.succeed();
	}

	@GameTest
	public void aFullAdamantSetsMeleeHitsPutAQuarterPastArmor(GameTestHelper helper) {
		float netheriteHit = meleeDamageTaken(helper, NETHERITE_SET, false);
		float adamantHit = meleeDamageTaken(helper, alloySet(Alloy.ADAMANT), false);
		float expected = 0.25F * MELEE_HIT + 0.75F * netheriteHit;

		helper.assertTrue(netheriteHit < MELEE_HIT, "armor reduces the hit");
		helper.assertTrue(Math.abs(adamantHit - expected) < TOLERANCE, "a full Adamant set's hit was " + adamantHit + ", expected " + expected);
		helper.succeed();
	}

	@GameTest
	public void protectionStillReducesAFullAdamantSetsHits(GameTestHelper helper) {
		float unprotected = meleeDamageTaken(helper, alloySet(Alloy.ADAMANT), false);
		float protectedHit = meleeDamageTaken(helper, alloySet(Alloy.ADAMANT), true);
		float protectedNetheriteHit = meleeDamageTaken(helper, NETHERITE_SET, true);

		helper.assertTrue(protectedHit < unprotected, "Protection reduces a full Adamant set's hit");
		helper.assertTrue(protectedHit > protectedNetheriteHit, "a full Adamant set still hits harder through Protection");
		helper.succeed();
	}

	@GameTest
	public void aFullAdamantSetsArrowsDontPierceArmor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie netheriteArcher = attacker(helper, NETHERITE_SET);
		Zombie adamantArcher = attacker(helper, alloySet(Alloy.ADAMANT));
		Arrow arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);

		float netheriteShot = damageTaken(armoredTarget(helper, false), level.damageSources().arrow(arrow, netheriteArcher), MELEE_HIT);
		float adamantShot = damageTaken(armoredTarget(helper, false), level.damageSources().arrow(arrow, adamantArcher), MELEE_HIT);

		helper.assertValueEqual(adamantShot, netheriteShot, "a full Adamant set's arrow damage");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void aFullOrichalcumSetSavesFromALethalHitWithTotemEffects(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		helper.assertTrue(OrichalcumSave.isReady(player), "the save starts ready");

		wearSet(player, Alloy.ORICHALCUM);
		hitLethally(helper, player);

		helper.assertTrue(player.isAlive(), "the player is saved");
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "the save gives the Totem's Regeneration");
		helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "the save gives the Totem's Absorption");
		helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "the save gives the Totem's Fire Resistance");
		helper.assertFalse(OrichalcumSave.isReady(player), "the save is recharging");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void threeOrichalcumPiecesDontSave(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ORICHALCUM);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		hitLethally(helper, player);

		helper.assertFalse(player.isAlive(), "the player dies");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void theOrichalcumSaveComesBeforeAHeldTotemThenLeavesItToSave(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ORICHALCUM);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
		hitLethally(helper, player);
		helper.assertTrue(player.getMainHandItem().is(Items.TOTEM_OF_UNDYING), "the Totem is kept");

		// Harder than the first hit, which vanilla's damage cooldown would otherwise ignore.
		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2000.0F);
		helper.assertTrue(player.isAlive(), "the Totem saves while the set recharges");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the Totem is used up");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void theOrichalcumSaveComesBeforeACrystalHeartInEitherHand(GameTestHelper helper) {
		for (InteractionHand hand : InteractionHand.values()) {
			ServerPlayer player = survivalPlayer(helper);

			wearSet(player, Alloy.ORICHALCUM);
			player.setItemInHand(hand, new ItemStack(MapleItems.CRYSTAL_HEART));
			hitLethally(helper, player);

			helper.assertTrue(player.isAlive(), "the player is saved");
			helper.assertTrue(player.getItemInHand(hand).is(MapleItems.CRYSTAL_HEART), "the Crystal Heart in the " + hand + " is kept");
		}

		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void theOrichalcumSaveDoesntStopAKill(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ORICHALCUM);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertFalse(player.isAlive(), "the killed player dies");
		helper.assertTrue(OrichalcumSave.isReady(respawned), "a kill starts no recharge");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:orichalcum_quick_recharge", maxTicks = 100)
	public void theOrichalcumSaveRechargesWhileTheSetIsOff(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ORICHALCUM);
		hitLethally(helper, player);
		wearNothing(player);

		helper.startSequence()
			.thenExecuteAfter(QUICK_RECHARGE_TICKS - MARGIN, () -> helper.assertFalse(OrichalcumSave.isReady(player), "the save is still recharging"))
			.thenExecuteAfter(2 * MARGIN, () -> helper.assertTrue(OrichalcumSave.isReady(player), "the save has recharged"))
			.thenSucceed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal")
	public void dyingAndRelogingKeepTheOrichalcumRecharge(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		wearSet(player, Alloy.ORICHALCUM);
		hitLethally(helper, player);

		ServerPlayer respawned = dieAndRespawn(helper, player);

		helper.assertFalse(OrichalcumSave.isReady(respawned), "the recharge goes on after death");
		helper.assertFalse(OrichalcumSave.isReady(relog(helper, respawned)), "the recharge goes on after relogging");
		helper.succeed();
	}

	@GameTest
	public void wearingEveryAlloySetGrantsTheChallenge(GameTestHelper helper) {
		ServerPlayer player = standingPlayer(helper);
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(WEAR_EVERY_SET));

		helper.assertTrue(advancement != null, WEAR_EVERY_SET + " exists");
		helper.assertValueEqual(advancement.value().parent(), Optional.of(Identifier.parse("maple_flavored:nether/smelt_alloy_ingot")), "parent");
		helper.assertValueEqual(advancement.value().display().map(DisplayInfo::type), Optional.of(AdvancementType.CHALLENGE), "frame");
		helper.assertValueEqual(advancement.value().rewards().experience(), 100, "XP reward");

		for (Alloy alloy : Alloy.values()) {
			helper.assertFalse(player.getAdvancements().getOrStartProgress(advancement).isDone(), "the challenge before wearing " + alloy.path());

			wearSet(player, alloy);
			player.doTick();
		}

		helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "the challenge after wearing every set");
		helper.succeed();
	}

	private static void assertBlockSpeedFactor(GameTestHelper helper, Block block) {
		helper.setBlock(FLOOR_POS, block);

		ServerPlayer player = standingPlayer(helper);

		wearSet(player, Alloy.SHAKUDO);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		helper.assertTrue(blockSpeedFactor(player) < 1.0F, block + " slows three Shakudo pieces");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AlloyGear.of(Alloy.SHAKUDO, GearKind.HELMET)));
		helper.assertValueEqual(blockSpeedFactor(player), 1.0F, block + "'s speed factor for a full Shakudo set");
	}

	private static void assertNeverStuck(GameTestHelper helper, Block block) {
		ServerPlayer player = standingPlayer(helper);
		Vec3 slowdown = new Vec3(0.25, 0.05, 0.25);

		wearSet(player, Alloy.SHAKUDO);
		player.makeStuckInBlock(block.defaultBlockState(), slowdown);
		helper.assertValueEqual(stuckSpeedMultiplier(player), Vec3.ZERO, block + "'s slowdown on a full Shakudo set");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		player.makeStuckInBlock(block.defaultBlockState(), slowdown);
		helper.assertValueEqual(stuckSpeedMultiplier(player), slowdown, block + "'s slowdown on three Shakudo pieces");
	}

	private static double knockedBackDistance(GameTestHelper helper, ServerPlayer player) {
		player.setDeltaMovement(Vec3.ZERO);
		player.knockback(1.0, 1.0, 0.0, helper.getLevel().damageSources().generic(), 1.0F);

		return player.getDeltaMovement().horizontalDistance();
	}

	private static float meleeDamageTaken(GameTestHelper helper, List<Item> attackerArmor, boolean protectedTarget) {
		Zombie attacker = attacker(helper, attackerArmor);

		return damageTaken(armoredTarget(helper, protectedTarget), helper.getLevel().damageSources().mobAttack(attacker), MELEE_HIT);
	}

	private static Zombie attacker(GameTestHelper helper, List<Item> armor) {
		Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, ATTACKER_POS);

		wearSet(attacker, armor);

		return attacker;
	}

	// A fresh target per hit, so no hit lands in another's invulnerability window.
	private static Zombie armoredTarget(GameTestHelper helper, boolean withProtection) {
		Zombie target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, TARGET_POS);
		Holder<Enchantment> protection = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);

		wearSet(target, NETHERITE_SET);

		if (withProtection) {
			for (EquipmentSlot slot : SET_SLOTS) target.getItemBySlot(slot).enchant(protection, 4);
		}

		return target;
	}

	private static float damageTaken(LivingEntity entity, DamageSource source, float damage) {
		float before = entity.getHealth();

		entity.hurtServer((ServerLevel) entity.level(), source, damage);

		return before - entity.getHealth();
	}

	private static ServerPlayer standingPlayer(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);

		player.snapTo(helper.absoluteVec(STANDING_POS));

		return player;
	}

	private static List<Item> alloySet(Alloy alloy) {
		return SET_KINDS.stream().map(kind -> AlloyGear.of(alloy, kind)).toList();
	}

	private static void wearSet(LivingEntity entity, Alloy alloy) {
		wearSet(entity, alloySet(alloy));
	}

	private static void wearSet(LivingEntity entity, List<Item> armor) {
		for (int piece = 0; piece < SET_SLOTS.size(); piece++) entity.setItemSlot(SET_SLOTS.get(piece), new ItemStack(armor.get(piece)));
	}

	private static void wearNothing(LivingEntity entity) {
		for (EquipmentSlot slot : SET_SLOTS) entity.setItemSlot(slot, ItemStack.EMPTY);
	}

	private static void wither(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60));
	}

	private static MobEffectInstance levitation() {
		return new MobEffectInstance(MobEffects.LEVITATION, 200);
	}

	private static void hitLethally(GameTestHelper helper, ServerPlayer player) {
		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F);
	}

	private static float blockSpeedFactor(ServerPlayer player) {
		return ((EntityAccessor) player).maple$blockSpeedFactor();
	}

	private static Vec3 stuckSpeedMultiplier(ServerPlayer player) {
		return ((EntityAccessor) player).maple$stuckSpeedMultiplier();
	}
}

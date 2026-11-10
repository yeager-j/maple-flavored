package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;
import static mapleflavored.gametest.TestItems.enchantment;

import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.hearts.Hearts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Blight: a hit gives Wither I for 4 seconds per level, each hit refreshes it, and it can't join Fire Aspect.
 *
 * <p>Hits are made by a mock player. Mock players aren't ticked by a connection, so their own effects never run out;
 * the effect's countdown is tested on a cow.
 */
public class BlightGameTests {
	private static final ResourceKey<Enchantment> BLIGHT = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("blight"));
	private static final BlockPos VICTIM_SPOT = new BlockPos(1, 1, 1);
	private static final int TICKS_PER_LEVEL = 4 * 20;
	private static final int WITHERING_INTERVAL = 100;
	private static final int MARGIN = 10;

	@GameTest
	public void aHitGivesWitherOneForFourSecondsPerLevel(GameTestHelper helper) {
		for (int level = 1; level <= 2; level++) {
			Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, VICTIM_SPOT);

			attackerWithBlight(helper, level).attack(cow);
			assertWither(helper, cow, level * TICKS_PER_LEVEL);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void eachHitRefreshesTheWither(GameTestHelper helper) {
		ServerPlayer attacker = attackerWithBlight(helper, 1);
		Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, VICTIM_SPOT);
		int beforeRunningOut = TICKS_PER_LEVEL - 20;

		attacker.attack(cow);

		helper.startSequence()
			.thenExecuteAfter(beforeRunningOut, () -> {
				helper.assertTrue(cow.getEffect(MobEffects.WITHER).getDuration() <= 20, "the first hit's Wither ran down");
				attacker.attack(cow);
				assertWither(helper, cow, TICKS_PER_LEVEL);
			})
			.thenExecuteAfter(TICKS_PER_LEVEL - beforeRunningOut + MARGIN, () -> {
				helper.assertTrue(cow.hasEffect(MobEffects.WITHER), "Wither outlasts the first hit's 4 seconds");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void repeatedHitsKeepWitheringAPlayersHearts(GameTestHelper helper) {
		ServerPlayer attacker = attackerWithBlight(helper, 1);
		ServerPlayer victim = survivalPlayer(helper);
		int rehit = WITHERING_INTERVAL / 2 + MARGIN;

		attacker.attack(victim);
		assertWither(helper, victim, TICKS_PER_LEVEL);

		helper.startSequence()
			.thenExecuteAfter(rehit, () -> {
				// Hurt immunity counts down in the victim's own tick, which a mock player doesn't run.
				victim.setInvulnerableTime(0);
				attacker.attack(victim);
				assertWither(helper, victim, TICKS_PER_LEVEL);
			})
			.thenExecuteAfter(WITHERING_INTERVAL - rehit + MARGIN, () -> {
				helper.assertValueEqual(Hearts.witheredHearts(victim), 1, "Withered Hearts");
			})
			.thenSucceed();
	}

	@GameTest
	public void blightAndFireAspectExcludeEachOther(GameTestHelper helper) {
		Holder<Enchantment> blight = enchantment(helper, BLIGHT);
		Holder<Enchantment> fireAspect = enchantment(helper, Enchantments.FIRE_ASPECT);

		helper.assertFalse(Enchantment.areCompatible(blight, fireAspect), "Blight joins Fire Aspect");
		helper.assertFalse(Enchantment.areCompatible(fireAspect, blight), "Fire Aspect joins Blight");
		helper.assertFalse(EnchantmentHelper.isEnchantmentCompatible(List.of(fireAspect), blight), "Blight goes on a Fire Aspect weapon");
		helper.assertTrue(
			EnchantmentHelper.isEnchantmentCompatible(List.of(enchantment(helper, Enchantments.SHARPNESS)), blight),
			"Blight goes on a Sharpness weapon"
		);
		helper.succeed();
	}

	@GameTest
	public void blightGoesOnFireAspectsWeapons(GameTestHelper helper) {
		Enchantment blight = enchantment(helper, BLIGHT).value();
		Enchantment fireAspect = enchantment(helper, Enchantments.FIRE_ASPECT).value();

		helper.assertValueEqual(blight.getMaxLevel(), 2, "Blight's max level");
		helper.assertValueEqual(blight.getSupportedItems().unwrapKey(), fireAspect.getSupportedItems().unwrapKey(), "supported items");
		helper.assertValueEqual(blight.definition().primaryItems(), fireAspect.definition().primaryItems(), "primary items");
		helper.assertValueEqual(blight.definition().slots(), fireAspect.definition().slots(), "slots");
		helper.succeed();
	}

	private static ServerPlayer attackerWithBlight(GameTestHelper helper, int level) {
		ServerPlayer attacker = survivalPlayer(helper);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);

		sword.enchant(enchantment(helper, BLIGHT), level);
		attacker.setItemInHand(InteractionHand.MAIN_HAND, sword);

		return attacker;
	}

	private static void assertWither(GameTestHelper helper, LivingEntity victim, int ticks) {
		MobEffectInstance wither = victim.getEffect(MobEffects.WITHER);

		helper.assertTrue(wither != null, "the victim has Wither");
		helper.assertValueEqual(wither.getAmplifier(), 0, "Wither's amplifier");
		helper.assertValueEqual(wither.getDuration(), ticks, "Wither's duration in ticks");
	}
}

package mapleflavored.gametest;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mobs are never an enchantment source: gear a mob spawned with drops at vanilla's chance without its enchantments,
 * and gear it picked up drops exactly as it was.
 */
public class SpawnedGearGameTests {
	private static final BlockPos MOB_POS = new BlockPos(2, 2, 2);
	private static final int KILLS = 300;
	// Vanilla's chance for a mob's own gear to drop, with no Looting.
	private static final double DROP_CHANCE = 0.085;

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void spawnedGearDropsAtVanillasChanceWithoutEnchantments(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		for (int i = 0; i < KILLS; i++) {
			Zombie zombie = addZombie(helper);

			zombie.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, Items.IRON_SWORD, Enchantments.SHARPNESS, 3));
			zombie.getRandom().setSeed(i);
			zombie.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
		}

		List<ItemStack> swords = droppedItems(helper, Items.IRON_SWORD);
		double mean = KILLS * DROP_CHANCE;
		double band = 4.0 * Math.sqrt(KILLS * DROP_CHANCE * (1.0 - DROP_CHANCE));

		helper.assertTrue(Math.abs(swords.size() - mean) <= band, swords.size() + " of " + KILLS + " swords dropped, expected about " + Math.round(mean));
		helper.assertTrue(swords.stream().noneMatch(ItemStack::isEnchanted), "a dropped sword kept its enchantments");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:normal", maxTicks = 100)
	public void spawnedGearSwappedForPickedUpGearDropsPlainAndThePickedUpGearDropsAsItWas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Zombie zombie = addZombie(helper);
		ItemStack found = enchanted(helper, Items.NETHERITE_SWORD, Enchantments.UNBREAKING, 2);

		found.setDamageValue(100);
		zombie.setCanPickUpLoot(true);
		zombie.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, Items.IRON_SWORD, Enchantments.SHARPNESS, 3));
		// Still spawned gear (any chance up to 1), but sure to drop when the zombie swaps it out.
		zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
		dropOnGround(helper, found.copy());

		helper.startSequence()
			.thenWaitUntil(() -> helper.assertValueEqual(zombie.getMainHandItem().getItem(), Items.NETHERITE_SWORD, "held sword"))
			.thenExecute(() -> {
				List<ItemStack> swapped = droppedItems(helper, Items.IRON_SWORD);

				helper.assertValueEqual(swapped.size(), 1, "swapped-out swords");
				helper.assertFalse(swapped.getFirst().isEnchanted(), "the swapped-out sword kept its enchantments");

				zombie.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);

				List<ItemStack> picked = droppedItems(helper, Items.NETHERITE_SWORD);

				helper.assertValueEqual(picked.size(), 1, "picked-up swords dropped");
				helper.assertTrue(ItemStack.matches(picked.getFirst(), found), "the picked-up sword dropped as it was: " + picked.getFirst());
			})
			.thenSucceed();
	}

	private static Zombie addZombie(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);

		zombie.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(MOB_POS)));
		level.addFreshEntity(zombie);

		return zombie;
	}

	private static ItemStack enchanted(GameTestHelper helper, Item item, ResourceKey<Enchantment> enchantment, int level) {
		ItemStack stack = new ItemStack(item);

		stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);

		return stack;
	}

	private static void dropOnGround(GameTestHelper helper, ItemStack stack) {
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(MOB_POS));

		helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack, 0.0, 0.0, 0.0));
	}

	private static List<ItemStack> droppedItems(GameTestHelper helper, Item item) {
		AABB around = new AABB(helper.absolutePos(MOB_POS)).inflate(4.0);

		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
			.map(ItemEntity::getItem)
			.filter(stack -> stack.is(item))
			.toList();
	}
}

package mapleflavored.gametest;

import static mapleflavored.gametest.Advancements.isDone;

import java.util.List;
import mapleflavored.MapleFlavored;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The boss rewards: an Elder Guardian's 2 Pith and its Crystal Heart for every player within 64 blocks, the Ender Dragon's
 * Zephyr book and Free the End's Crystal Heart for every player in its fight's range, and Sneak 100's Crystal Heart for
 * every player within 50 blocks of a Warden when it dies or digs back down.
 *
 * <p>Each test credits players across the arena, so each runs alone, in its own environment's batch, in a
 * {@link SubspaceArena}.
 */
public class BossRewardGameTests {
	private static final Identifier ELDER_GUARDIAN_ADVANCEMENT = Identifier.parse("maple_flavored:adventure/kill_elder_guardian");
	private static final Identifier FREE_THE_END = Identifier.parse("minecraft:end/kill_dragon");
	private static final Identifier SNEAK_100 = Identifier.parse("minecraft:adventure/avoid_vibration");
	private static final ResourceKey<Enchantment> ZEPHYR = ResourceKey.create(Registries.ENCHANTMENT, MapleFlavored.id("zephyr"));

	// A dragon fight counts players within 192 blocks of the point 128 blocks above its origin.
	private static final double DRAGON_FIGHT_RANGE = 192.0;
	private static final int DRAGON_FIGHT_HEIGHT = 128;

	// As in SubspaceGameTests: short enough to watch a Warden dig down, long enough for a sniff to come first.
	private static final int DIG_COOLDOWN_TICKS = 600;

	@GameTest(environment = "maple_flavored_gametest:boss_rewards_elder_guardian_drops", maxTicks = 100)
	public void anElderGuardianDropsTwoPith(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ElderGuardian guardian = arena.still(EntityTypes.ELDER_GUARDIAN, Vec3.ZERO);

		guardian.kill(level);

		int pith = drops(level, guardian).stream().filter(drop -> drop.is(MapleItems.PITH)).mapToInt(ItemStack::getCount).sum();

		helper.assertValueEqual(pith, 2, "Pith dropped");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:boss_rewards_elder_guardian_kill", maxTicks = 100)
	public void anElderGuardiansDeathCreditsEveryPlayerWithin64Blocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer atRange = arena.player(new Vec3(64.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer creative = arena.player(new Vec3(-30.0, 0.0, 0.0), GameType.CREATIVE);
		ServerPlayer pastRange = arena.player(new Vec3(-65.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer spectator = arena.player(new Vec3(10.0, 0.0, 0.0), GameType.SPECTATOR);
		ElderGuardian guardian = arena.still(EntityTypes.ELDER_GUARDIAN, Vec3.ZERO);

		guardian.kill(level);

		for (ServerPlayer player : List.of(atRange, creative)) {
			helper.assertTrue(isDone(helper, player, ELDER_GUARDIAN_ADVANCEMENT), "the Elder Guardian kill is granted within 64 blocks");
			helper.assertValueEqual(player.getInventory().countItem(Hearts.CRYSTAL_HEART), 1, "Crystal Hearts rewarded");
		}

		helper.assertFalse(isDone(helper, pastRange, ELDER_GUARDIAN_ADVANCEMENT), "the Elder Guardian kill is granted 65 blocks out");
		helper.assertFalse(isDone(helper, spectator, ELDER_GUARDIAN_ADVANCEMENT), "the Elder Guardian kill is granted to a spectator");
		helper.succeed();
	}

	@GameTest(environment = "maple_flavored_gametest:boss_rewards_dragon_drops", maxTicks = 100)
	public void theEnderDragonDropsAZephyrBookEveryKill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		EnderDragon dragon = arena.still(EntityTypes.ENDER_DRAGON, new Vec3(0.0, 5.0, 0.0));

		killWithoutKiller(level, dragon);

		Holder<Enchantment> zephyr = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ZEPHYR);
		List<ItemStack> books = drops(level, dragon).stream().filter(drop -> drop.is(Items.ENCHANTED_BOOK)).toList();

		dragon.discard();
		helper.assertValueEqual(books.size(), 1, "enchanted books dropped");

		ItemEnchantments stored = books.getFirst().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		helper.assertValueEqual(stored.size(), 1, "enchantments on the book");
		helper.assertValueEqual(stored.getLevel(zephyr), 1, "the book's Zephyr level");
		helper.succeed();
	}

	// Killed by an explosion, so no player has vanilla's kill credit, and only the fight's range shares Free the End.
	@GameTest(environment = "maple_flavored_gametest:boss_rewards_dragon_kill", maxTicks = 100)
	public void theEnderDragonsDeathCreditsFreeTheEndToEveryPlayerInItsFight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		List<ServerPlayer> inFight = List.of(
			arena.player(new Vec3(4.0, 0.0, 0.0), GameType.SURVIVAL),
			arena.player(new Vec3(-20.0, 0.0, 0.0), GameType.SURVIVAL)
		);
		ServerPlayer pastFight = arena.player(new Vec3(10.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer spectator = arena.player(new Vec3(-4.0, 0.0, 0.0), GameType.SPECTATOR);
		// The fight's centre sits 185 blocks back along x, so the first player stands just inside its 192-block range and
		// the second just past it.
		Vec3 fightCentre = arena.at(new Vec3(-185.0, 0.0, 0.0));
		BlockPos origin = BlockPos.containing(fightCentre).below(DRAGON_FIGHT_HEIGHT);

		Vec3 rangeCentre = Vec3.atLowerCornerOf(origin.above(DRAGON_FIGHT_HEIGHT));

		helper.assertTrue(inFight.getFirst().position().distanceTo(rangeCentre) < DRAGON_FIGHT_RANGE, "the first player is in range");
		helper.assertTrue(pastFight.position().distanceTo(rangeCentre) > DRAGON_FIGHT_RANGE, "the last player is out of range");

		EnderDragon fightless = arena.still(EntityTypes.ENDER_DRAGON, new Vec3(0.0, 5.0, 0.0));

		killWithoutKiller(level, fightless);
		fightless.discard();

		for (ServerPlayer player : List.of(inFight.getFirst(), inFight.getLast(), pastFight, spectator)) {
			helper.assertFalse(isDone(helper, player, FREE_THE_END), "Free the End is granted for a dragon outside a fight");
		}

		EnderDragonFight fight = EnderDragonFight.createDefault();
		EnderDragon fought = arena.still(EntityTypes.ENDER_DRAGON, new Vec3(0.0, 5.0, 0.0));

		fight.init(level, 0L, origin);
		fought.setDragonFight(fight);
		killWithoutKiller(level, fought);
		// A dying dragon in a fight ends it once its death animation runs out, which would build an exit portal here.
		fought.discard();

		for (ServerPlayer player : inFight) {
			helper.assertTrue(isDone(helper, player, FREE_THE_END), "Free the End is granted in the fight's range");
			helper.assertValueEqual(player.getInventory().countItem(Hearts.CRYSTAL_HEART), 1, "Crystal Hearts rewarded");
		}

		helper.assertFalse(isDone(helper, pastFight, FREE_THE_END), "Free the End is granted past the fight's range");
		helper.assertFalse(isDone(helper, spectator, FREE_THE_END), "Free the End is granted to a spectator");
		helper.succeed();
	}

	// The killed Warden stands 30 blocks from its Subspace's centre, so the credit is measured from the Warden: one player
	// is near it but outside the Subspace, another inside the Subspace but far from it.
	@GameTest(environment = "maple_flavored_gametest:boss_rewards_warden_death", maxTicks = 100)
	public void aWardensDeathCreditsEveryPlayerWithin50BlocksOfIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		List<ServerPlayer> near = List.of(
			arena.player(new Vec3(75.0, 0.0, 0.0), GameType.SURVIVAL),
			arena.player(new Vec3(10.0, 0.0, 0.0), GameType.SURVIVAL)
		);
		ServerPlayer far = arena.player(new Vec3(-40.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer spectator = arena.player(new Vec3(35.0, 0.0, 0.0), GameType.SPECTATOR);
		Warden discarded = arena.stillWarden(Vec3.ZERO);

		discarded.discard();

		for (ServerPlayer player : near) {
			helper.assertFalse(isDone(helper, player, SNEAK_100), "a discarded Warden credits surviving it");
		}

		Warden killed = arena.stillWarden(Vec3.ZERO);
		Vec3 wandered = arena.at(new Vec3(30.0, 0.0, 0.0));

		killed.snapTo(wandered.x, wandered.y, wandered.z);
		killed.kill(level);

		for (ServerPlayer player : near) {
			helper.assertTrue(isDone(helper, player, SNEAK_100), "Sneak 100 is granted within 50 blocks of the Warden");
			helper.assertValueEqual(player.getInventory().countItem(Hearts.CRYSTAL_HEART), 1, "Crystal Hearts rewarded");
		}

		helper.assertFalse(isDone(helper, far, SNEAK_100), "Sneak 100 is granted 70 blocks from the Warden");
		helper.assertFalse(isDone(helper, spectator, SNEAK_100), "Sneak 100 is granted to a spectator");
		helper.succeed();
	}

	// The environment sets the Warden survival range to 20 blocks.
	@GameTest(environment = "maple_flavored_gametest:boss_rewards_warden_range", maxTicks = 100)
	public void theWardenSurvivalRangeGameRuleSetsHowNearPlayersMustBe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SubspaceArena arena = SubspaceArena.cleared(helper);
		ServerPlayer inRange = arena.player(new Vec3(20.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer pastRange = arena.player(new Vec3(-21.0, 0.0, 0.0), GameType.SURVIVAL);
		Warden warden = arena.stillWarden(Vec3.ZERO);

		warden.kill(level);

		helper.assertTrue(isDone(helper, inRange, SNEAK_100), "Sneak 100 is granted 20 blocks out");
		helper.assertFalse(isDone(helper, pastRange, SNEAK_100), "Sneak 100 is granted 21 blocks out");
		helper.succeed();
	}

	// The near player stands past vanilla's awareness box, so the penned Warden digs down.
	@GameTest(environment = "maple_flavored_gametest:boss_rewards_warden_digging", maxTicks = 1100)
	public void aWardenDiggingDownCreditsEveryPlayerWithin50BlocksOfIt(GameTestHelper helper) {
		SubspaceArena arena = SubspaceArena.cleared(helper);

		arena.pen();

		Warden warden = arena.warden(Vec3.ZERO);
		ServerPlayer near = arena.player(new Vec3(40.0, 0.0, 0.0), GameType.SURVIVAL);
		ServerPlayer far = arena.player(new Vec3(-51.0, 0.0, 0.0), GameType.SURVIVAL);

		warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, DIG_COOLDOWN_TICKS);

		helper.succeedWhen(() -> {
			helper.assertTrue(warden.isRemoved(), "the Warden dug down");
			helper.assertValueEqual(warden.getRemovalReason(), Entity.RemovalReason.DISCARDED, "how the Warden was removed");
			helper.assertTrue(isDone(helper, near, SNEAK_100), "Sneak 100 is granted 40 blocks from the Warden");
			helper.assertValueEqual(near.getInventory().countItem(Hearts.CRYSTAL_HEART), 1, "Crystal Hearts rewarded");
			helper.assertFalse(isDone(helper, far, SNEAK_100), "Sneak 100 is granted 51 blocks from the Warden");
		});
	}

	// Explosions always hurt a dragon, and leave no player with the kill.
	private static void killWithoutKiller(ServerLevel level, EnderDragon dragon) {
		dragon.hurtServer(level, level.damageSources().explosion(null, null), 10_000.0F);
	}

	private static List<ItemStack> drops(ServerLevel level, Entity boss) {
		return level.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(boss.position(), 16.0, 16.0, 16.0))
			.stream()
			.map(ItemEntity::getItem)
			.toList();
	}
}

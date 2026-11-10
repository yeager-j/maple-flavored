package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import mapleflavored.alloys.Alloy;
import mapleflavored.alloys.AlloyGear;
import mapleflavored.alloys.GearKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

/**
 * Illager book drops: on a player kill, Vindicators roll Sharpness and Sweeping Edge, Pillager Captains Power, Punch and
 * Infinity, Evokers one Protection and Ravagers Knockback, each its own roll. Each Looting level adds 20% of the base
 * chance, and Orichalcum's drop bonus adds nothing. Illagers a raid spawned for a wave drop no books; Ravagers always can.
 *
 * <p>Each test rolls the mob's own loot table the way {@code LivingEntity.dropFromLootTable} does, many times from one
 * seeded random, so the rates are fixed.
 */
public class IllagerBookDropGameTests {
	private static final int KILLS = 4000;
	// Enough kills that two rare books dropping together can't pass as never dropping together.
	private static final int JOINT_KILLS = 40000;
	private static final long SEED = 7L;
	// A seeded rate must land within this many binomial standard deviations of the expected rate.
	private static final double RATE_TOLERANCE = 4.0;
	private static final double LOOTING_BONUS_PER_LEVEL = 0.2;
	private static final BlockPos MOB_POS = new BlockPos(1, 1, 1);

	private static final List<Book> BOOKS = List.of(
		new Book(Dropper.VINDICATOR, Enchantments.SHARPNESS, 0.10),
		new Book(Dropper.VINDICATOR, Enchantments.SWEEPING_EDGE, 0.025),
		new Book(Dropper.CAPTAIN, Enchantments.POWER, 0.20),
		new Book(Dropper.CAPTAIN, Enchantments.PUNCH, 0.05),
		new Book(Dropper.CAPTAIN, Enchantments.INFINITY, 0.05),
		new Book(Dropper.EVOKER, Enchantments.PROTECTION, 0.15),
		new Book(Dropper.RAVAGER, Enchantments.KNOCKBACK, 0.10)
	);

	@GameTest
	public void eachBookDropsAtItsBaseChanceOnAPlayerKill(GameTestHelper helper) {
		ServerPlayer player = player(helper, ItemStack.EMPTY);

		for (Dropper dropper : Dropper.values()) {
			Kills kills = Kills.of(helper, dropper.spawn(helper), player, KILLS);

			for (Book book : dropper.books()) {
				kills.assertRate(book.enchantment(), book.base());
			}

			kills.assertOnlyBooks(dropper.books());
		}

		helper.succeed();
	}

	@GameTest
	public void eachBookIsItsOwnRoll(GameTestHelper helper) {
		ServerPlayer player = player(helper, ItemStack.EMPTY);
		Kills captain = Kills.of(helper, Dropper.CAPTAIN.spawn(helper), player, JOINT_KILLS);
		Kills vindicator = Kills.of(helper, Dropper.VINDICATOR.spawn(helper), player, JOINT_KILLS);

		captain.assertJointRate(Enchantments.POWER, Enchantments.PUNCH, 0.20 * 0.05);
		captain.assertJointRate(Enchantments.PUNCH, Enchantments.INFINITY, 0.05 * 0.05);
		vindicator.assertJointRate(Enchantments.SHARPNESS, Enchantments.SWEEPING_EDGE, 0.10 * 0.025);
		helper.succeed();
	}

	@GameTest
	public void plainPillagersDropNoBooks(GameTestHelper helper) {
		Kills kills = Kills.of(helper, helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, MOB_POS), player(helper, ItemStack.EMPTY), KILLS);

		kills.assertNoBooks();
		helper.succeed();
	}

	@GameTest
	public void evokersDropOneProtectionBookAtMost(GameTestHelper helper) {
		Kills kills = Kills.of(helper, Dropper.EVOKER.spawn(helper), player(helper, ItemStack.EMPTY), KILLS);

		helper.assertTrue(kills.books.stream().allMatch(books -> books.size() <= 1), "no Evoker kill drops more than one book");
		helper.succeed();
	}

	@GameTest
	public void illagersDropNoBooksUnlessAPlayerKillsThem(GameTestHelper helper) {
		for (Dropper dropper : Dropper.values()) {
			Kills.of(helper, dropper.spawn(helper), null, KILLS).assertNoBooks();
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void eachLootingLevelAddsAFifthOfTheBaseChance(GameTestHelper helper) {
		for (int looting : List.of(1, 3)) {
			ServerPlayer player = player(helper, lootingSword(helper, new ItemStack(Items.IRON_SWORD), looting));

			assertBookRates(helper, player, 1.0 + LOOTING_BONUS_PER_LEVEL * looting);
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void orichalcumAddsNothingToBookChances(GameTestHelper helper) {
		assertBookRates(helper, player(helper, orichalcumSword()), 1.0);
		assertBookRates(helper, player(helper, lootingSword(helper, orichalcumSword(), 3)), 1.0 + LOOTING_BONUS_PER_LEVEL * 3);
		helper.succeed();
	}

	@GameTest
	public void illagersARaidSpawnsForAWaveDropNoBooks(GameTestHelper helper) {
		Raid raid = raid(helper);
		ServerPlayer player = player(helper, ItemStack.EMPTY);
		Pillager leader = EntityTypes.PILLAGER.create(helper.getLevel(), EntitySpawnReason.EVENT);

		// As Raid.spawnGroup makes its wave's leader, which finalizeSpawn then gives the ominous banner.
		leader.setPatrolLeader(true);

		List<Raider> wave = List.of(
			waveRaider(helper, raid, EntityTypes.VINDICATOR.create(helper.getLevel(), EntitySpawnReason.EVENT)),
			waveRaider(helper, raid, leader),
			waveRaider(helper, raid, EntityTypes.EVOKER.create(helper.getLevel(), EntitySpawnReason.EVENT))
		);

		helper.assertTrue(leader.isCaptain(), "the wave's leader is a Pillager Captain");

		for (Raider raider : wave) {
			Kills.of(helper, raider, player, KILLS).assertNoBooks();
		}

		helper.succeed();
	}

	@GameTest
	public void ravagersARaidSpawnsForAWaveStillDropKnockback(GameTestHelper helper) {
		Raider ravager = waveRaider(helper, raid(helper), EntityTypes.RAVAGER.create(helper.getLevel(), EntitySpawnReason.EVENT));

		Kills.of(helper, ravager, player(helper, ItemStack.EMPTY), KILLS).assertRate(Enchantments.KNOCKBACK, 0.10);
		helper.succeed();
	}

	@GameTest
	public void illagersThatJoinARaidKeepTheirBooks(GameTestHelper helper) {
		Raid raid = raid(helper);
		ServerPlayer player = player(helper, ItemStack.EMPTY);

		for (Dropper dropper : List.of(Dropper.VINDICATOR, Dropper.CAPTAIN, Dropper.EVOKER)) {
			Raider raider = (Raider) dropper.spawn(helper);

			// As Raider.aiStep recruits a raider already in the world.
			raid.joinRaid(helper.getLevel(), 1, raider, null, true);
			helper.assertTrue(raider.hasRaid(), dropper + " joined the raid");

			Kills kills = Kills.of(helper, raider, player, KILLS);

			for (Book book : dropper.books()) {
				kills.assertRate(book.enchantment(), book.base());
			}
		}

		helper.succeed();
	}

	@GameTest
	public void aWaveIllagerStillDropsNoBooksAfterReloading(GameTestHelper helper) {
		Raider vindicator = waveRaider(helper, raid(helper), EntityTypes.VINDICATOR.create(helper.getLevel(), EntitySpawnReason.EVENT));
		Mob reloaded = reload(helper, vindicator);

		Kills.of(helper, reloaded, player(helper, ItemStack.EMPTY), KILLS).assertNoBooks();
		helper.succeed();
	}

	// Asserts every book's rate when the player kills its mob, as a multiple of the book's base chance.
	private static void assertBookRates(GameTestHelper helper, ServerPlayer player, double multiplier) {
		for (Dropper dropper : Dropper.values()) {
			Kills kills = Kills.of(helper, dropper.spawn(helper), player, KILLS);

			for (Book book : dropper.books()) {
				kills.assertRate(book.enchantment(), book.base() * multiplier);
			}
		}
	}

	private static Raid raid(GameTestHelper helper) {
		return new Raid(helper.absolutePos(MOB_POS), Difficulty.NORMAL);
	}

	// Joins the new raider to the raid as Raid.spawnGroup does, which spawns it into the level.
	private static Raider waveRaider(GameTestHelper helper, Raid raid, @Nullable Raider raider) {
		helper.assertTrue(raider != null, "the wave raider is created");
		raid.joinRaid(helper.getLevel(), 1, raider, helper.absolutePos(MOB_POS), false);
		helper.assertTrue(helper.getLevel().getEntity(raider.getUUID()) == raider, "the wave raider is spawned");

		return raider;
	}

	// Saves the mob the way its chunk would on unloading and loads it back in its place.
	private static Mob reload(GameTestHelper helper, Mob mob) {
		ServerLevel level = helper.getLevel();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());

		mob.save(output);
		mob.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

		CompoundTag saved = output.buildResult();
		var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved);
		Entity loaded = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, entity -> entity);

		helper.assertTrue(loaded instanceof Mob, "the saved mob loads");
		helper.assertTrue(level.addFreshEntity(loaded), "the saved mob is added back");

		return (Mob) loaded;
	}

	private static ServerPlayer player(GameTestHelper helper, ItemStack mainHand) {
		ServerPlayer player = MockPlayers.survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);

		return player;
	}

	private static ItemStack orichalcumSword() {
		return new ItemStack(AlloyGear.of(Alloy.ORICHALCUM, GearKind.SWORD));
	}

	private static ItemStack lootingSword(GameTestHelper helper, ItemStack sword, int level) {
		sword.enchant(enchantment(helper, Enchantments.LOOTING), level);

		return sword;
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/** The mobs that drop books, each spawned standing still in the test structure. */
	private enum Dropper {
		VINDICATOR,
		CAPTAIN,
		EVOKER,
		RAVAGER;

		Mob spawn(GameTestHelper helper) {
			return switch (this) {
				case VINDICATOR -> helper.spawnWithNoFreeWill(EntityTypes.VINDICATOR, MOB_POS);
				case CAPTAIN -> captain(helper);
				case EVOKER -> helper.spawnWithNoFreeWill(EntityTypes.EVOKER, MOB_POS);
				case RAVAGER -> helper.spawnWithNoFreeWill(EntityTypes.RAVAGER, MOB_POS);
			};
		}

		List<Book> books() {
			return BOOKS.stream().filter(book -> book.dropper() == this).toList();
		}

		private static Pillager captain(GameTestHelper helper) {
			Pillager captain = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, MOB_POS);
			var bannerPatterns = helper.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);

			captain.setPatrolLeader(true);
			captain.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(bannerPatterns));
			helper.assertTrue(captain.isCaptain(), "the Pillager is a Captain");

			return captain;
		}
	}

	/** A book a mob drops, at its chance without Looting. */
	private record Book(Dropper dropper, ResourceKey<Enchantment> enchantment, double base) {
	}

	/** The enchanted books each of many kills of one mob dropped. */
	private record Kills(GameTestHelper helper, String mob, List<List<ItemStack>> books) {
		// Rolls the mob's loot table the way LivingEntity.dropFromLootTable does, killed by the player or by nothing.
		static Kills of(GameTestHelper helper, Mob mob, @Nullable ServerPlayer killer, int kills) {
			ServerLevel level = helper.getLevel();
			LootTable table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable().orElseThrow());
			LootParams.Builder params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, mob)
				.withParameter(LootContextParams.ORIGIN, mob.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, killer == null ? level.damageSources().generic() : level.damageSources().playerAttack(killer));

			if (killer != null) {
				params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).withParameter(LootContextParams.ATTACKING_ENTITY, killer);
			}

			LootParams lootParams = params.create(LootContextParamSets.ENTITY);
			RandomSource random = RandomSource.create(SEED);
			List<List<ItemStack>> books = new ArrayList<>();

			for (int kill = 0; kill < kills; kill++) {
				books.add(table.getRandomItems(lootParams, random).stream().filter(drop -> drop.is(Items.ENCHANTED_BOOK)).toList());
			}

			return new Kills(helper, mob.getType().toShortString(), books);
		}

		void assertRate(ResourceKey<Enchantment> enchantment, double expected) {
			assertRateOfKillsDroppingAll(List.of(enchantment), expected);
		}

		void assertJointRate(ResourceKey<Enchantment> first, ResourceKey<Enchantment> second, double expected) {
			assertRateOfKillsDroppingAll(List.of(first, second), expected);
		}

		void assertOnlyBooks(List<Book> allowed) {
			for (List<ItemStack> kill : books) {
				for (ItemStack book : kill) {
					ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

					helper.assertValueEqual(stored.size(), 1, mob + " book's enchantment count");

					Holder<Enchantment> enchantment = stored.keySet().iterator().next();
					boolean isAllowed = allowed.stream().anyMatch(each -> enchantment.is(each.enchantment()));

					helper.assertTrue(isAllowed, mob + " dropped a book of " + enchantment.getRegisteredName());
					helper.assertValueEqual(stored.getLevel(enchantment), 1, mob + " book's level");
				}
			}
		}

		void assertNoBooks() {
			long dropping = books.stream().filter(kill -> !kill.isEmpty()).count();

			helper.assertValueEqual(dropping, 0L, mob + " kills dropping books");
		}

		private void assertRateOfKillsDroppingAll(List<ResourceKey<Enchantment>> enchantments, double expected) {
			List<Holder<Enchantment>> holders = enchantments.stream().map(key -> enchantment(helper, key)).toList();
			long dropping = books.stream().filter(kill -> holders.stream().allMatch(holder -> dropped(kill, holder))).count();
			double rate = (double) dropping / books.size();
			double tolerance = RATE_TOLERANCE * Math.sqrt(expected * (1.0 - expected) / books.size());
			String what = mob + " " + enchantments.stream().map(key -> key.identifier().getPath()).toList() + " rate " + rate;

			helper.assertTrue(Math.abs(rate - expected) <= tolerance, what + " is within " + tolerance + " of " + expected);
		}

		private static boolean dropped(List<ItemStack> kill, Holder<Enchantment> enchantment) {
			return kill.stream().anyMatch(book -> book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0);
		}
	}
}

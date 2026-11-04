package mapleflavored.gametest;

import static mapleflavored.gametest.EnchantingTables.UPPER_RING;
import static mapleflavored.gametest.EnchantingTables.book;
import static mapleflavored.gametest.EnchantingTables.enchantment;
import static mapleflavored.gametest.EnchantingTables.fill;
import static mapleflavored.gametest.EnchantingTables.fullLibraryBooks;
import static mapleflavored.gametest.EnchantingTables.fullLibraryLevels;
import static mapleflavored.gametest.EnchantingTables.grindstone;
import static mapleflavored.gametest.EnchantingTables.isDone;
import static mapleflavored.gametest.EnchantingTables.placeTable;
import static mapleflavored.gametest.EnchantingTables.shelves;
import static mapleflavored.gametest.EnchantingTables.survivalPlayer;
import static mapleflavored.gametest.EnchantingTables.table;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.MapleFlavored;
import mapleflavored.enchanting.AppleChecklist;
import mapleflavored.enchanting.AppleEffect;
import mapleflavored.enchanting.Checklist;
import mapleflavored.enchanting.EnchantingDeposit;
import mapleflavored.enchanting.EnchantingTableMenu;
import mapleflavored.experience.ExperiencePoints;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A plain golden apple is just food. The Enchanting Table turns it into an enchanted golden apple with the effects its
 * Library Progress unlocks, for an Enchantment Slot and a lapis per effect and a block of gold as its deposit, which
 * the grindstone hands back. Enchanted golden apples come only from the table, and any of them cures a zombie villager.
 */
public class GoldenAppleGameTests {
	private static final Identifier APPLE_ADVANCEMENT = MapleFlavored.id("story/enchant_golden_apple");
	private static final Identifier ENCHANTER = Identifier.withDefaultNamespace("story/enchant_item");
	private static final Identifier ZOMBIE_DOCTOR = Identifier.withDefaultNamespace("story/cure_zombie_villager");
	private static final Identifier BALANCED_DIET = Identifier.withDefaultNamespace("husbandry/balanced_diet");
	private static final ResourceKey<LootTable> APPLE_LOOT = ResourceKey.create(
		Registries.LOOT_TABLE, Identifier.parse("maple_flavored_gametest:enchanted_golden_apples")
	);
	// The first menu slot of both the table and the grindstone shows the player's first main inventory slot, 9.
	private static final int FIRST_INVENTORY_MENU_SLOT = 3;
	private static final int FIRST_MAIN_INVENTORY_SLOT = 9;
	private static final BlockPos ZOMBIE_SPOT = new BlockPos(1, 1, 7);

	@GameTest
	public void aPlainGoldenAppleIsFoodWithNoEffects(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper, 0);

		player.getFoodData().setFoodLevel(10);
		new ItemStack(Items.GOLDEN_APPLE).finishUsingItem(helper.getLevel(), player);

		helper.assertValueEqual(player.getFoodData().getFoodLevel(), 14, "food after a golden apple");
		helper.assertTrue(player.getActiveEffects().isEmpty(), "a golden apple gives effects: " + player.getActiveEffects());
		helper.succeed();
	}

	@GameTest
	public void appleEffectsUnlockAtTheirShareOfAFullLibrary(GameTestHelper helper) {
		Set<AppleEffect> regeneration = EnumSet.of(AppleEffect.REGENERATION);
		Set<AppleEffect> fireResistance = EnumSet.of(AppleEffect.REGENERATION, AppleEffect.FIRE_RESISTANCE);
		Set<AppleEffect> absorption = EnumSet.of(AppleEffect.REGENERATION, AppleEffect.FIRE_RESISTANCE, AppleEffect.ABSORPTION);

		assertAppleOffers(helper, 19, Set.of());
		assertAppleOffers(helper, 20, regeneration);
		assertAppleOffers(helper, 38, regeneration);
		assertAppleOffers(helper, 39, fireResistance);
		assertAppleOffers(helper, 64, fireResistance);
		assertAppleOffers(helper, 65, absorption);
		assertAppleOffers(helper, 97, absorption);
		assertAppleOffers(helper, 98, EnumSet.allOf(AppleEffect.class));
		helper.succeed();
	}

	@GameTest
	public void aTableBelowFifteenBookshelvesOffersNoAppleEffects(GameTestHelper helper) {
		placeTable(helper, List.of(), shelves(fullLibraryBooks(helper)));

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		helper.assertTrue(table.progress().isFull(), "the Library is Full");
		helper.assertValueEqual(table.appleChecklist().offered(), Set.of(), "apple offers with 7 shelves");
		helper.succeed();
	}

	@GameTest
	public void eachEffectTakesAnEnchantmentSlotWithNoExtraSlotForGold(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		assertSlotLimit(helper, 29, List.of(AppleEffect.REGENERATION), 0);
		assertSlotLimit(helper, 30, List.of(AppleEffect.REGENERATION), 1);
		assertSlotLimit(helper, 30, List.of(AppleEffect.REGENERATION, AppleEffect.FIRE_RESISTANCE), 1);
		assertSlotLimit(helper, 60, List.of(AppleEffect.REGENERATION, AppleEffect.FIRE_RESISTANCE), 2);
		helper.succeed();
	}

	@GameTest
	public void theFirstSaveTakesABlockOfGoldAndEachEffectALapis(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer player = survivalPlayer(helper, 90);
		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.GOLDEN_APPLE), 2, ItemStack.EMPTY);
		assertRefuses(helper, table, player, new AppleChecklist.NoEffectSelected(), "nothing chosen");

		toggle(helper, table, player, AppleEffect.REGENERATION);
		toggle(helper, table, player, AppleEffect.FIRE_RESISTANCE);
		assertRefuses(helper, table, player, new Checklist.NeedsMaterial(new ItemStack(Items.GOLD_BLOCK).getHoverName()), "no block of gold");

		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.GOLD_BLOCK));
		assertSaves(helper, table, player, "the first save");

		ItemStack apple = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(apple.getItem(), Items.ENCHANTED_GOLDEN_APPLE, "the saved apple");
		helper.assertValueEqual(AppleEffect.on(apple), EnumSet.of(AppleEffect.REGENERATION, AppleEffect.FIRE_RESISTANCE), "its effects");
		helper.assertTrue(ItemStack.matches(EnchantingDeposit.refund(apple), new ItemStack(Items.GOLD_BLOCK)), "the deposit is a block of gold");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "both lapis are spent");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().isEmpty(), "the block of gold is spent");

		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 3));
		toggle(helper, table, player, AppleEffect.ABSORPTION);
		assertSaves(helper, table, player, "a second save without gold");

		apple = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(AppleEffect.on(apple).size(), 3, "effects after the second save");
		helper.assertTrue(ItemStack.matches(EnchantingDeposit.refund(apple), new ItemStack(Items.GOLD_BLOCK)), "the deposit is still one block");
		helper.assertTrue(table.getSlot(EnchantingTableMenu.LAPIS_SLOT).getItem().isEmpty(), "a lapis per effect is spent");
		helper.succeed();
	}

	@GameTest
	public void anEffectTheLibraryNoLongerOffersBlocksTheSave(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ItemStack resistanceApple = makeApple(helper, survivalPlayer(helper, 30), AppleEffect.RESISTANCE);

		placeLibraryWorth(helper, 20);

		ServerPlayer player = survivalPlayer(helper, 30);
		EnchantingTableMenu table = table(helper, player);

		fill(table, resistanceApple, 1, ItemStack.EMPTY);

		helper.assertValueEqual(table.appleChecklist().chosen(), Set.of(AppleEffect.RESISTANCE), "Resistance starts chosen");
		assertRefuses(helper, table, player, new AppleChecklist.NotOffered(AppleEffect.RESISTANCE), "Resistance at 20 levels");

		toggle(helper, table, player, AppleEffect.RESISTANCE);
		toggle(helper, table, player, AppleEffect.REGENERATION);
		assertSaves(helper, table, player, "Regeneration instead");

		ItemStack apple = table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem();

		helper.assertValueEqual(AppleEffect.on(apple), Set.of(AppleEffect.REGENERATION), "the apple's effects");
		helper.succeed();
	}

	@GameTest
	public void anEnchantedGoldenAppleGivesExactlyItsEffects(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer player = survivalPlayer(helper, 120);
		ItemStack everything = makeApple(helper, player, AppleEffect.values());

		everything.finishUsingItem(helper.getLevel(), player);

		assertEffect(helper, player, MobEffects.REGENERATION, 400, 1);
		assertEffect(helper, player, MobEffects.FIRE_RESISTANCE, 6000, 0);
		assertEffect(helper, player, MobEffects.ABSORPTION, 2400, 3);
		assertEffect(helper, player, MobEffects.RESISTANCE, 6000, 0);

		AdvancementHolder balancedDiet = helper.getLevel().getServer().getAdvancements().get(BALANCED_DIET);
		var criterion = player.getAdvancements().getOrStartProgress(balancedDiet).getCriterion("enchanted_golden_apple");

		helper.assertTrue(criterion != null && criterion.isDone(), "A Balanced Diet counts the enchanted golden apple");

		ServerPlayer eater = survivalPlayer(helper, 60);

		makeApple(helper, eater, AppleEffect.FIRE_RESISTANCE, AppleEffect.ABSORPTION).finishUsingItem(helper.getLevel(), eater);

		helper.assertValueEqual(eater.getActiveEffects().size(), 2, "effects from a Fire Resistance and Absorption apple");
		assertEffect(helper, eater, MobEffects.FIRE_RESISTANCE, 6000, 0);
		assertEffect(helper, eater, MobEffects.ABSORPTION, 2400, 3);
		helper.succeed();
	}

	@GameTest
	public void applesStackOnlyWhenTheirEffectsMatch(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ItemStack regeneration = makeApple(helper, survivalPlayer(helper, 30), AppleEffect.REGENERATION);
		ItemStack otherRegeneration = makeApple(helper, survivalPlayer(helper, 30), AppleEffect.REGENERATION);
		ItemStack fireResistance = makeApple(helper, survivalPlayer(helper, 30), AppleEffect.FIRE_RESISTANCE);

		helper.assertTrue(ItemStack.isSameItemSameComponents(regeneration, otherRegeneration), "two Regeneration apples stack");
		helper.assertFalse(ItemStack.isSameItemSameComponents(regeneration, fireResistance), "Regeneration and Fire Resistance apples stack");
		helper.succeed();
	}

	@GameTest
	public void theTableTakesOneAppleFromAStack(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer player = survivalPlayer(helper, 30);
		EnchantingTableMenu table = table(helper, player);

		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.GOLDEN_APPLE, 3));
		table.clicked(FIRST_INVENTORY_MENU_SLOT, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getCount(), 1, "apples in the table");
		helper.assertValueEqual(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT).getCount(), 2, "apples left in the inventory");

		table.getSlot(EnchantingTableMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI));
		table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).set(new ItemStack(Items.GOLD_BLOCK, 2));
		toggle(helper, table, player, AppleEffect.REGENERATION);
		assertSaves(helper, table, player, "one apple");

		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().getCount(), 1, "enchanted apples");
		helper.assertValueEqual(table.getSlot(EnchantingTableMenu.MATERIAL_SLOT).getItem().getCount(), 1, "blocks of gold left");
		helper.assertTrue(
			ItemStack.matches(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT), new ItemStack(Items.GOLDEN_APPLE, 2)),
			"the rest of the stack is unchanged"
		);
		helper.succeed();
	}

	@GameTest
	public void theGrindstoneTurnsOneAppleBackIntoAGoldenApple(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer player = survivalPlayer(helper, 30);
		ItemStack apple = makeApple(helper, player, AppleEffect.REGENERATION);
		GrindstoneMenu grindstone = grindstone(helper, player);
		int pointsBefore = ExperiencePoints.total(player);

		helper.assertFalse(grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).mayPlace(new ItemStack(Items.GOLDEN_APPLE)), "a golden apple is ground");
		helper.assertFalse(grindstone.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).mayPlace(apple), "the lower slot takes an apple");

		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, apple.copyWithCount(3));
		grindstone.clicked(FIRST_INVENTORY_MENU_SLOT, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertValueEqual(grindstone.getSlot(GrindstoneMenu.INPUT_SLOT).getItem().getCount(), 1, "apples in the grindstone");
		helper.assertTrue(grindstone.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "the lower slot is empty");

		grindstone.setCarried(ItemStack.EMPTY);
		grindstone.clicked(GrindstoneMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);

		ItemStack ground = grindstone.getCarried();

		helper.assertTrue(ItemStack.matches(ground, new ItemStack(Items.GOLDEN_APPLE)), "the ground apple is a plain golden apple: " + ground);
		helper.assertTrue(ItemStack.isSameItemSameComponents(ground, new ItemStack(Items.GOLDEN_APPLE)), "it stacks with golden apples");
		helper.assertValueEqual(player.getInventory().countItem(Items.GOLD_BLOCK), 1, "blocks of gold handed back");
		helper.assertValueEqual(player.getInventory().countItem(Items.LAPIS_LAZULI), 0, "lapis handed back");
		helper.assertValueEqual(ExperiencePoints.total(player), pointsBefore, "points after grinding");
		helper.assertTrue(
			ItemStack.matches(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT), apple.copyWithCount(2)),
			"the other apples keep their effects and deposits"
		);
		helper.succeed();
	}

	@GameTest
	public void anyEnchantedGoldenAppleCuresAZombieVillager(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer player = survivalPlayer(helper, 30);
		ItemStack fireResistance = makeApple(helper, player, AppleEffect.FIRE_RESISTANCE);

		helper.assertTrue(cures(helper, fireResistance), "a Fire Resistance apple cures");
		helper.assertFalse(cures(helper, new ItemStack(Items.GOLDEN_APPLE)), "a golden apple cures");
		helper.succeed();
	}

	@GameTest
	public void lootGivesGoldenApplesInsteadOfEnchantedOnes(GameTestHelper helper) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(APPLE_LOOT);
		LootParams params = new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
			.create(LootContextParamSets.CHEST);
		List<ItemStack> drops = table.getRandomItems(params, RandomSource.create(0L));

		helper.assertValueEqual(drops.size(), 1, "drop count");
		helper.assertTrue(ItemStack.matches(drops.getFirst(), new ItemStack(Items.GOLDEN_APPLE, 2)), "the drop: " + drops.getFirst());
		helper.succeed();
	}

	@GameTest
	public void makingAnAppleGrantsAnAppleADay(GameTestHelper helper) {
		placeLibraryWorth(helper, 130);

		ServerPlayer appleMaker = survivalPlayer(helper, 30);
		ServerPlayer swordMaker = survivalPlayer(helper, 30);
		Holder<Enchantment> sharpness = enchantment(helper, Enchantments.SHARPNESS);
		EnchantingTableMenu table = table(helper, swordMaker);

		makeApple(helper, appleMaker, AppleEffect.REGENERATION);
		fill(table, new ItemStack(Items.IRON_SWORD), 1, new ItemStack(Items.IRON_INGOT));
		boolean toggled = table.clickMenuButton(swordMaker, EnchantingTableMenu.toggleButton(sharpness, table.enchantments()));

		helper.assertTrue(toggled, "Sharpness toggles");
		assertSaves(helper, table, swordMaker, "a sword");

		var advancements = helper.getLevel().getServer().getAdvancements();

		helper.assertTrue(isDone(helper, appleMaker, APPLE_ADVANCEMENT), "making an apple grants An Apple a Day");
		helper.assertFalse(isDone(helper, swordMaker, APPLE_ADVANCEMENT), "saving a sword grants An Apple a Day");
		helper.assertValueEqual(advancements.get(APPLE_ADVANCEMENT).value().parent(), Optional.of(ENCHANTER), "An Apple a Day's parent");
		helper.assertValueEqual(advancements.get(ZOMBIE_DOCTOR).value().parent(), Optional.of(APPLE_ADVANCEMENT), "Zombie Doctor's parent");
		helper.succeed();
	}

	/** Places a table with 16 plain bookshelves and a Library of books worth exactly {@code levels} of Library Progress. */
	private static void placeLibraryWorth(GameTestHelper helper, int levels) {
		List<ItemStack> books = new ArrayList<>();
		int remaining = levels;

		for (Map.Entry<Holder<Enchantment>, Integer> entry : fullLibraryLevels(helper).entrySet()) {
			if (remaining == 0) break;

			int level = Math.min(entry.getValue(), remaining);

			books.add(book(entry.getKey(), level));
			remaining -= level;
		}

		List<List<ItemStack>> chiseledShelves = new ArrayList<>(shelves(books));
		int fullLibraryShelves = shelves(fullLibraryBooks(helper)).size();

		// Empty shelves replace any a larger Library filled earlier in the test.
		while (chiseledShelves.size() < fullLibraryShelves) chiseledShelves.add(List.of());

		placeTable(helper, UPPER_RING, chiseledShelves);
	}

	private static void assertAppleOffers(GameTestHelper helper, int levels, Set<AppleEffect> expected) {
		placeLibraryWorth(helper, levels);

		EnchantingTableMenu table = table(helper, survivalPlayer(helper, 0));

		helper.assertValueEqual(table.progress().levels(), levels, "Library Progress");
		helper.assertValueEqual(table.appleChecklist().offered(), expected, "apple offers at " + levels + " levels");
	}

	/** Saves an apple with {@code effects} at {@code level}, failing unless it saves exactly when they fit the {@code slots}. */
	private static void assertSlotLimit(GameTestHelper helper, int level, List<AppleEffect> effects, int slots) {
		ServerPlayer player = survivalPlayer(helper, level);
		EnchantingTableMenu table = table(helper, player);
		String label = effects.size() + " effects at level " + level;

		fill(table, new ItemStack(Items.GOLDEN_APPLE), effects.size(), new ItemStack(Items.GOLD_BLOCK));
		effects.forEach(effect -> toggle(helper, table, player, effect));

		if (effects.size() <= slots) {
			assertSaves(helper, table, player, label);
		} else {
			assertRefuses(helper, table, player, new Checklist.TooMany(effects.size(), slots), label);
		}
	}

	/** Saves a golden apple with {@code effects} at the table, returning the enchanted golden apple. */
	private static ItemStack makeApple(GameTestHelper helper, ServerPlayer player, AppleEffect... effects) {
		EnchantingTableMenu table = table(helper, player);

		fill(table, new ItemStack(Items.GOLDEN_APPLE), effects.length, new ItemStack(Items.GOLD_BLOCK));

		for (AppleEffect effect : effects) {
			toggle(helper, table, player, effect);
		}

		assertSaves(helper, table, player, List.of(effects) + " apple");

		return table.getSlot(EnchantingTableMenu.ITEM_SLOT).getItem().copy();
	}

	private static void toggle(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, AppleEffect effect) {
		helper.assertTrue(table.clickMenuButton(player, EnchantingTableMenu.toggleButton(effect)), effect + " toggles");
	}

	private static void assertSaves(GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, String label) {
		helper.assertTrue(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves");
	}

	/** Saves, failing unless the save is refused for {@code problem} and nothing in the table changes. */
	private static void assertRefuses(
		GameTestHelper helper, EnchantingTableMenu table, ServerPlayer player, Checklist.Problem problem, String label
	) {
		List<ItemStack> slotsBefore = tableSlots(table);
		Set<AppleEffect> chosenBefore = table.appleChecklist().chosen();

		helper.assertTrue(table.problems(player).contains(problem), label + ": problems " + table.problems(player) + " lack " + problem);
		helper.assertFalse(table.clickMenuButton(player, EnchantingTableMenu.SAVE_BUTTON), label + " saves");
		helper.assertTrue(ItemStack.listMatches(slotsBefore, tableSlots(table)), label + ": the table's slots are unchanged");
		helper.assertValueEqual(table.appleChecklist().chosen(), chosenBefore, label + ": the choices");
	}

	private static List<ItemStack> tableSlots(EnchantingTableMenu table) {
		return List.of(EnchantingTableMenu.ITEM_SLOT, EnchantingTableMenu.LAPIS_SLOT, EnchantingTableMenu.MATERIAL_SLOT).stream()
			.map(slot -> table.getSlot(slot).getItem().copy())
			.toList();
	}

	private static void assertEffect(GameTestHelper helper, ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
		MobEffectInstance instance = player.getEffect(effect);
		String name = effect.getRegisteredName();

		helper.assertTrue(instance != null, name + " is applied");
		helper.assertValueEqual(instance.getDuration(), duration, name + " duration");
		helper.assertValueEqual(instance.getAmplifier(), amplifier, name + " amplifier");
	}

	/** Whether a player feeding {@code apple} to a zombie villager under Weakness starts curing it. */
	private static boolean cures(GameTestHelper helper, ItemStack apple) {
		ZombieVillager zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE_VILLAGER, ZOMBIE_SPOT);
		ServerPlayer player = survivalPlayer(helper, 0);

		zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
		player.setItemInHand(InteractionHand.MAIN_HAND, apple.copy());
		player.interactOn(zombie, InteractionHand.MAIN_HAND, Vec3.ZERO);

		boolean converting = zombie.isConverting();

		helper.assertValueEqual(player.getMainHandItem().isEmpty(), converting, "the apple is used up exactly when it cures");
		zombie.discard();

		return converting;
	}
}

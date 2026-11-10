package mapleflavored.gametest;

import static mapleflavored.gametest.MockPlayers.survivalPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mapleflavored.gametest.mixin.ExperienceOrbAccessor;
import mapleflavored.hearts.Hearts;
import mapleflavored.items.MapleItems;
import mapleflavored.reinforcement.Reinforcement;
import mapleflavored.trades.BookTrade;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

/**
 * Villagers sell plain gear, Toolsmiths sell copper tools, and no villager sells an enchanted book, Pith, a Crystal
 * Heart or a raw diamond. Every Novice Librarian buys any enchanted book and pays per find, levels 2 to 4 have no book
 * trade, and every Master Armorer sells the Reinforcement template.
 */
public class TradeGameTests {
	// Each villager rolls its level's trade set afresh, so enough of them see every trade a set can pick.
	private static final int VILLAGERS_PER_LEVEL = 16;
	private static final int ROLLS_PER_TRADE = 20;
	private static final BlockPos VILLAGER = new BlockPos(1, 1, 1);
	// The last of the 36 main inventory slots, which the merchant menu lists last.
	private static final int LAST_INVENTORY_SLOT = 35;
	private static final Set<Item> NEVER_SOLD = Set.of(
		Items.ENCHANTED_BOOK, Items.DIAMOND, MapleItems.PITH, Hearts.CRYSTAL_HEART,
		Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_PICKAXE, Items.STONE_HOE, Items.STONE_SWORD
	);
	// Vanilla's enchanted gear trades, now plain at their base price.
	private static final List<PlainGear> PLAIN_GEAR = List.of(
		new PlainGear("armorer/4/emerald_enchanted_diamond_boots", Items.DIAMOND_BOOTS, 8),
		new PlainGear("armorer/4/emerald_enchanted_diamond_leggings", Items.DIAMOND_LEGGINGS, 14),
		new PlainGear("armorer/5/emerald_enchanted_diamond_chestplate", Items.DIAMOND_CHESTPLATE, 16),
		new PlainGear("armorer/5/emerald_enchanted_diamond_helmet", Items.DIAMOND_HELMET, 8),
		new PlainGear("fisherman/3/emerald_enchanted_fishing_rod", Items.FISHING_ROD, 3),
		new PlainGear("fletcher/4/emerald_enchanted_bow", Items.BOW, 2),
		new PlainGear("fletcher/5/emerald_enchanted_crossbow", Items.CROSSBOW, 3),
		new PlainGear("toolsmith/3/emerald_enchanted_iron_axe", Items.IRON_AXE, 1),
		new PlainGear("toolsmith/3/emerald_enchanted_iron_pickaxe", Items.IRON_PICKAXE, 3),
		new PlainGear("toolsmith/3/emerald_enchanted_iron_shovel", Items.IRON_SHOVEL, 2),
		new PlainGear("toolsmith/4/emerald_enchanted_diamond_axe", Items.DIAMOND_AXE, 12),
		new PlainGear("toolsmith/4/emerald_enchanted_diamond_shovel", Items.DIAMOND_SHOVEL, 5),
		new PlainGear("toolsmith/5/emerald_enchanted_diamond_pickaxe", Items.DIAMOND_PICKAXE, 13),
		new PlainGear("wandering_trader/emerald_enchanted_iron_pickaxe", Items.IRON_PICKAXE, 1),
		new PlainGear("weaponsmith/1/emerald_enchanted_iron_sword", Items.IRON_SWORD, 2),
		new PlainGear("weaponsmith/4/emerald_enchanted_diamond_axe", Items.DIAMOND_AXE, 12),
		new PlainGear("weaponsmith/5/emerald_enchanted_diamond_sword", Items.DIAMOND_SWORD, 8)
	);
	private static final Set<Item> NOVICE_TOOLSMITH_SALES = Set.of(
		Items.EMERALD, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_PICKAXE, Items.COPPER_HOE
	);

	@GameTest(maxTicks = 400)
	public void noTradeSellsEnchantmentsBooksDiamondsOrHeartIngredients(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Villager villager = new Villager(EntityTypes.VILLAGER, level);
		LootContext context = tradeContext(level, villager);

		for (var tradeSet : level.registryAccess().lookupOrThrow(Registries.TRADE_SET).listElements().toList()) {
			for (Holder<VillagerTrade> trade : tradeSet.value().trades()) {
				for (int roll = 0; roll < ROLLS_PER_TRADE; roll++) {
					MerchantOffer offer = trade.value().getOffer(context);

					if (offer != null) assertAllowedSale(helper, offer, tradeSet.key().identifier() + " " + trade.getRegisteredName());
				}
			}
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void everyProfessionAndLevelOffersOnlyAllowedSales(GameTestHelper helper) {
		for (var profession : helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).listElements().toList()) {
			for (int villagerLevel = 1; villagerLevel <= 5; villagerLevel++) {
				for (int villager = 0; villager < VILLAGERS_PER_LEVEL; villager++) {
					for (MerchantOffer offer : offers(helper, profession.key(), villagerLevel)) {
						assertAllowedSale(helper, offer, profession.key().identifier() + " level " + villagerLevel);
					}
				}
			}
		}

		helper.succeed();
	}

	@GameTest
	public void villagersSellPlainGearWhereVanillaSoldEnchantedGear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootContext context = tradeContext(level, new Villager(EntityTypes.VILLAGER, level));

		for (PlainGear gear : PLAIN_GEAR) {
			VillagerTrade trade = level.registryAccess()
				.lookupOrThrow(Registries.VILLAGER_TRADE)
				.getOrThrow(ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace(gear.trade())))
				.value();
			MerchantOffer offer = trade.getOffer(context);

			helper.assertTrue(offer != null, gear.trade() + " is offered");
			helper.assertValueEqual(offer.getResult().getItem(), gear.item(), gear.trade() + " sale");
			helper.assertTrue(offer.getResult().getEnchantments().isEmpty(), gear.trade() + " sells plain gear");
			helper.assertValueEqual(offer.getCostA().getCount(), gear.price(), gear.trade() + " price");
		}

		helper.succeed();
	}

	@GameTest
	public void noviceToolsmithsSellCopperToolsInsteadOfStone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootContext context = tradeContext(level, new Villager(EntityTypes.VILLAGER, level));
		ResourceKey<TradeSet> noviceTrades = level.registryAccess()
			.lookupOrThrow(Registries.VILLAGER_PROFESSION)
			.getOrThrow(VillagerProfession.TOOLSMITH)
			.value()
			.getTrades(1);
		Set<Item> sales = new HashSet<>();

		for (Holder<VillagerTrade> trade : level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOrThrow(noviceTrades).value().trades()) {
			sales.add(trade.value().getOffer(context).getResult().getItem());
		}

		helper.assertValueEqual(sales, NOVICE_TOOLSMITH_SALES, "Novice Toolsmith sales");
		helper.succeed();
	}

	@GameTest
	public void everyNoviceLibrarianOffersAllThreeTradesIncludingTheBookTrade(GameTestHelper helper) {
		for (int villager = 0; villager < VILLAGERS_PER_LEVEL; villager++) {
			MerchantOffers offers = offers(helper, VillagerProfession.LIBRARIAN, 1);
			List<MerchantOffer> bookTrades = offers.stream().filter(BookTrade::isBookTrade).toList();

			helper.assertValueEqual(offers.size(), 3, "Novice Librarian trade count");
			helper.assertValueEqual(bookTrades.size(), 1, "Novice Librarian book trades");

			MerchantOffer bookTrade = bookTrades.getFirst();

			helper.assertValueEqual(bookTrade.getCostA().getCount(), 1, "books taken");
			helper.assertValueEqual(bookTrade.getResult().getCount(), 4, "listed emeralds");
			helper.assertValueEqual(bookTrade.getXp(), 10, "listed villager XP");
			helper.assertValueEqual(bookTrade.getMaxUses(), 12, "uses");
			helper.assertValueEqual(bookTrade.getPriceMultiplier(), 0.0F, "price multiplier");
		}

		helper.succeed();
	}

	@GameTest
	public void librarianLevelsTwoToFourHaveNoBookTrade(GameTestHelper helper) {
		for (int villagerLevel = 2; villagerLevel <= 4; villagerLevel++) {
			for (int villager = 0; villager < VILLAGERS_PER_LEVEL; villager++) {
				for (MerchantOffer offer : offers(helper, VillagerProfession.LIBRARIAN, villagerLevel)) {
					boolean involvesBook = offer.getCostA().is(Items.ENCHANTED_BOOK)
						|| offer.getCostB().is(Items.ENCHANTED_BOOK)
						|| offer.getResult().is(Items.ENCHANTED_BOOK);

					helper.assertFalse(involvesBook, "Librarian level " + villagerLevel + " trades " + offer.getResult());
				}
			}
		}

		helper.succeed();
	}

	@GameTest
	public void everyMasterArmorerSellsTheReinforcementTemplate(GameTestHelper helper) {
		for (int villager = 0; villager < VILLAGERS_PER_LEVEL; villager++) {
			Optional<MerchantOffer> template = offers(helper, VillagerProfession.ARMORER, 5).stream()
				.filter(offer -> offer.getResult().is(Reinforcement.TEMPLATE))
				.findFirst();

			helper.assertTrue(template.isPresent(), "Master Armorer sells the Reinforcement template");
			helper.assertTrue(template.get().getCostA().is(Items.EMERALD), "template is bought with emeralds");
			helper.assertValueEqual(template.get().getCostA().getCount(), 32, "template price");
		}

		helper.succeed();
	}

	@GameTest
	public void theResultSlotPaysPerFindForAnyEnchantedBook(GameTestHelper helper) {
		Trading trading = startTrading(helper);
		List<Payout> payouts = List.of(
			new Payout(book(helper, Enchantments.SHARPNESS, 1), 4),
			new Payout(book(helper, Enchantments.MENDING, 1), 4),
			new Payout(book(helper, Enchantments.PROTECTION, 2), 8),
			new Payout(book(helper, Enchantments.EFFICIENCY, 4), 32),
			new Payout(book(helper, Enchantments.SHARPNESS, 5), 64),
			new Payout(new ItemStack(Items.ENCHANTED_BOOK), 0)
		);

		for (Payout payout : payouts) {
			trading.menu().getSlot(0).set(payout.book());

			helper.assertValueEqual(trading.menu().getSlot(2).getItem().getCount(), payout.emeralds(), "emeralds for " + payout.book());
			helper.assertValueEqual(trading.menu().getFutureTraderXp(), payout.emeralds() / 4 * 10, "previewed XP for " + payout.book());
		}

		helper.succeed();
	}

	@GameTest
	public void sellingABookPaysEmeraldsVillagerXpAndPlayerXpPerFind(GameTestHelper helper) {
		Trading trading = startTrading(helper);
		ServerPlayer player = trading.player();
		Villager villager = trading.villager();

		trading.menu().getSlot(0).set(book(helper, Enchantments.EFFICIENCY, 3));
		trading.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, player);

		int emeralds = player.getInventory().countItem(Items.EMERALD);
		// An orb may stand for several of its value. The 200 points per find come as orbs of 149, 37, 7 and 7, and awarding
		// the second 7 merges it into the first one time in 40.
		int orbXp = helper.getLevel()
			.getEntitiesOfClass(ExperienceOrb.class, new AABB(villager.blockPosition()).inflate(4))
			.stream()
			.mapToInt(orb -> orb.getValue() * ((ExperienceOrbAccessor) orb).maple$count())
			.sum();
		// Vanilla's own trade orb is 3 to 6 points, plus 5 as this trade levels the Novice up.
		int vanillaOrbXp = orbXp - 4 * BookTrade.PLAYER_XP_PER_FIND;

		helper.assertValueEqual(emeralds, 16, "emeralds paid for a level III book");
		helper.assertTrue(trading.menu().getSlot(0).getItem().isEmpty(), "the book is taken");
		helper.assertValueEqual(villager.getVillagerXp(), 40, "villager XP for a level III book");
		helper.assertTrue(vanillaOrbXp >= 8 && vanillaOrbXp <= 11, "player XP beyond 200 points was " + vanillaOrbXp);

		trading.menu().getSlot(0).set(new ItemStack(Items.PAPER, 24));
		trading.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertValueEqual(villager.getVillagerXp(), 42, "villager XP after a paper trade, which pays once");
		helper.succeed();
	}

	@GameTest
	public void shiftClickingAPayoutThatDoesNotFitTakesNothing(GameTestHelper helper) {
		Trading trading = startTrading(helper);
		ServerPlayer player = trading.player();
		ItemStack book = book(helper, Enchantments.SHARPNESS, 5);

		fillInventoryExceptLastSlot(player);
		player.getInventory().setItem(LAST_INVENTORY_SLOT, new ItemStack(Items.EMERALD, 63));
		trading.menu().getSlot(0).set(book);
		trading.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, player);

		MerchantOffer bookTrade = trading.villager().getOffers().stream().filter(BookTrade::isBookTrade).findFirst().orElseThrow();

		helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 63, "emeralds after a refused payout");
		helper.assertTrue(ItemStack.matches(trading.menu().getSlot(0).getItem(), book), "the book stays in the payment slot");
		helper.assertValueEqual(trading.menu().getSlot(2).getItem().getCount(), 64, "the payout stays in the result slot");
		helper.assertValueEqual(bookTrade.getUses(), 0, "uses after a refused payout");
		helper.assertValueEqual(trading.villager().getVillagerXp(), 0, "villager XP after a refused payout");

		player.getInventory().setItem(LAST_INVENTORY_SLOT, ItemStack.EMPTY);
		trading.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, player);

		helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 64, "emeralds when the payout exactly fits");
		helper.assertValueEqual(trading.villager().getVillagerXp(), 160, "villager XP for a level V book");
		helper.succeed();
	}

	private static void fillInventoryExceptLastSlot(ServerPlayer player) {
		for (int slot = 0; slot < LAST_INVENTORY_SLOT; slot++) {
			player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
		}
	}

	private static void assertAllowedSale(GameTestHelper helper, MerchantOffer offer, String source) {
		ItemStack sale = offer.getResult();
		ItemEnchantments stored = sale.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

		helper.assertFalse(NEVER_SOLD.contains(sale.getItem()), source + " sells " + sale);
		helper.assertTrue(sale.getEnchantments().isEmpty() && stored.isEmpty(), source + " sells enchanted " + sale);
	}

	private static MerchantOffers offers(GameTestHelper helper, ResourceKey<VillagerProfession> profession, int villagerLevel) {
		ServerLevel level = helper.getLevel();
		Villager villager = new Villager(EntityTypes.VILLAGER, level);

		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), profession).withLevel(villagerLevel));

		return villager.getOffers();
	}

	private static LootContext tradeContext(ServerLevel level, Villager villager) {
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, villager.position())
			.withParameter(LootContextParams.THIS_ENTITY, villager)
			.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
			.create(LootContextParamSets.VILLAGER_TRADE);

		return new LootContext.Builder(params).create(Optional.empty());
	}

	private static Trading startTrading(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		Villager villager = helper.spawn(EntityTypes.VILLAGER, VILLAGER);
		ServerLevel level = helper.getLevel();

		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN).withLevel(1));
		villager.setVillagerXp(0);
		villager.getOffers();
		villager.setTradingPlayer(player);

		return new Trading(player, villager, new MerchantMenu(1, player.getInventory(), villager));
	}

	private static ItemStack book(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
		Holder<Enchantment> enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);

		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
	}

	private record PlainGear(String trade, Item item, int price) {
	}

	private record Payout(ItemStack book, int emeralds) {
	}

	private record Trading(ServerPlayer player, Villager villager, MerchantMenu menu) {
	}
}

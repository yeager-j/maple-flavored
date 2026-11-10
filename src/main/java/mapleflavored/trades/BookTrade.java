package mapleflavored.trades;

import mapleflavored.MapleFlavored;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * The Novice Librarian's trade that buys any enchanted book and pays per find the book holds. A level n enchantment is
 * 2^(n−1) finds, so combining books never changes what they sell for.
 *
 * <p>The trade's data names it as the book trade by putting {@link #PAYS_PER_FIND} on what it gives, and lists the
 * level I price. These rules scale its emeralds, villager XP and player XP by the finds of the book actually paid, up
 * to the most finds whose payout fits one stack.
 */
public final class BookTrade {
	/** Player XP points paid per find, on top of vanilla's trade orb. Matches the anvil's 50 points per book level. */
	public static final int PLAYER_XP_PER_FIND = 50;

	/**
	 * Marks a trade's result as paid once per find of the enchanted book the trade takes, written in the trade's data
	 * as {@code "maple_flavored:pays_per_find": {}}. The items paid out don't carry it.
	 */
	public static final DataComponentType<Unit> PAYS_PER_FIND = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MapleFlavored.id("pays_per_find"),
		DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build()
	);

	// Commands can store levels up to 255. Past this level one enchantment's finds already exceed any stack.
	private static final int HIGHEST_COUNTED_LEVEL = 8;

	private BookTrade() {
	}

	/** Registers the book trade's marker. Call once from mod init. */
	public static void register() {
	}

	/** Whether the offer is the book trade. */
	public static boolean isBookTrade(MerchantOffer offer) {
		return offer.getResult().has(PAYS_PER_FIND);
	}

	/**
	 * Marks the book trade among offers saved before {@link #PAYS_PER_FIND} existed, when the trade was known by taking
	 * an enchanted book and paying emeralds. Offers made since carry the marker from the trade's data.
	 */
	public static void markSavedOffers(MerchantOffers offers) {
		for (MerchantOffer offer : offers) {
			boolean savedBookTrade = offer.getBaseCostA().is(Items.ENCHANTED_BOOK) && offer.getResult().is(Items.EMERALD);

			if (savedBookTrade) offer.getResult().set(PAYS_PER_FIND, Unit.INSTANCE);
		}
	}

	/**
	 * How many times over the paid items scale the offer's result and XP: the finds of the book paid for the book trade,
	 * 0 for a book holding no enchantments, and 1 for every other trade. The payment may sit in either slot.
	 */
	public static int multiplier(MerchantOffer offer, ItemStack payment, ItemStack otherPayment) {
		if (!isBookTrade(offer)) return 1;

		ItemStack book = offer.getItemCostA().test(payment) ? payment : otherPayment;

		return Math.min(finds(book), mostFinds(offer));
	}

	/**
	 * What the offer pays out for a trade worth {@code multiplier} times over, or an empty stack for 0 times.
	 *
	 * @param listed the result the offer lists, which is not changed
	 * @param multiplier from {@link #multiplier}
	 */
	public static ItemStack payout(ItemStack listed, int multiplier) {
		if (multiplier == 0) return ItemStack.EMPTY;

		ItemStack payout = listed.copyWithCount(listed.getCount() * multiplier);

		payout.remove(PAYS_PER_FIND);

		return payout;
	}

	/**
	 * Rewards the finds a book trade paid beyond the first: the villager's XP for each, and an orb of player XP for all
	 * of them. Does nothing for other trades. Call before the merchant is notified of the trade, so the villager counts
	 * the XP toward its level up.
	 *
	 * @param multiplier from {@link #multiplier}, read before the trade took its payment
	 */
	public static void rewardFinds(Merchant merchant, MerchantOffer offer, int multiplier) {
		if (!isBookTrade(offer) || !(merchant instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) return;

		villager.setVillagerXp(villager.getVillagerXp() + offer.getXp() * (multiplier - 1));
		ExperienceOrb.award(level, villager.position().add(0, 0.5, 0), PLAYER_XP_PER_FIND * multiplier);
	}

	// The most finds whose payout still fits the one stack a trade's result slot holds.
	private static int mostFinds(MerchantOffer offer) {
		ItemStack listed = offer.getResult();

		return listed.getMaxStackSize() / listed.getCount();
	}

	private static int finds(ItemStack book) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		int finds = 0;

		for (var entry : stored.entrySet()) {
			int level = Math.min(entry.getIntValue(), HIGHEST_COUNTED_LEVEL);

			finds += 1 << (level - 1);
		}

		return finds;
	}
}

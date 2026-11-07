package mapleflavored.trades;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * The Novice Librarian's trade that buys any enchanted book and pays per find the book holds. A level n enchantment is
 * 2^(n−1) finds, so combining books never changes what they sell for. The trade's data lists the level I price; these
 * rules scale its emeralds, villager XP and player XP by the finds of the book actually paid.
 */
public final class BookTrade {
	/** Player XP points paid per find, on top of vanilla's trade orb. Matches the anvil's 50 points per book level. */
	public static final int PLAYER_XP_PER_FIND = 50;

	// A level V book is 16 finds, whose 64 emeralds fill a stack, the most a trade's result can hold.
	private static final int MOST_FINDS = 16;
	// Commands can store levels up to 255, which would overflow the shift.
	private static final int HIGHEST_COUNTED_LEVEL = 5;

	private BookTrade() {
	}

	/** Whether the offer is the book trade: it takes an enchanted book and pays emeralds. */
	public static boolean isBookTrade(MerchantOffer offer) {
		return offer.getBaseCostA().is(Items.ENCHANTED_BOOK) && offer.getResult().is(Items.EMERALD);
	}

	/**
	 * How many times over the paid items scale the offer's result and XP: the finds of the book paid for the book trade,
	 * 0 for a book holding no enchantments, and 1 for every other trade. The payment may sit in either slot.
	 */
	public static int multiplier(MerchantOffer offer, ItemStack payment, ItemStack otherPayment) {
		if (!isBookTrade(offer)) return 1;

		ItemStack book = offer.getItemCostA().test(payment) ? payment : otherPayment;

		return finds(book);
	}

	/** The finds a book holds, summed over its stored enchantments and capped at a level V book's 16. */
	public static int finds(ItemStack book) {
		ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		int finds = 0;

		for (var entry : stored.entrySet()) {
			int level = Math.min(entry.getIntValue(), HIGHEST_COUNTED_LEVEL);

			finds += 1 << (level - 1);
		}

		return Math.min(finds, MOST_FINDS);
	}
}

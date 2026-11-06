package mapleflavored.alloys;

import java.util.List;
import java.util.function.BooleanSupplier;
import mapleflavored.MapleFlavored;
import mapleflavored.mixin.LootItemAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

/**
 * Orichalcum tools' drop bonus: on drops that Fortune or Looting affect, an Orichalcum tool or weapon adds half of the drop again,
 * on top of what the enchantment adds, at any enchantment level. A count gains half the count it had before Fortune or
 * Looting; a Looting chance gains half its unenchanted chance. Drops in the {@code maple_flavored:no_orichalcum_bonus}
 * item tag (Ancient Embers, Pith and enchanted books) never gain it.
 *
 * <p>Fortune's bonus follows the tool that broke the block, and Looting's the killer's main hand, as vanilla's do.
 *
 * <p>A chance condition can't see the drop it guards, so it skips the bonus only where its place shows the drop is
 * excluded: on a pool whose every entry is an excluded item, or on an excluded item's own entry. An excluded drop's
 * Looting chance must sit in one of those places; on a pool shared with other items it would get the bonus.
 */
public final class OrichalcumDrops {
	private static final TagKey<Item> NO_BONUS = TagKey.create(Registries.ITEM, MapleFlavored.id("no_orichalcum_bonus"));
	private static final float BONUS = 0.5F;

	// How many loot pools or entries that give only excluded items are being rolled on this thread. A chance condition
	// can't see the item it guards, so the pool or entry holding it switches the bonus off around it.
	private static final ThreadLocal<int[]> EXCLUDED_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	private OrichalcumDrops() {
	}

	/**
	 * Adds the bonus to a drop a Fortune or Looting count function just changed, if the item it used is an Orichalcum tool
	 * or weapon.
	 *
	 * @param drop the function's result, grown in place
	 * @param countBefore the drop's count before the function
	 * @param heldItem the item the function read the enchantment from
	 * @return whether the bonus applied, so the function's own limit should cap the result again
	 */
	public static boolean addCountBonus(
		ItemStack drop, int countBefore, Holder<Enchantment> enchantment, @Nullable ItemInstance heldItem, RandomSource random
	) {
		if (drop.is(NO_BONUS) || !appliesTo(enchantment, heldItem)) return false;

		drop.grow(Mth.floor(countBefore * BONUS + random.nextFloat()));

		return true;
	}

	/**
	 * The chance a Looting chance condition passes at: {@code chance}, plus half of {@code unenchantedChance} if the item it
	 * used is an Orichalcum tool or weapon and the drop it guards isn't excluded.
	 */
	public static float withChanceBonus(float chance, float unenchantedChance, Holder<Enchantment> enchantment, @Nullable ItemInstance heldItem) {
		if (EXCLUDED_DEPTH.get()[0] > 0 || !appliesTo(enchantment, heldItem)) return chance;

		return chance + unenchantedChance * BONUS;
	}

	/** The killer's main hand item, which Looting reads, or null without a living killer. */
	public static @Nullable ItemInstance killerWeapon(LootContext context) {
		return context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof LivingEntity killer ? killer.getMainHandItem() : null;
	}

	/** Whether every entry can only give excluded items, so the pool's conditions get no bonus. */
	public static boolean givesOnlyExcludedItems(List<LootPoolEntryContainer> entries) {
		return !entries.isEmpty() && entries.stream().allMatch(OrichalcumDrops::givesOnlyExcludedItems);
	}

	/** Whether the entry can only give excluded items, or nothing, so its conditions get no bonus. */
	public static boolean givesOnlyExcludedItems(LootPoolEntryContainer entry) {
		if (entry instanceof EmptyLootItem) return true;

		return entry instanceof LootItem item && ((LootItemAccessor) item).getItem().is(NO_BONUS);
	}

	/** Rolls a loot pool with no chance bonus. */
	public static void rollWithoutChanceBonus(Runnable roll) {
		testWithoutChanceBonus(() -> {
			roll.run();

			return true;
		});
	}

	/** Runs a loot test, such as an entry's conditions, with no chance bonus. */
	public static boolean testWithoutChanceBonus(BooleanSupplier test) {
		int[] depth = EXCLUDED_DEPTH.get();

		depth[0]++;

		try {
			return test.getAsBoolean();
		} finally {
			depth[0]--;
		}
	}

	private static boolean appliesTo(Holder<Enchantment> enchantment, @Nullable ItemInstance heldItem) {
		boolean fortuneOrLooting = enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.LOOTING);

		if (!fortuneOrLooting || heldItem == null) return false;

		return AlloyGear.alloyOf(heldItem).orElse(null) == Alloy.ORICHALCUM && AlloyGear.kindOf(heldItem).map(GearKind::isHeld).orElse(false);
	}
}

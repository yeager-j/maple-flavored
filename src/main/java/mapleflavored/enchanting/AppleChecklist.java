package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The checklist for a golden apple: which {@link AppleEffect}s it lists and which are chosen. Each effect takes an
 * Enchantment Slot and a lapis, and the first save of a plain golden apple takes a block of gold as its deposit.
 *
 * @param item the golden apple, plain or enchanted, in the table
 * @param offered the effects the table's Library offers, as {@link Library#appleOffers()} gives them
 * @param chosen the chosen effects
 */
public record AppleChecklist(ItemStack item, Set<AppleEffect> offered, Set<AppleEffect> chosen) implements Checklist {
	/** No effect is chosen. The grindstone, not the table, turns an apple plain. */
	public record NoEffectSelected() implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "no_effect_selected");
		}
	}

	/** A chosen effect isn't offered by the table's Library. */
	public record NotOffered(AppleEffect effect) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "not_offered", effect.describe());
		}
	}

	/** Whether the item is a golden apple, plain or enchanted. */
	public static boolean accepts(ItemStack item) {
		return item.is(Items.GOLDEN_APPLE) || item.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	/**
	 * The effects listed, in {@link AppleEffect} order: those offered, those the apple already gives, and anything
	 * chosen, so a choice the Library no longer offers can still be unticked.
	 */
	public List<AppleEffect> listed() {
		if (!accepts(item)) return List.of();

		Set<AppleEffect> onApple = AppleEffect.on(item);

		return Arrays.stream(AppleEffect.values())
			.filter(effect -> offered.contains(effect) || onApple.contains(effect) || chosen.contains(effect))
			.toList();
	}

	@Override
	public int chosenCount() {
		return chosen.size();
	}

	@Override
	public List<Row> rows(List<Problem> problems) {
		return listed().stream()
			.map(effect -> new Row(
				effect.describe(),
				chosen.contains(effect),
				EnchantingTableMenu.toggleButton(effect),
				problems.stream().filter(problem -> problem.equals(new NotOffered(effect))).toList()
			))
			.toList();
	}

	/** The deposit, a block of gold, is due on the first save of a plain golden apple. */
	@Override
	public Cost cost(boolean infiniteMaterials) {
		if (infiniteMaterials) return new Cost(0, false);

		return new Cost(chosenCount(), item.is(Items.GOLDEN_APPLE));
	}

	@Override
	public List<Problem> problems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials) {
		List<Problem> problems = new ArrayList<>();

		if (chosen.isEmpty()) problems.add(new NoEffectSelected());

		Arrays.stream(AppleEffect.values())
			.filter(effect -> chosen.contains(effect) && !offered.contains(effect))
			.forEach(effect -> problems.add(new NotOffered(effect)));
		problems.addAll(slotAndPaymentProblems(playerLevel, lapis, material, infiniteMaterials));

		return problems;
	}

	@Override
	public ItemStack saved() {
		return AppleEffect.enchant(item, chosen);
	}
}

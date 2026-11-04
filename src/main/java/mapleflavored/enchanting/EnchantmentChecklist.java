package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The checklist for gear: which enchantments it lists, which are chosen and at what level, and why a save would be
 * refused.
 *
 * @param item the item in the table
 * @param offers the Library's offers, as {@link Library#offers()} gives them
 * @param states each enchantment's state; enchantments left out are {@link State#UNSELECTED}
 * @param enchantments the enchantment registry, which orders the rows
 */
public record EnchantmentChecklist(
	ItemStack item,
	Map<Holder<Enchantment>, Integer> offers,
	Map<Holder<Enchantment>, State> states,
	Registry<Enchantment> enchantments
) implements Checklist {
	/** Whether an enchantment on the checklist is chosen, and so where its level comes from. */
	public enum State {
		/** Not chosen. */
		UNSELECTED,
		/** Chosen by the player, at the level the Library offers. */
		SELECTED,
		/**
		 * Already on the item when it went in. Saves at the Library's level when that is at least the item's, and keeps
		 * the item's level otherwise, which blocks the save until the player unticks it.
		 */
		CARRIED;

		/** The state after the player clicks the row: chosen rows untick, and an unticked row picks the Library's level. */
		public State toggled() {
			return this == UNSELECTED ? SELECTED : UNSELECTED;
		}
	}

	/** A reason the table refuses to save that is about one enchantment's row. */
	public interface EnchantmentProblem extends Problem {
		/** Whether the reason is about this enchantment's row. */
		boolean concerns(Holder<Enchantment> enchantment);
	}

	/** Nothing is chosen. The grindstone, not the table, strips an item. */
	public record NothingSelected() implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "nothing_selected");
		}
	}

	/** A chosen enchantment can't go on the item. */
	public record NotForItem(Holder<Enchantment> enchantment) implements EnchantmentProblem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "not_for_item", enchantment.value().description());
		}

		@Override
		public boolean concerns(Holder<Enchantment> other) {
			return enchantment.equals(other);
		}
	}

	/** The Library doesn't hold a chosen enchantment at its level: {@code offered} is its level there, or 0 for none. */
	public record NotInLibrary(Holder<Enchantment> enchantment, int level, int offered) implements EnchantmentProblem {
		@Override
		public Component describe() {
			if (offered == 0) return Component.translatable(KEY_PREFIX + "not_in_library", Enchantment.getFullname(enchantment, level));

			return Component.translatable(KEY_PREFIX + "lower_in_library", Enchantment.getFullname(enchantment, offered));
		}

		@Override
		public boolean concerns(Holder<Enchantment> other) {
			return enchantment.equals(other);
		}
	}

	/** Two chosen enchantments can't be on one item together. */
	public record Incompatible(Holder<Enchantment> first, Holder<Enchantment> second) implements EnchantmentProblem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "incompatible", first.value().description(), second.value().description());
		}

		@Override
		public boolean concerns(Holder<Enchantment> other) {
			return first.equals(other) || second.equals(other);
		}
	}

	/**
	 * Whether the table takes the item as gear: it has a material unit to pay its deposit with, and some enchantment
	 * can go on it. Books and golden apples have no enchantments that can go on them, so they aren't gear.
	 */
	public static boolean accepts(ItemStack item, Registry<Enchantment> enchantments) {
		if (MaterialUnits.materialOf(item).isEmpty()) return false;

		return enchantments.stream().anyMatch(enchantment -> enchantment.canEnchant(item));
	}

	/** The states for an item just put in the table: everything on it is {@link State#CARRIED}. */
	public static Map<Holder<Enchantment>, State> preselected(ItemStack item) {
		Map<Holder<Enchantment>, State> states = new HashMap<>();

		item.getEnchantments().keySet().forEach(enchantment -> states.put(enchantment, State.CARRIED));

		return states;
	}

	/**
	 * The enchantments listed, in tooltip order: the Library's offers the item can take, the item's own enchantments,
	 * and anything chosen, so a choice whose book was taken away can still be unticked.
	 */
	public List<Holder<Enchantment>> listed() {
		List<Holder<Enchantment>> listed = new ArrayList<>();

		for (Holder<Enchantment> enchantment : enchantments.getTagOrEmpty(EnchantmentTags.TOOLTIP_ORDER)) {
			if (isListed(enchantment)) listed.add(enchantment);
		}

		enchantments.listElements()
			.filter(this::isListed)
			.filter(enchantment -> !listed.contains(enchantment))
			.forEach(listed::add);

		return listed;
	}

	/** Whether the enchantment is listed, so the player can toggle it. */
	public boolean isListed(Holder<Enchantment> enchantment) {
		if (item.isEmpty()) return false;

		boolean offeredForItem = offer(enchantment) > 0 && enchantment.value().canEnchant(item);

		return offeredForItem || itemLevel(enchantment) > 0 || state(enchantment) != State.UNSELECTED;
	}

	/** The enchantment's state. */
	public State state(Holder<Enchantment> enchantment) {
		return states.getOrDefault(enchantment, State.UNSELECTED);
	}

	/** The level the enchantment saves at, or 0 if it isn't chosen or a chosen one isn't in the Library. */
	public int level(Holder<Enchantment> enchantment) {
		return switch (state(enchantment)) {
			case UNSELECTED -> 0;
			case SELECTED -> offer(enchantment);
			case CARRIED -> Math.max(itemLevel(enchantment), offer(enchantment));
		};
	}

	/**
	 * The level to show on the enchantment's row: what it saves at, or for an unticked row what ticking it would pick.
	 * Where the Library doesn't hold the enchantment, the item's level, or 1 for neither.
	 */
	public int shownLevel(Holder<Enchantment> enchantment) {
		int level = state(enchantment) == State.UNSELECTED ? offer(enchantment) : level(enchantment);

		if (level > 0) return level;

		return Math.max(itemLevel(enchantment), 1);
	}

	/** The chosen enchantments, in row order. */
	public List<Holder<Enchantment>> chosen() {
		return listed().stream().filter(enchantment -> state(enchantment) != State.UNSELECTED).toList();
	}

	/** The item's enchantments once saved: exactly the chosen ones, at their levels. */
	public ItemEnchantments selection() {
		ItemEnchantments.Mutable selection = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		chosen().forEach(enchantment -> selection.set(enchantment, level(enchantment)));

		return selection.toImmutable();
	}

	@Override
	public int chosenCount() {
		return chosen().size();
	}

	@Override
	public List<Row> rows(List<Problem> problems) {
		return listed().stream()
			.map(enchantment -> new Row(
				Enchantment.getFullname(enchantment, shownLevel(enchantment)),
				state(enchantment) != State.UNSELECTED,
				EnchantingTableMenu.toggleButton(enchantment, enchantments),
				problems.stream().filter(problem -> problem instanceof EnchantmentProblem about && about.concerns(enchantment)).toList()
			))
			.toList();
	}

	/** The deposit is due on the first save of an unenchanted item. */
	@Override
	public Cost cost(boolean infiniteMaterials) {
		if (infiniteMaterials) return new Cost(0, false);

		return new Cost(chosenCount(), !item.isEnchanted());
	}

	@Override
	public List<Problem> problems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials) {
		List<Holder<Enchantment>> chosen = chosen();
		List<Problem> problems = new ArrayList<>();

		if (chosen.isEmpty()) problems.add(new NothingSelected());

		problems.addAll(choiceProblems(chosen));
		problems.addAll(slotAndPaymentProblems(playerLevel, lapis, material, infiniteMaterials));

		return problems;
	}

	@Override
	public ItemStack saved() {
		ItemStack saved = item.copy();

		EnchantmentHelper.setEnchantments(saved, selection());

		return saved;
	}

	private List<Problem> choiceProblems(List<Holder<Enchantment>> chosen) {
		List<Problem> problems = new ArrayList<>();

		for (Holder<Enchantment> enchantment : chosen) {
			int offered = offer(enchantment);

			if (!enchantment.value().canEnchant(item)) problems.add(new NotForItem(enchantment));
			if (offered == 0 || offered < level(enchantment)) problems.add(new NotInLibrary(enchantment, shownLevel(enchantment), offered));
		}

		for (int first = 0; first < chosen.size(); first++) {
			for (int second = first + 1; second < chosen.size(); second++) {
				if (!Enchantment.areCompatible(chosen.get(first), chosen.get(second))) {
					problems.add(new Incompatible(chosen.get(first), chosen.get(second)));
				}
			}
		}

		return problems;
	}

	private int offer(Holder<Enchantment> enchantment) {
		return offers.getOrDefault(enchantment, 0);
	}

	private int itemLevel(Holder<Enchantment> enchantment) {
		return item.getEnchantments().getLevel(enchantment);
	}
}

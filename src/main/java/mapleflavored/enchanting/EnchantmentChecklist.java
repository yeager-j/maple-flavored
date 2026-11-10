package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * @param states each enchantment's state; enchantments left out are {@link State#UNSELECTED}. A {@link State#CARRIED}
 *     one saves at the Library's level when that is at least the item's, and keeps the item's level otherwise, which
 *     blocks the save until the player unticks it.
 * @param enchantments the enchantment registry, which orders and numbers the rows
 * @param library the table's bookshelves and Library Progress
 * @param saver who is saving, and what they pay with
 */
public record EnchantmentChecklist(
	ItemStack item,
	Map<Holder<Enchantment>, Integer> offers,
	Map<Holder<Enchantment>, Checklist.State> states,
	Registry<Enchantment> enchantments,
	Library.Status library,
	Checklist.Saver saver
) implements Checklist {
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
		if (!MaterialUnits.hasMaterial(item)) return false;

		return enchantments.stream().anyMatch(enchantment -> enchantment.canEnchant(item));
	}

	/**
	 * The enchantments listed, in tooltip order with any that order leaves out last: the Library's offers the item can
	 * take, the item's own enchantments, and anything chosen, so a choice whose book was taken away can still be
	 * unticked.
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

	/** The number of the enchantment's row: its id in the enchantment registry. */
	public int row(Holder<Enchantment> enchantment) {
		return enchantments.asHolderIdMap().getId(enchantment);
	}

	@Override
	public List<Row> rows() {
		List<Problem> problems = choiceProblems();

		return listed().stream()
			.map(enchantment -> new Row(
				row(enchantment),
				Enchantment.getFullname(enchantment, shownLevel(enchantment)),
				state(enchantment) != State.UNSELECTED,
				problems.stream().filter(problem -> problem instanceof EnchantmentProblem about && about.concerns(enchantment)).toList()
			))
			.toList();
	}

	@Override
	public State state(int row) {
		Holder<Enchantment> enchantment = enchantments.asHolderIdMap().byId(row);

		return enchantment == null ? State.UNSELECTED : state(enchantment);
	}

	@Override
	public Optional<Checklist> toggled(int row) {
		Holder<Enchantment> enchantment = enchantments.asHolderIdMap().byId(row);

		if (enchantment == null || !isListed(enchantment)) return Optional.empty();

		Map<Holder<Enchantment>, State> toggled = new HashMap<>(states);

		toggled.put(enchantment, state(enchantment).toggled());

		return Optional.of(withStates(toggled));
	}

	@Override
	public Checklist preselected() {
		Map<Holder<Enchantment>, State> carried = new HashMap<>();

		item.getEnchantments().keySet().forEach(enchantment -> carried.put(enchantment, State.CARRIED));

		return withStates(carried);
	}

	@Override
	public List<Problem> choiceProblems() {
		List<Holder<Enchantment>> chosen = chosen();
		List<Problem> problems = new ArrayList<>();

		if (chosen.isEmpty()) problems.add(new NothingSelected());

		for (Holder<Enchantment> enchantment : chosen) {
			int offered = offer(enchantment);
			boolean missing = offered == 0 || offered < level(enchantment);

			if (!enchantment.value().canEnchant(item)) problems.add(new NotForItem(enchantment));
			// An unpowered table offers nothing, which is the one problem to report, not one per enchantment.
			if (missing && library.isPowered()) problems.add(new NotInLibrary(enchantment, shownLevel(enchantment), offered));
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

	@Override
	public ItemStack saved() {
		ItemStack saved = item.copy();

		EnchantmentHelper.setEnchantments(saved, selection());

		return saved;
	}

	private EnchantmentChecklist withStates(Map<Holder<Enchantment>, State> newStates) {
		return new EnchantmentChecklist(item, offers, Map.copyOf(newStates), enchantments, library, saver);
	}

	private int offer(Holder<Enchantment> enchantment) {
		return offers.getOrDefault(enchantment, 0);
	}

	private int itemLevel(Holder<Enchantment> enchantment) {
		return item.getEnchantments().getLevel(enchantment);
	}
}

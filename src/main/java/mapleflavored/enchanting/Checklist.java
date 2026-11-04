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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Enchanting Table's checklist for one item: which enchantments it lists, which are chosen and at what level, what
 * saving costs, and why a save would be refused. Both the table's menu and its screen build one from the same data, so
 * the server and the client apply one rule.
 *
 * @param item the item in the table
 * @param offers the Library's offers, as {@link Library#offers()} gives them
 * @param states each enchantment's state; enchantments left out are {@link State#UNSELECTED}
 * @param enchantments the enchantment registry, which orders the rows
 */
public record Checklist(
	ItemStack item,
	Map<Holder<Enchantment>, Integer> offers,
	Map<Holder<Enchantment>, State> states,
	Registry<Enchantment> enchantments
) {
	private static final String KEY_PREFIX = "container.maple_flavored.enchanting_table.";

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

	/** What saving takes from the table's slots. */
	public record Cost(int lapis, boolean deposit) {
	}

	/** A reason the table refuses to save. */
	public sealed interface Problem {
		/** The reason, for the player. */
		Component describe();

		/** Whether the reason is about this enchantment's row. */
		default boolean concerns(Holder<Enchantment> enchantment) {
			return false;
		}
	}

	/** Nothing is chosen. The grindstone, not the table, strips an item. */
	public record NothingSelected() implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "nothing_selected");
		}
	}

	/** A chosen enchantment can't go on the item. */
	public record NotForItem(Holder<Enchantment> enchantment) implements Problem {
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
	public record NotInLibrary(Holder<Enchantment> enchantment, int level, int offered) implements Problem {
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
	public record Incompatible(Holder<Enchantment> first, Holder<Enchantment> second) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "incompatible", first.value().description(), second.value().description());
		}

		@Override
		public boolean concerns(Holder<Enchantment> other) {
			return first.equals(other) || second.equals(other);
		}
	}

	/** More enchantments are chosen than the saving player has Enchantment Slots for. */
	public record TooMany(int chosen, int slots) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "too_many", chosen, slots);
		}
	}

	/** The lapis slot holds less than the save costs. */
	public record NeedsLapis(int lapis) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "needs_lapis", lapis);
		}
	}

	/** The item pays its deposit on this save, and the material slot doesn't hold its material. */
	public record NeedsMaterial(Component material) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "needs_material", material);
		}
	}

	/**
	 * Whether the table takes the item: it has a material unit to pay its deposit with, and some enchantment can go on
	 * it. Books and golden apples have no enchantments that can go on them, so they are refused.
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
	public List<Holder<Enchantment>> rows() {
		List<Holder<Enchantment>> rows = new ArrayList<>();

		for (Holder<Enchantment> enchantment : enchantments.getTagOrEmpty(EnchantmentTags.TOOLTIP_ORDER)) {
			if (isRow(enchantment)) rows.add(enchantment);
		}

		enchantments.listElements()
			.filter(this::isRow)
			.filter(enchantment -> !rows.contains(enchantment))
			.forEach(rows::add);

		return rows;
	}

	/** Whether the enchantment is listed, so the player can toggle it. */
	public boolean isRow(Holder<Enchantment> enchantment) {
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
		return rows().stream().filter(enchantment -> state(enchantment) != State.UNSELECTED).toList();
	}

	/** The item's enchantments once saved: exactly the chosen ones, at their levels. */
	public ItemEnchantments selection() {
		ItemEnchantments.Mutable selection = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		chosen().forEach(enchantment -> selection.set(enchantment, level(enchantment)));

		return selection.toImmutable();
	}

	/**
	 * What saving takes: 1 lapis per chosen enchantment, plus the deposit on the first save of an unenchanted item.
	 * Players with infinite materials pay nothing.
	 */
	public Cost cost(boolean infiniteMaterials) {
		if (infiniteMaterials) return new Cost(0, false);

		return new Cost(chosen().size(), !item.isEnchanted());
	}

	/**
	 * Every reason the table would refuse to save, empty when it can save. Players with infinite materials skip the
	 * slot, lapis and material checks, but still need the Library to hold what they choose.
	 */
	public List<Problem> problems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials) {
		List<Holder<Enchantment>> chosen = chosen();
		List<Problem> problems = new ArrayList<>();

		if (chosen.isEmpty()) problems.add(new NothingSelected());

		problems.addAll(choiceProblems(chosen));

		int slots = EnchantmentSlots.available(playerLevel, item);

		if (!infiniteMaterials && chosen.size() > slots) problems.add(new TooMany(chosen.size(), slots));

		problems.addAll(paymentProblems(cost(infiniteMaterials), lapis, material));

		return problems;
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

	private List<Problem> paymentProblems(Cost cost, ItemStack lapis, ItemStack material) {
		List<Problem> problems = new ArrayList<>();
		int lapisHeld = lapis.is(Items.LAPIS_LAZULI) ? lapis.getCount() : 0;

		if (lapisHeld < cost.lapis()) problems.add(new NeedsLapis(cost.lapis()));

		boolean holdsMaterial = MaterialUnits.materialOf(item).map(unit -> unit.test(material)).orElse(false);

		if (cost.deposit() && !holdsMaterial) {
			problems.add(new NeedsMaterial(MaterialUnits.materialName(item).orElse(item.getHoverName())));
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

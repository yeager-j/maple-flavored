package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Enchanting Table's checklist for the item in it: what the player can choose, what saving the choices costs, and
 * why a save would be refused. Gear gets an {@link EnchantmentChecklist}, golden apples an {@link AppleChecklist}, and
 * a table with no item an {@link EmptyChecklist}. The table's menu and its screen build the same checklist from the
 * same data, so the server and the client apply one rule.
 */
public sealed interface Checklist permits EnchantmentChecklist, AppleChecklist, EmptyChecklist {
	/** The prefix of the table's translation keys. */
	String KEY_PREFIX = "container.maple_flavored.enchanting_table.";

	/** Whether a row is chosen, and so where what it saves comes from. */
	enum State {
		/** Not chosen. */
		UNSELECTED,
		/** Chosen by the player, to save what the Library offers. */
		SELECTED,
		/** Already on the item when it went in, to save what the item has unless the Library offers more. */
		CARRIED;

		/** The state after the player clicks the row: chosen rows untick, and an unticked row becomes {@link #SELECTED}. */
		public State toggled() {
			return this == UNSELECTED ? SELECTED : UNSELECTED;
		}
	}

	/**
	 * One row the screen lists.
	 *
	 * @param id the row's number, fixed for the kind of checklist whatever else is listed, which {@link #toggled} takes
	 * @param name what the row would save
	 * @param chosen whether the row is ticked
	 * @param problems the reasons the table refuses to save that are about this row
	 */
	record Row(int id, Component name, boolean chosen, List<Problem> problems) {
	}

	/** What saving takes: Enchantment Slots, lapis from the lapis slot, and the Deposit from the material slot. */
	record Cost(int slots, int lapis, boolean deposit) {
		/** What saving takes from a player with infinite materials. */
		public static final Cost NOTHING = new Cost(0, 0, false);
	}

	/**
	 * Who is saving, and what they have put in the table to pay with.
	 *
	 * @param level the player's level, which sets their Enchantment Slots
	 * @param infiniteMaterials whether the player pays nothing, as in creative
	 * @param lapis the stack in the lapis slot
	 * @param material the stack in the material slot
	 */
	record Saver(int level, boolean infiniteMaterials, ItemStack lapis, ItemStack material) {
		/** The player as a saver, with the table's lapis and material stacks. */
		public static Saver of(Player player, ItemStack lapis, ItemStack material) {
			return new Saver(player.experienceLevel, player.hasInfiniteMaterials(), lapis, material);
		}

		private Cost cost(Checklist checklist) {
			if (infiniteMaterials) return Cost.NOTHING;

			return new Cost(checklist.chosenCount(), checklist.chosenCount(), EnchantingDeposit.isDue(checklist.item()));
		}

		private List<Problem> problems(Checklist checklist) {
			List<Problem> problems = new ArrayList<>();
			ItemStack item = checklist.item();
			Cost cost = cost(checklist);
			int lapisHeld = lapis.is(Items.LAPIS_LAZULI) ? lapis.getCount() : 0;
			boolean holdsMaterial = MaterialUnits.isMaterialOf(item, material);

			if (cost.slots() > checklist.slots()) problems.add(new TooMany(cost.slots(), checklist.slots()));
			if (lapisHeld < cost.lapis()) problems.add(new NeedsLapis(cost.lapis()));
			if (cost.deposit() && !holdsMaterial) problems.add(new NeedsMaterial(MaterialUnits.materialName(item).orElse(item.getHoverName())));

			return problems;
		}
	}

	/** A reason the table refuses to save. */
	interface Problem {
		/** The reason, for the player. */
		Component describe();
	}

	/** The table has too few bookshelves around it to offer anything. */
	record NeedsBookshelves(int bookshelves) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "needs_bookshelves", Library.REQUIRED_BOOKSHELVES, bookshelves);
		}
	}

	/** More things are chosen than the saving player has Enchantment Slots for. */
	record TooMany(int chosen, int slots) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "too_many", chosen, slots);
		}
	}

	/** The lapis slot holds less than the save costs. */
	record NeedsLapis(int lapis) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "needs_lapis", lapis);
		}
	}

	/** The item pays its Deposit on this save, and the material slot doesn't hold its material. */
	record NeedsMaterial(Component material) implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "needs_material", material);
		}
	}

	/** Whether the table takes the item: gear it can enchant, or a golden apple of either kind. */
	static boolean accepts(ItemStack item, Registry<Enchantment> enchantments) {
		return EnchantmentChecklist.accepts(item, enchantments) || AppleChecklist.accepts(item);
	}

	/** The item in the table, or an empty stack for none. */
	ItemStack item();

	/** The table's bookshelves and Library Progress. */
	Library.Status library();

	/** Who is saving, and what they pay with. */
	Saver saver();

	/** How many rows are chosen, each taking an Enchantment Slot and a lapis. */
	int chosenCount();

	/** The rows the screen lists, in order. */
	List<Row> rows();

	/** The state of the row numbered {@code row}, {@link State#UNSELECTED} for a number that is no row's. */
	State state(int row);

	/** This checklist with the row numbered {@code row} clicked, or empty if no such row is listed. */
	Optional<Checklist> toggled(int row);

	/** This checklist as it starts for its item: exactly what the item already has is {@link State#CARRIED}. */
	Checklist preselected();

	/** The reasons the table refuses to save that are about the choices themselves, whoever saves. */
	List<Problem> choiceProblems();

	/** A copy of the item with the choices saved onto it, before anything is paid. */
	ItemStack saved();

	/** The Enchantment Slots the saver has for the item. */
	default int slots() {
		return EnchantmentSlots.available(saver().level(), item());
	}

	/**
	 * What saving takes: an Enchantment Slot and 1 lapis per chosen row, plus the Deposit on a plain item's first save.
	 * Players with infinite materials pay nothing.
	 */
	default Cost cost() {
		return saver().cost(this);
	}

	/**
	 * Every reason the table would refuse to save, empty when it can save. Players with infinite materials skip the
	 * slot, lapis and material checks, but still need the Library to offer what they choose.
	 */
	default List<Problem> problems() {
		List<Problem> problems = new ArrayList<>();

		if (!library().isPowered()) problems.add(new NeedsBookshelves(library().bookshelves()));

		problems.addAll(choiceProblems());
		problems.addAll(saver().problems(this));

		return problems;
	}
}

package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.List;
import mapleflavored.materialunits.MaterialUnits;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The Enchanting Table's checklist for the item in it: what the player can choose, what saving the choices costs, and
 * why a save would be refused. Gear gets an {@link EnchantmentChecklist} and golden apples an {@link AppleChecklist}.
 * The table's menu and its screen build the same checklist from the same data, so the server and the client apply one
 * rule.
 */
public sealed interface Checklist permits EnchantmentChecklist, AppleChecklist {
	/** The prefix of the table's translation keys. */
	String KEY_PREFIX = "container.maple_flavored.enchanting_table.";

	/** One row the screen lists: its name, whether it's chosen, the menu button that toggles it, and its problems. */
	record Row(Component name, boolean chosen, int button, List<Problem> problems) {
	}

	/** What saving takes from the table's slots. */
	record Cost(int lapis, boolean deposit) {
	}

	/** A reason the table refuses to save. */
	interface Problem {
		/** The reason, for the player. */
		Component describe();
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

	/** The item pays its deposit on this save, and the material slot doesn't hold its material. */
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

	/** The item in the table. */
	ItemStack item();

	/** How many things are chosen, each taking an Enchantment Slot and a lapis. */
	int chosenCount();

	/** The rows the screen lists, in order, each with those of {@code problems} that concern it. */
	List<Row> rows(List<Problem> problems);

	/**
	 * What saving takes: 1 lapis per chosen thing, plus the deposit on the item's first save. Players with infinite
	 * materials pay nothing.
	 */
	Cost cost(boolean infiniteMaterials);

	/**
	 * Every reason the table would refuse to save, empty when it can save. Players with infinite materials skip the
	 * slot, lapis and material checks, but still need the Library to offer what they choose.
	 */
	List<Problem> problems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials);

	/** A copy of the item with the choices saved onto it, before anything is paid. */
	ItemStack saved();

	/** The problems every checklist shares: too few Enchantment Slots, and too little lapis or material. */
	default List<Problem> slotAndPaymentProblems(int playerLevel, ItemStack lapis, ItemStack material, boolean infiniteMaterials) {
		List<Problem> problems = new ArrayList<>();
		int slots = EnchantmentSlots.available(playerLevel, item());

		if (!infiniteMaterials && chosenCount() > slots) problems.add(new TooMany(chosenCount(), slots));

		Cost cost = cost(infiniteMaterials);
		int lapisHeld = lapis.is(Items.LAPIS_LAZULI) ? lapis.getCount() : 0;

		if (lapisHeld < cost.lapis()) problems.add(new NeedsLapis(cost.lapis()));

		boolean holdsMaterial = MaterialUnits.materialOf(item()).map(unit -> unit.test(material)).orElse(false);

		if (cost.deposit() && !holdsMaterial) {
			problems.add(new NeedsMaterial(MaterialUnits.materialName(item()).orElse(item().getHoverName())));
		}

		return problems;
	}
}

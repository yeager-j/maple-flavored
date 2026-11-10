package mapleflavored.enchanting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The checklist for a golden apple: which {@link AppleEffect}s it lists and which are chosen. Each effect takes an
 * Enchantment Slot and a lapis, and the first save of a plain golden apple takes a block of gold as its Deposit.
 *
 * @param item the golden apple, plain or enchanted, in the table
 * @param offered the effects the table's Library offers, as {@link Library#appleOffers()} gives them
 * @param states each effect's state; effects left out are {@link State#UNSELECTED}. A {@link State#CARRIED} one saves
 *     exactly as the apple stores it, and a {@link State#SELECTED} one at the effect's current strength and duration.
 * @param library the table's bookshelves and Library Progress
 * @param saver who is saving, and what they pay with
 */
public record AppleChecklist(
	ItemStack item,
	Set<AppleEffect> offered,
	Map<AppleEffect, Checklist.State> states,
	Library.Status library,
	Checklist.Saver saver
) implements Checklist {
	/** A reason the table refuses to save that is about one effect's row. */
	public interface EffectProblem extends Problem {
		/** Whether the reason is about this effect's row. */
		boolean concerns(AppleEffect effect);
	}

	/** No effect is chosen. The grindstone, not the table, turns an apple plain. */
	public record NoEffectSelected() implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "no_effect_selected");
		}
	}

	/** A chosen effect isn't offered by the table's Library. */
	public record NotOffered(AppleEffect effect) implements EffectProblem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "not_offered", effect.describe());
		}

		@Override
		public boolean concerns(AppleEffect other) {
			return effect == other;
		}
	}

	/** Whether the item is a golden apple, plain or enchanted. */
	public static boolean accepts(ItemStack item) {
		return item.is(Items.GOLDEN_APPLE) || item.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	/** The number of the effect's row. */
	public static int row(AppleEffect effect) {
		return effect.ordinal();
	}

	/**
	 * The effects listed, in {@link AppleEffect} order: those offered, those the apple already gives, and anything
	 * chosen, so a choice the Library no longer offers can still be unticked.
	 */
	public List<AppleEffect> listed() {
		Set<AppleEffect> onApple = AppleEffect.on(item);

		return Arrays.stream(AppleEffect.values())
			.filter(effect -> offered.contains(effect) || onApple.contains(effect) || state(effect) != State.UNSELECTED)
			.toList();
	}

	/** The effect's state. */
	public State state(AppleEffect effect) {
		return states.getOrDefault(effect, State.UNSELECTED);
	}

	/** The chosen effects. */
	public Set<AppleEffect> chosen() {
		Set<AppleEffect> chosen = EnumSet.noneOf(AppleEffect.class);

		Arrays.stream(AppleEffect.values()).filter(effect -> state(effect) != State.UNSELECTED).forEach(chosen::add);

		return chosen;
	}

	/**
	 * The effect as its row would save it: as the apple stores it while {@link State#CARRIED}, and otherwise at the
	 * effect's current strength and duration.
	 */
	public MobEffectInstance instance(AppleEffect effect) {
		Optional<MobEffectInstance> carried = state(effect) == State.CARRIED ? effect.storedOn(item) : Optional.empty();

		return carried.map(MobEffectInstance::new).orElseGet(effect::instance);
	}

	@Override
	public int chosenCount() {
		return chosen().size();
	}

	@Override
	public List<Row> rows() {
		List<Problem> problems = choiceProblems();

		return listed().stream()
			.map(effect -> new Row(
				row(effect),
				AppleEffect.describe(instance(effect)),
				state(effect) != State.UNSELECTED,
				problems.stream().filter(problem -> problem instanceof EffectProblem about && about.concerns(effect)).toList()
			))
			.toList();
	}

	@Override
	public State state(int row) {
		return isRow(row) ? state(AppleEffect.values()[row]) : State.UNSELECTED;
	}

	@Override
	public Optional<Checklist> toggled(int row) {
		if (!isRow(row)) return Optional.empty();

		AppleEffect effect = AppleEffect.values()[row];

		if (!listed().contains(effect)) return Optional.empty();

		Map<AppleEffect, State> toggled = new EnumMap<>(AppleEffect.class);

		toggled.putAll(states);
		toggled.put(effect, state(effect).toggled());

		return Optional.of(withStates(toggled));
	}

	@Override
	public Checklist preselected() {
		Map<AppleEffect, State> carried = new EnumMap<>(AppleEffect.class);

		AppleEffect.on(item).forEach(effect -> carried.put(effect, State.CARRIED));

		return withStates(carried);
	}

	@Override
	public List<Problem> choiceProblems() {
		Set<AppleEffect> chosen = chosen();
		List<Problem> problems = new ArrayList<>();

		if (chosen.isEmpty()) problems.add(new NoEffectSelected());

		// An unpowered table offers nothing, which is the one problem to report, not one per effect.
		if (!library.isPowered()) return problems;

		chosen.stream().filter(effect -> !offered.contains(effect)).forEach(effect -> problems.add(new NotOffered(effect)));

		return problems;
	}

	@Override
	public ItemStack saved() {
		return AppleEffect.enchant(item, chosen().stream().map(this::instance).toList());
	}

	private AppleChecklist withStates(Map<AppleEffect, State> newStates) {
		return new AppleChecklist(item, offered, Map.copyOf(newStates), library, saver);
	}

	private static boolean isRow(int row) {
		return row >= 0 && row < AppleEffect.values().length;
	}
}

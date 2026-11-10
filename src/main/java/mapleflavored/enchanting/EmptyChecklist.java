package mapleflavored.enchanting;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The checklist of a table with no item in it: it lists nothing, costs nothing, and can't save.
 *
 * @param library the table's bookshelves and Library Progress
 * @param saver who is using the table
 */
public record EmptyChecklist(Library.Status library, Saver saver) implements Checklist {
	/** The table holds no item to save onto. */
	public record NoItem() implements Problem {
		@Override
		public Component describe() {
			return Component.translatable(KEY_PREFIX + "no_item");
		}
	}

	@Override
	public ItemStack item() {
		return ItemStack.EMPTY;
	}

	@Override
	public int chosenCount() {
		return 0;
	}

	@Override
	public List<Row> rows() {
		return List.of();
	}

	@Override
	public State state(int row) {
		return State.UNSELECTED;
	}

	@Override
	public Optional<Checklist> toggled(int row) {
		return Optional.empty();
	}

	@Override
	public Checklist preselected() {
		return this;
	}

	@Override
	public List<Problem> choiceProblems() {
		return List.of(new NoItem());
	}

	@Override
	public ItemStack saved() {
		return ItemStack.EMPTY;
	}
}

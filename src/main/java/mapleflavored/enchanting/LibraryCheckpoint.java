package mapleflavored.enchanting;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * A reward a Library unlocks once its {@link Library.Progress} reaches a share of a Full Library. The server gates the
 * reward with {@link Library.Progress#reached}, and the table's screen marks it on the progress bar.
 *
 * @param percent the share of a Full Library's levels needed, from 1 to 100
 * @param icon what the progress bar shows for the reward
 * @param reward the reward's name
 */
public record LibraryCheckpoint(int percent, ItemStackTemplate icon, Component reward) {
	/** Mending, offered only by a Full Library. */
	public static final LibraryCheckpoint MENDING = new LibraryCheckpoint(
		100, new ItemStackTemplate(Items.ENCHANTED_BOOK), Component.translatable("enchantment.minecraft.mending")
	);

	/** Every checkpoint, by rising percent. */
	public static final List<LibraryCheckpoint> ALL = List.of(MENDING);
}

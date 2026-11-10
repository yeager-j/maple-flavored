package mapleflavored.enchanting;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * A reward a Library unlocks once its table is powered and its Library Progress reaches a share of a Full
 * Library. {@link Library.Status#unlocked} is that gate, for the server's offers and for the table's screen, which marks
 * the reward on the progress bar.
 *
 * @param percent the share of a Full Library's levels needed, from 1 to 100
 * @param icon what the progress bar shows for the reward
 * @param reward the reward's name
 */
public record LibraryCheckpoint(int percent, ItemStackTemplate icon, Component reward) {
	/** Mending, offered only by a Full Library. */
	public static final LibraryCheckpoint MENDING = new LibraryCheckpoint(
		Library.Progress.FULL_PERCENT, new ItemStackTemplate(Items.ENCHANTED_BOOK), Component.translatable("enchantment.minecraft.mending")
	);

	/** Every checkpoint, by rising percent: the golden apple effects, then Mending. */
	public static final List<LibraryCheckpoint> ALL = Stream.concat(
		Arrays.stream(AppleEffect.values()).map(LibraryCheckpoint::of),
		Stream.of(MENDING)
	).toList();

	/** Whether only a Full Library reaches this checkpoint. */
	public boolean needsFullLibrary() {
		return percent == Library.Progress.FULL_PERCENT;
	}

	/** The checkpoint for a golden apple effect, shown as a bottle in the effect's color. */
	public static LibraryCheckpoint of(AppleEffect effect) {
		PotionContents tint = new PotionContents(Optional.empty(), Optional.of(effect.color()), List.of(), Optional.empty());
		DataComponentPatch tinted = DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, tint).build();
		ItemStackTemplate icon = new ItemStackTemplate(Items.POTION, tinted);

		return new LibraryCheckpoint(
			effect.percent(), icon, Component.translatable(Checklist.KEY_PREFIX + "apple_reward", effect.describe())
		);
	}
}

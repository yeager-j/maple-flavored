package mapleflavored.hearts;

import java.util.List;
import mapleflavored.MapleFlavored;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** The Milestone advancements, each of which lowers its holder's Heart Floor by one where Milestones count. */
final class Milestones {
	private static final List<Identifier> MILESTONES = List.of(
		MapleFlavored.id("story/obtain_copper_ingot"),
		Identifier.withDefaultNamespace("story/smelt_iron"),
		Identifier.withDefaultNamespace("story/mine_diamond"),
		Identifier.withDefaultNamespace("story/enter_the_nether"),
		MapleFlavored.id("story/obtain_ender_eye"),
		Identifier.withDefaultNamespace("story/enter_the_end"),
		MapleFlavored.id("nether/obtain_netherite_ingot")
	);

	private Milestones() {
	}

	/** How many Milestones the player has completed, in any order. A Milestone missing from the loaded data never counts. */
	static int held(ServerPlayer player) {
		var loadedAdvancements = player.level().getServer().getAdvancements();
		int held = 0;

		for (Identifier milestone : MILESTONES) {
			AdvancementHolder advancement = loadedAdvancements.get(milestone);

			if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) held++;
		}

		return held;
	}
}

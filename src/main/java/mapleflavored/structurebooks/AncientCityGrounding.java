package mapleflavored.structurebooks;

import java.util.List;
import mapleflavored.grounded.Grounded;
import mapleflavored.wardensummoning.WardenTerritory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * An Ancient City keeps players inside its bounds Grounded while its book is in place, so nobody can glide in, take it
 * and glide out. Once the book is gone, however it went, the city Grounds nobody. A city without a book never Grounds
 * anyone. Grounded itself leaves creative and spectator players alone.
 */
final class AncientCityGrounding {
	// How often each player's city is looked up, in ticks. Grounded asks to be kept at least every second, and a player
	// who has just crossed a city's bounds is still far from its book.
	private static final int LOOKUP_INTERVAL = 10;

	private AncientCityGrounding() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % LOOKUP_INTERVAL != 0) return;

			for (ServerPlayer player : level.players()) {
				if (isGroundedByCity(level, player)) Grounded.keepGrounded(player);
			}
		});
	}

	private static boolean isGroundedByCity(ServerLevel level, ServerPlayer player) {
		return WardenTerritory.ancientCityAt(level, player.blockPosition())
			.filter(city -> hasBookInPlace(level, city.getPieces()))
			.isPresent();
	}

	private static boolean hasBookInPlace(ServerLevel level, List<StructurePiece> pieces) {
		return StructureBookPiece.among(pieces).filter(book -> !GoneBooks.isGone(level, book.spot())).isPresent();
	}
}

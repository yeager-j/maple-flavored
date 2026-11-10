package mapleflavored.structurebooks;

import java.util.List;
import mapleflavored.grounded.Grounded;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * An Ancient City keeps players inside its bounds Grounded while its book is in place, so nobody can glide in, take it
 * and glide out. Once the book is gone, however it went, the city Grounds nobody. A city without a book never Grounds
 * anyone. Creative and spectator players are left alone, as a Warden's Subspace leaves them.
 */
final class AncientCityGrounding {
	private AncientCityGrounding() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			for (ServerPlayer player : level.players()) {
				if (isGroundedByCity(level, player)) Grounded.keepGrounded(player);
			}
		});
	}

	private static boolean isGroundedByCity(ServerLevel level, ServerPlayer player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) return false;

		Structure ancientCity = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(BuiltinStructures.ANCIENT_CITY);

		if (ancientCity == null) return false;

		// The whole bounding box, gaps between buildings included, as for Warden Territory.
		StructureStart city = level.structureManager().getStructureAt(player.blockPosition(), ancientCity);

		return city.isValid() && hasBookInPlace(level, city.getPieces());
	}

	private static boolean hasBookInPlace(ServerLevel level, List<StructurePiece> pieces) {
		return StructureBookPiece.among(pieces).filter(book -> !GoneBooks.isGone(level, book.spot())).isPresent();
	}
}

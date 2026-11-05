package mapleflavored.wardensummoning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Warden Territory: the Deep Dark biome and every Ancient City's bounds, the only places a shriek counts for a player. */
public final class WardenTerritory {
	private WardenTerritory() {
	}

	/**
	 * Whether the position is in Warden Territory. An Ancient City's bounds are its whole bounding box, gaps between its
	 * buildings included.
	 */
	public static boolean contains(ServerLevel level, BlockPos position) {
		if (level.getBiome(position).is(Biomes.DEEP_DARK)) return true;

		Structure ancientCity = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(BuiltinStructures.ANCIENT_CITY);

		return ancientCity != null && level.structureManager().getStructureAt(position, ancientCity).isValid();
	}
}

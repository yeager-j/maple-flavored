package mapleflavored.wardensummoning;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Warden Territory: the Deep Dark biome and every Ancient City's bounds, the only places a shriek counts for a player. */
public final class WardenTerritory {
	private WardenTerritory() {
	}

	/** Whether the position is in Warden Territory. */
	public static boolean contains(ServerLevel level, BlockPos position) {
		return level.getBiome(position).is(Biomes.DEEP_DARK) || ancientCityAt(level, position).isPresent();
	}

	/** The Ancient City whose bounds hold the position: its whole bounding box, gaps between its buildings included. */
	public static Optional<StructureStart> ancientCityAt(ServerLevel level, BlockPos position) {
		Structure ancientCity = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(BuiltinStructures.ANCIENT_CITY);

		if (ancientCity == null) return Optional.empty();

		StructureStart city = level.structureManager().getStructureAt(position, ancientCity);

		return city.isValid() ? Optional.of(city) : Optional.empty();
	}
}

package mapleflavored.structurebooks;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The world's terrain as the generator previews it while a structure is planned, before any chunk is built. It knows
 * oceans and underground water and lava, but not caves or lakes added later.
 */
public interface TerrainPreview {
	/** Whether any block in the column at x and z, from {@code minY} to {@code maxY} inclusive, holds water or lava. */
	boolean hasLiquid(int x, int z, int minY, int maxY);

	/** Whether the biome at {@code pos} is in {@code tag}. */
	boolean biomeIs(BlockPos pos, TagKey<Biome> tag);

	/** The preview for one structure's planning. It keeps each column it samples, so ask for it once per structure. */
	static TerrainPreview of(
		ChunkGenerator generator,
		BiomeSource biomeSource,
		Climate.Sampler climateSampler,
		RandomState randomState,
		LevelHeightAccessor heightAccessor
	) {
		Long2ObjectMap<NoiseColumn> columns = new Long2ObjectOpenHashMap<>();
		BiomeResolver biomes = biomeSource.createResolver(climateSampler);

		return new TerrainPreview() {
			@Override
			public boolean hasLiquid(int x, int z, int minY, int maxY) {
				// ChunkPos.pack packs any two ints, here a block column's x and z.
				NoiseColumn column = columns.computeIfAbsent(
					ChunkPos.pack(x, z),
					unused -> generator.getBaseColumn(x, z, heightAccessor, randomState)
				);

				for (int y = minY; y <= maxY; y++) {
					if (!column.getBlock(y).getFluidState().isEmpty()) return true;
				}

				return false;
			}

			@Override
			public boolean biomeIs(BlockPos pos, TagKey<Biome> tag) {
				int quartX = QuartPos.fromBlock(pos.getX());
				int quartY = QuartPos.fromBlock(pos.getY());
				int quartZ = QuartPos.fromBlock(pos.getZ());

				return biomes.getNoiseBiome(quartX, quartY, quartZ).is(tag);
			}
		};
	}
}

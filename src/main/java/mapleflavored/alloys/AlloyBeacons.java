package mapleflavored.alloys;

import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/** But why?: a level 4 beacon whose whole pyramid, all 164 blocks, is Alloy blocks in any mix. */
public final class AlloyBeacons {
	/** The five Alloy blocks. */
	public static final TagKey<Block> ALLOY_BLOCKS = TagKey.create(Registries.BLOCK, MapleFlavored.id("alloy_blocks"));

	private static final int FULL_PYRAMID_LEVELS = 4;

	// The box vanilla's Beaconator credits players in, around the beacon and its pyramid.
	private static final double CREDIT_REACH_HORIZONTAL = 10.0;
	private static final double CREDIT_REACH_VERTICAL = 5.0;

	private AlloyBeacons() {
	}

	/**
	 * Credits But why? to the players near a beacon that just counted its pyramid's levels, when it has all four and
	 * every block is an Alloy block. Called every time a lit beacon counts its levels, so building a pyramid under a
	 * beacon that is already lit counts too.
	 */
	public static void levelsCounted(Level level, BlockPos beacon, int levels) {
		if (levels != FULL_PYRAMID_LEVELS || !isAllAlloy(level, beacon)) return;

		AABB creditBox = new AABB(beacon.getX(), beacon.getY(), beacon.getZ(), beacon.getX(), beacon.getY() - FULL_PYRAMID_LEVELS, beacon.getZ())
			.inflate(CREDIT_REACH_HORIZONTAL, CREDIT_REACH_VERTICAL, CREDIT_REACH_HORIZONTAL);

		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, creditBox)) {
			MapleTriggers.ALLOY_BEACON_CONSTRUCTED.trigger(player);
		}
	}

	// The same layers vanilla's BeaconBlockEntity.updateBase counts: a square of side 2n + 1, n blocks below.
	private static boolean isAllAlloy(Level level, BlockPos beacon) {
		for (int layer = 1; layer <= FULL_PYRAMID_LEVELS; layer++) {
			for (int dx = -layer; dx <= layer; dx++) {
				for (int dz = -layer; dz <= layer; dz++) {
					if (!level.getBlockState(beacon.offset(dx, -layer, dz)).is(ALLOY_BLOCKS)) return false;
				}
			}
		}

		return true;
	}
}

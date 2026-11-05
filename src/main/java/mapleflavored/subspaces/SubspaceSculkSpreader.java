package mapleflavored.subspaces;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.phys.Vec3;

/**
 * The sculk patch a Warden's Subspace spreads where the Warden appeared: a sculk catalyst's spreader, charged once, whose
 * cursors stay within a radius of the Subspace's centre. Its type is how the sculk mixins tell a Subspace's spread from
 * vanilla's.
 */
final class SubspaceSculkSpreader extends SculkSpreader {
	private static final SculkSpreader CATALYST = SculkSpreader.createLevelSpreader();

	private final Subspace subspace;
	private final BlockPos origin;
	private final double radius;

	/**
	 * A patch holding the charge, spreading from the origin as a catalyst would from a mob dying there.
	 *
	 * @param radius how far from the Subspace's centre its cursors may go. What they write reaches a little further:
	 * veins wrap around a converted block, up to √5 blocks from the cursor.
	 */
	SubspaceSculkSpreader(Subspace subspace, BlockPos origin, double radius, int charge) {
		super(
			CATALYST.isWorldGeneration(),
			CATALYST.replaceableBlocks(),
			CATALYST.growthSpawnCost(),
			CATALYST.noGrowthRadius(),
			CATALYST.chargeDecayRate(),
			CATALYST.additionalDecayRate()
		);
		this.subspace = subspace;
		this.origin = origin;
		this.radius = radius;
		addCursors(origin, charge);
	}

	Subspace subspace() {
		return subspace;
	}

	/** Whether a cursor may move to the position: within the patch, and where it can keep spreading. */
	boolean canSpreadTo(LevelAccessor level, BlockPos position) {
		boolean isInPatch = subspace.centre().distanceToSqr(Vec3.atCenterOf(position)) <= Mth.square(radius);

		return isInPatch && level instanceof ServerLevel serverLevel && serverLevel.shouldTickBlocksAt(position);
	}

	/** Spreads its charge for one tick, unless its origin isn't ticking. Returns whether it has charge left. */
	boolean spread(ServerLevel level) {
		if (level.shouldTickBlocksAt(origin)) updateCursors(level, origin, level.getRandom(), true);

		return !getCursors().isEmpty();
	}
}

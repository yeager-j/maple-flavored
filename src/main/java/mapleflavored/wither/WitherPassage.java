package mapleflavored.wither;

import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.bosses.BossKillCredit;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The blocks the Wither moves through as if they weren't there, so it can't be caged in them. Every player inside its
 * Subspace when it moves through one gets Nice Try.
 */
public final class WitherPassage {
	/** The blocks the Wither passes through: bedrock and obsidian. */
	public static final TagKey<Block> PASSES_THROUGH = TagKey.create(Registries.BLOCK, MapleFlavored.id("wither_passes_through"));

	// Shrinks each box along a move, so a Wither only touching a block's face hasn't passed into it.
	private static final double TOUCHING = 1.0E-4;

	// How far apart the boxes along a move are checked: well under a block, so they miss no block the move crosses, while
	// the corners a diagonal move only skirts stay out.
	private static final double STEP = 0.4;

	private WitherPassage() {
	}

	/** Whether the entity, if any, moves through the block without colliding with it. */
	public static boolean passesThrough(@Nullable Entity entity, BlockState state) {
		return entity instanceof WitherBoss && state.is(PASSES_THROUGH);
	}

	/**
	 * Credits the players inside the Wither's Subspace if its move went through a block it passes through. Call after
	 * each move, with the Wither's bounding box from before it.
	 */
	public static void afterMove(WitherBoss wither, AABB before) {
		Vec3 shift = wither.getBoundingBox().getCenter().subtract(before.getCenter());
		int steps = Math.max(1, Mth.ceil(shift.length() / STEP));

		for (int step = 0; step <= steps; step++) {
			AABB along = before.move(shift.scale((double) step / steps)).deflate(TOUCHING);

			if (wither.level().getBlockStates(along).anyMatch(state -> state.is(PASSES_THROUGH))) {
				BossKillCredit.creditPlayersInside(wither, MapleTriggers.WITHER_PASSED_THROUGH);

				return;
			}
		}
	}
}

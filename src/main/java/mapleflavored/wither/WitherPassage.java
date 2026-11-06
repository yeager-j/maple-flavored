package mapleflavored.wither;

import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.subspaces.WitherSubspaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The blocks the Wither moves through as if they weren't there, so it can't be caged in them. Every player inside its
 * Subspace when it moves through one gets Nice Try.
 */
public final class WitherPassage {
	/** The blocks the Wither passes through: bedrock and obsidian. */
	public static final TagKey<Block> PASSES_THROUGH = TagKey.create(Registries.BLOCK, MapleFlavored.id("wither_passes_through"));

	// Shrinks the space a move swept, so a Wither only touching a block's face hasn't passed into it.
	private static final double TOUCHING = 1.0E-4;

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
		AABB swept = before.minmax(wither.getBoundingBox()).deflate(TOUCHING);

		if (wither.level().getBlockStates(swept).anyMatch(state -> state.is(PASSES_THROUGH))) {
			WitherSubspaces.triggerForPlayersInside(wither, MapleTriggers.WITHER_PASSED_THROUGH);
		}
	}
}

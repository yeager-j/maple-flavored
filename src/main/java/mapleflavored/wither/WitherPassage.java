package mapleflavored.wither;

import mapleflavored.MapleFlavored;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The blocks the Wither moves through as if they weren't there, so it can't be caged in them. */
public final class WitherPassage {
	/** The blocks the Wither passes through: bedrock and obsidian. */
	public static final TagKey<Block> PASSES_THROUGH = TagKey.create(Registries.BLOCK, MapleFlavored.id("wither_passes_through"));

	private WitherPassage() {
	}

	/** Whether the entity, if any, moves through the block without colliding with it. */
	public static boolean passesThrough(@Nullable Entity entity, BlockState state) {
		return entity instanceof WitherBoss && state.is(PASSES_THROUGH);
	}
}

package mapleflavored.mixin;

import java.util.function.BiConsumer;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A spawner can't be mined or blown up until it is Cleared, and flares up at a player who tries. Breaking a Cleared
 * one with a pickaxe fires its advancement; its drops come from {@link Spawners}.
 */
@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends BaseEntityBlock {
	private SpawnerBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (!Spawners.isCleared(level, pos)) return 0.0F;

		return super.getDestroyProgress(state, player, level, pos);
	}

	// Vanilla calls this as a survival player starts to mine the block.
	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		super.attack(state, level, pos, player);

		if (level instanceof ServerLevel serverLevel && !Spawners.isCleared(level, pos)) Spawners.refuseHit(serverLevel, pos);
	}

	@Override
	protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
		if (Spawners.isCleared(level, pos)) super.onExplosionHit(state, level, pos, explosion, onHit);
	}

	// Vanilla calls this only for a survival break with the right tool, the same rule that rolls the Pith.
	@Override
	public void playerDestroy(
		ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith
	) {
		super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
		Spawners.onBrokenByPlayer(player, blockEntity);
	}
}

package mapleflavored.structurebooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A structure book: a floating level I book that can't be mined, blown up, pushed or replaced. Using it takes the book.
 */
public class StructureBookBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);
	private static final int PARTICLE_CHANCE = 4;

	public StructureBookBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StructureBookBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) StructureBooks.take(serverPlayer, pos);

		return InteractionResult.SUCCESS;
	}

	// Vanilla lets a bucket or dispenser empty fluid into any non-solid block, destroying it.
	@Override
	protected boolean canBeReplaced(BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(PARTICLE_CHANCE) != 0) return;

		// An enchant particle starts at its position plus its "speed" and drifts back to its position, here the book.
		level.addParticle(
			ParticleTypes.ENCHANT,
			pos.getX() + 0.5,
			pos.getY() + 0.5,
			pos.getZ() + 0.5,
			random.nextFloat() * 2 - 1,
			random.nextFloat() * 2 - 1,
			random.nextFloat() * 2 - 1
		);
	}
}

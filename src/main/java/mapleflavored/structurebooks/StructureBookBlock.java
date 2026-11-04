package mapleflavored.structurebooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A structure book: a floating level I book that can't be mined, blown up, pushed or replaced. Using it takes the book.
 * A book placed in water is waterlogged, so underwater books sit in water rather than an air bubble.
 */
public class StructureBookBlock extends BaseEntityBlock {
	/** Whether the book holds water. Only structure placement sets it: buckets can't fill or drain it. */
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);
	private static final int PARTICLE_CHANCE = 4;

	public StructureBookBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos pos,
		Direction directionToNeighbour,
		BlockPos neighbourPos,
		BlockState neighbourState,
		RandomSource random
	) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));

		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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

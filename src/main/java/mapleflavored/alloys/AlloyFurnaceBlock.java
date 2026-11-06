package mapleflavored.alloys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The Alloy Furnace: a furnace block that faces its placer and is lit while it smelts. */
public class AlloyFurnaceBlock extends AbstractFurnaceBlock {
	// The blast furnace's crackle and smoke, as the Alloy Furnace is built around one.
	private static final double CRACKLE_CHANCE = 0.1;
	private static final double FRONT_OFFSET = 0.52;
	private static final double SMOKE_SPREAD = 0.6;
	private static final double SMOKE_HEIGHT = 9.0 / 16.0;

	AlloyFurnaceBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AlloyFurnaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel serverLevel)) return null;

		return createTickerHelper(type, Alloys.ALLOY_FURNACE_BLOCK_ENTITY, (tickLevel, pos, tickState, furnace) -> furnace.serverTick(serverLevel, pos, tickState));
	}

	@Override
	protected void openContainer(Level level, BlockPos pos, Player player) {
		if (level.getBlockEntity(pos) instanceof AlloyFurnaceBlockEntity furnace) {
			player.openMenu(furnace);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;

		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;

		if (random.nextDouble() < CRACKLE_CHANCE) {
			level.playLocalSound(x, y, z, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
		}

		Direction facing = state.getValue(FACING);
		double sideways = random.nextDouble() * SMOKE_SPREAD - SMOKE_SPREAD / 2;
		double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * FRONT_OFFSET : sideways;
		double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * FRONT_OFFSET : sideways;

		level.addParticle(ParticleTypes.SMOKE, x + dx, y + random.nextDouble() * SMOKE_HEIGHT, z + dz, 0.0, 0.0, 0.0);
	}
}

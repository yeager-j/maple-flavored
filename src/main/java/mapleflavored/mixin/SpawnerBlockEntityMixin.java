package mapleflavored.mixin;

import mapleflavored.spawners.ClearableSpawner;
import mapleflavored.spawners.SpawnerCage;
import mapleflavored.spawners.SpawnerClearing;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives each spawner block its Clearing, saved with it and sent to clients in its update tag. Each server tick records
 * the players in range, and each client tick draws the progress cage.
 */
@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin implements ClearableSpawner {
	@Unique
	private static final String CLEARING = "maple_flavored:clearing";

	@Unique
	private SpawnerClearing clearing = SpawnerClearing.unstarted();

	@Override
	public SpawnerClearing maple_flavored$clearing() {
		return clearing;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void saveClearing(ValueOutput output, CallbackInfo ci) {
		output.store(CLEARING, SpawnerClearing.CODEC, clearing);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void loadClearing(ValueInput input, CallbackInfo ci) {
		clearing = input.read(CLEARING, SpawnerClearing.CODEC).orElseGet(SpawnerClearing::unstarted);
	}

	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void recordPlayersInRange(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity spawner, CallbackInfo ci) {
		Spawners.recordPlayersInRange((ServerLevel) level, pos, spawner);
	}

	@Inject(method = "clientTick", at = @At("HEAD"))
	private static void drawCage(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity spawner, CallbackInfo ci) {
		SpawnerCage.draw(level, pos, spawner);
	}
}

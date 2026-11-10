package mapleflavored.mixin;

import mapleflavored.spawners.ClearableSpawner;
import mapleflavored.spawners.SpawnerClearing;
import mapleflavored.spawners.SpawnerView;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives each spawner block its Clearing, saved with it. Clients are sent only a {@link SpawnerView} of it, in the
 * update tag, which the heat renderer and mining prediction read. Each server tick records the players in range.
 */
@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin implements ClearableSpawner {
	@Unique
	private static final String CLEARING = "maple_flavored:clearing";

	@Unique
	private static final String VIEW = "maple_flavored:spawner_view";

	@Unique
	private SpawnerClearing clearing = SpawnerClearing.unstarted();

	// The view the server last sent. Only a client's copy of the spawner has one.
	@Unique
	private @Nullable SpawnerView sentView;

	@Override
	public SpawnerClearing maple_flavored$clearing() {
		return clearing;
	}

	@Override
	public SpawnerView maple_flavored$view() {
		return sentView != null ? sentView : clearing.view();
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void saveClearing(ValueOutput output, CallbackInfo ci) {
		output.store(CLEARING, SpawnerClearing.CODEC, clearing);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void loadClearing(ValueInput input, CallbackInfo ci) {
		clearing = input.read(CLEARING, SpawnerClearing.CODEC).orElseGet(SpawnerClearing::unstarted);
		sentView = input.read(VIEW, SpawnerView.CODEC).orElse(null);
	}

	// Vanilla builds the update tag from the save, so the Clearing is swapped for its view.
	@Inject(method = "getUpdateTag", at = @At("RETURN"))
	private void sendViewInsteadOfClearing(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> callback) {
		CompoundTag tag = callback.getReturnValue();

		tag.remove(CLEARING);
		tag.store(VIEW, SpawnerView.CODEC, clearing.view());
	}

	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void recordPlayersInRange(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity spawner, CallbackInfo ci) {
		Spawners.recordPlayersInRange((ServerLevel) level, pos, spawner);
	}
}

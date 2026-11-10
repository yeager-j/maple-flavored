package mapleflavored.mixin;

import mapleflavored.structurebooks.GoneBooks;
import mapleflavored.structurebooks.ShriekerKeepers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells the Ancient City's keepers and the structure books about every block change in a loaded chunk, so a keeper
 * shrieker counts as broken and a book as gone however it goes: mined, blown up, pushed or replaced by a command.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
	// Returns the old state when the block changed, or null when nothing did.
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void countBrokenKeeperOrGoneBook(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
		BlockState oldState = cir.getReturnValue();

		if (oldState == null || !(((LevelChunk) (Object) this).getLevel() instanceof ServerLevel serverLevel)) return;

		ShriekerKeepers.onBlockChanged(serverLevel, pos, oldState);
		GoneBooks.onBlockChanged(serverLevel, pos, oldState);
	}
}

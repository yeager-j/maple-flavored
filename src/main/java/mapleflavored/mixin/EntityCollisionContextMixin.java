package mapleflavored.mixin;

import mapleflavored.wither.WitherPassage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the Wither move through the blocks it passes through: an entity's moves ask its context for each block's shape. */
@Mixin(EntityCollisionContext.class)
public abstract class EntityCollisionContextMixin {
	@Shadow
	public abstract @Nullable Entity getEntity();

	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
	private void passThrough(BlockState state, CollisionGetter collisionGetter, BlockPos pos, CallbackInfoReturnable<VoxelShape> callback) {
		if (WitherPassage.passesThrough(getEntity(), state)) callback.setReturnValue(Shapes.empty());
	}
}

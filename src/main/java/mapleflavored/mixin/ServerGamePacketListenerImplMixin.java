package mapleflavored.mixin;

import java.util.Set;
import mapleflavored.zephyr.Zephyr;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells Zephyr when the server moves a player, which every player teleport does through here. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
	private void endZephyrRise(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci) {
		Zephyr.onTeleport(((ServerGamePacketListenerImpl) (Object) this).player);
	}
}

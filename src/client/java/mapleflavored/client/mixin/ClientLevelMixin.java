package mapleflavored.client.mixin;

import mapleflavored.client.subspaces.SculkSky;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A client level's sky turns sculk-dark inside Warden Subspaces. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void addSculkSky(
		EnvironmentAttributeSystem.Builder builder,
		CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> callback
	) {
		SculkSky.addLayers(builder, (ClientLevel) (Object) this);
	}
}

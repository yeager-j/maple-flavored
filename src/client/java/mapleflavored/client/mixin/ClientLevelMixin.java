package mapleflavored.client.mixin;

import mapleflavored.client.bloodmoons.BloodMoonSky;
import mapleflavored.client.subspaces.SculkSky;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A client level's sky turns red during a Blood Moon, and sculk-dark inside Warden Subspaces. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
	private void addSkyLayers(
		EnvironmentAttributeSystem.Builder builder,
		CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> callback
	) {
		ClientLevel level = (ClientLevel) (Object) this;

		// The sculk sky goes on top, so a Warden Subspace stays sculk-dark during a Blood Moon.
		BloodMoonSky.addLayers(builder, level);
		SculkSky.addLayers(builder, level);
	}
}

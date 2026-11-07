package mapleflavored.client.mixin;

import mapleflavored.bloodmoons.BloodMoons;
import mapleflavored.client.bloodmoons.BloodMoonSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The moon turns red during a Blood Moon. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	// The Blood Moon's strength in the level this frame's sky was extracted from.
	@Unique
	private float bloodMoonStrength;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void readBloodMoon(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo callback) {
		bloodMoonStrength = BloodMoons.strength(level);
	}

	@ModifyArg(
		method = "renderMoon",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
		index = 1
	)
	private Vector4f tintBloodMoon(Vector4f moonColor) {
		BloodMoonSky.tintMoon(moonColor, bloodMoonStrength);

		return moonColor;
	}
}

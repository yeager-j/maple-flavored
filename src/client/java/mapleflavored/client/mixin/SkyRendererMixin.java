package mapleflavored.client.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
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

/**
 * The moon turns red during a Blood Moon. The Blood Moon's strength is extracted into the sky's render state with the
 * rest of the sky, and the moon is tinted from the state being drawn.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	// The strength in the state this call to render is drawing. The moon's own method isn't handed the state.
	@Unique
	private float drawnBloodMoonStrength;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void extractBloodMoon(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo callback) {
		BloodMoonSky.extractStrength(state, BloodMoons.strength(level));
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void readBloodMoon(GpuBufferSlice fog, SkyRenderState state, CallbackInfo callback) {
		drawnBloodMoonStrength = BloodMoonSky.strengthIn(state);
	}

	@ModifyArg(
		method = "renderMoon",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
		index = 1
	)
	private Vector4f tintBloodMoon(Vector4f moonColor) {
		BloodMoonSky.tintMoon(moonColor, drawnBloodMoonStrength);

		return moonColor;
	}
}

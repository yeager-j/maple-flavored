package mapleflavored.client.subspaces;

import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * The sculk sky inside a Warden Subspace: the sky, fog and clouds turn sculk-dark, and sunrises fade, as the camera
 * goes inside. It blends in over the last few blocks to the edge, so crossing it is a fade rather than a cut.
 */
public final class SculkSky {
	// Hand-check values.
	private static final Vector3fc SKY = ARGB.vector3fFromRGB24(0x0A2F36);
	private static final Vector3fc FOG = ARGB.vector3fFromRGB24(0x07252B);
	private static final Vector4fc CLOUDS = ARGB.vector4fFromARGB32(0xCC0F3B44);
	private static final Vector4fc NO_SUNRISE = ARGB.vector4fFromARGB32(0x00000000);
	private static final double BLEND_BLOCKS = 8.0;

	private SculkSky() {
	}

	/** Adds the sculk sky to a client level's environment. Call once as the level builds its environment. */
	public static void addLayers(EnvironmentAttributeSystem.Builder builder, ClientLevel level) {
		builder.addPositionalLayer(EnvironmentAttributes.SKY_COLOR, (base, position, biomes) -> ARGB.srgbLerp(depthInside(level, position), base, SKY));
		builder.addPositionalLayer(EnvironmentAttributes.FOG_COLOR, (base, position, biomes) -> ARGB.srgbLerp(depthInside(level, position), base, FOG));
		builder.addPositionalLayer(EnvironmentAttributes.CLOUD_COLOR, (base, position, biomes) -> ARGB.srgbLerp(depthInside(level, position), base, CLOUDS));
		builder.addPositionalLayer(
			EnvironmentAttributes.SUNRISE_SUNSET_COLOR,
			(base, position, biomes) -> ARGB.srgbLerp(depthInside(level, position), base, NO_SUNRISE)
		);
	}

	// 0 outside every Warden Subspace, rising to 1 once the position is BLEND_BLOCKS inside one's edge.
	private static float depthInside(ClientLevel level, Vec3 position) {
		double deepest = Subspaces.containing(level, position).stream()
			.filter(subspace -> subspace.kind() == Subspace.Kind.WARDEN)
			.mapToDouble(subspace -> subspace.radius() - subspace.centre().distanceTo(position))
			.max()
			.orElse(0.0);

		return (float) Mth.clamp(deepest / BLEND_BLOCKS, 0.0, 1.0);
	}
}

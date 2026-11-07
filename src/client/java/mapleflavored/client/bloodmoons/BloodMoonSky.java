package mapleflavored.client.bloodmoons;

import mapleflavored.bloodmoons.BloodMoons;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * The Blood Moon's red sky and moon: the sky, fog, clouds, sunset and moon turn red as a Blood Moon rises, and back as it
 * sets, following {@link BloodMoons#strength}.
 */
public final class BloodMoonSky {
	// Hand-check values.
	private static final Vector3fc SKY = ARGB.vector3fFromRGB24(0x3A0606);
	private static final Vector3fc FOG = ARGB.vector3fFromRGB24(0x2A0505);
	private static final Vector4fc CLOUDS = ARGB.vector4fFromARGB32(0xFF4A1414);
	private static final Vector3fc SUNSET = ARGB.vector3fFromRGB24(0xD01A10);
	private static final Vector3fc MOON = ARGB.vector3fFromRGB24(0xFF4030);

	private BloodMoonSky() {
	}

	/** Adds the Blood Moon's sky to a client level's environment. Call once as the level builds its environment. */
	public static void addLayers(EnvironmentAttributeSystem.Builder builder, ClientLevel level) {
		builder.addPositionalLayer(EnvironmentAttributes.SKY_COLOR, (base, position, biomes) -> ARGB.srgbLerp(BloodMoons.strength(level), base, SKY));
		builder.addPositionalLayer(EnvironmentAttributes.FOG_COLOR, (base, position, biomes) -> ARGB.srgbLerp(BloodMoons.strength(level), base, FOG));
		builder.addPositionalLayer(EnvironmentAttributes.CLOUD_COLOR, (base, position, biomes) -> ARGB.srgbLerp(BloodMoons.strength(level), base, CLOUDS));
		builder.addPositionalLayer(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, (base, position, biomes) -> {
			// The sunset keeps its own fade; only its colour turns red.
			Vector4fc redSunset = new Vector4f(SUNSET.x(), SUNSET.y(), SUNSET.z(), base.w());

			return ARGB.srgbLerp(BloodMoons.strength(level), base, redSunset);
		});
	}

	/** Turns the moon's draw colour red by the Blood Moon's strength, keeping its alpha. */
	public static void tintMoon(Vector4f moonColor, float strength) {
		moonColor.set(
			Mth.lerp(strength, moonColor.x(), MOON.x()),
			Mth.lerp(strength, moonColor.y(), MOON.y()),
			Mth.lerp(strength, moonColor.z(), MOON.z()),
			moonColor.w()
		);
	}
}

package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which Blood Moon last rose in the Overworld, counted in periods of the {@code maple_flavored:blood_moon} timeline.
 * Saved so that a restart during a Blood Moon doesn't announce it again.
 */
public final class BloodMoonState extends SavedData {
	private static final int NONE = -1;
	private static final Codec<BloodMoonState> CODEC = Codec.INT.fieldOf("last_risen").codec().xmap(BloodMoonState::new, BloodMoonState::lastRisen);

	/** The Overworld saved data that holds the state. */
	// Fabric skips data fixing for a null fix type.
	public static final SavedDataType<BloodMoonState> TYPE = new SavedDataType<>(MapleFlavored.id("blood_moon"), BloodMoonState::new, CODEC, null);

	private int lastRisen = NONE;

	private BloodMoonState() {
	}

	private BloodMoonState(int lastRisen) {
		this.lastRisen = lastRisen;
	}

	int lastRisen() {
		return lastRisen;
	}

	void setLastRisen(int bloodMoon) {
		lastRisen = bloodMoon;
		setDirty();
	}
}

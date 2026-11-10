package mapleflavored.deepvariants;

import net.minecraft.world.level.Level;

/**
 * The Overworld's depth bands, which the mob rules read: the surface from Y 63 up, the caves below it, and the deep
 * below Y 0, where Deep variants spawn. No other dimension has bands, so every question here is false outside the
 * Overworld.
 */
public final class OverworldDepths {
	private static final int LOWEST_SURFACE_Y = 63;
	private static final int DEEP_BELOW_Y = 0;

	private OverworldDepths() {
	}

	/** Whether {@code y} is at the surface or above it: Y 63 or higher in the Overworld. */
	public static boolean isSurface(Level level, int y) {
		return level.dimension() == Level.OVERWORLD && y >= LOWEST_SURFACE_Y;
	}

	/** Whether {@code y} is below the surface, in the caves or the deep: under Y 63 in the Overworld. */
	public static boolean isUnderground(Level level, int y) {
		return level.dimension() == Level.OVERWORLD && y < LOWEST_SURFACE_Y;
	}

	/** Whether {@code y} is in the deep: under Y 0 in the Overworld. */
	public static boolean isDeep(Level level, int y) {
		return level.dimension() == Level.OVERWORLD && y < DEEP_BELOW_Y;
	}
}

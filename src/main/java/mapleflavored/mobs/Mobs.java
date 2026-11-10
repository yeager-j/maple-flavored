package mapleflavored.mobs;

/** What Maple registers for mobs in general: noticing entities that are gone for good, and marking raid waves. */
public final class Mobs {
	private Mobs() {
	}

	/** Registers the mob hooks. Call once from mod init. */
	public static void register() {
		GoneForGood.register();
		RaidWaves.register();
	}
}

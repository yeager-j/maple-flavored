package mapleflavored.client.hearts;

/** The client's side of hearts: the death screen's Max Hearts line and the Crystal Heart's activation. */
public final class HeartsClient {
	private HeartsClient() {
	}

	/** Registers the payload receivers and the death screen line. Call once from client init. */
	public static void register() {
		DeathScreenMaxHearts.register();
		CrystalHeartActivation.register();
	}
}

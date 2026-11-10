package mapleflavored.client.deepvariants;

/** The renderers of the Deep variants. */
public final class DeepVariantRenderers {
	private DeepVariantRenderers() {
	}

	/** Registers every Deep variant's renderer. Call once from client init. */
	public static void register() {
		DeepZombieRenderer.register();
		DeepSkeletonRenderer.register();
		DeepCreeperRenderer.register();
		DeepSpiderRenderer.register();
	}
}

package mapleflavored.client.subspaces;

/** The client's side of Subspaces: their shells and the Wither Subspace's dim light. */
public final class SubspacesClient {
	private SubspacesClient() {
	}

	/** Draws Subspaces from now on. Call once from client init. */
	public static void register() {
		SubspaceShellRenderer.register();
		WitherSubspaceLight.register();
	}
}

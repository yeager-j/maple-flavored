package mapleflavored.anvil;

import java.util.Optional;

/** An anvil menu that tells its screen why the anvil refuses its slots. Implemented by a mixin on {@code AnvilMenu}. */
public interface AnvilRefusalMenu {
	/** Why the anvil refuses its current slots, or empty if it doesn't. Decided on the server and synced to the client. */
	Optional<AnvilResults.Refused> mapleflavored$refusal();
}

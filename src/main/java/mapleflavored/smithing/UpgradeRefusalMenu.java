package mapleflavored.smithing;

/** A smithing menu that tells its screen when a material upgrade was refused because an input is enchanted. */
public interface UpgradeRefusalMenu {
	/** Whether the current inputs match a material upgrade that refuses an enchanted input. Synced to the client. */
	boolean mapleflavored$refusedEnchantedInput();
}

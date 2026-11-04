package mapleflavored.smithing;

/** A smithing menu that tells its screen when a material upgrade was refused because the base is enchanted. */
public interface UpgradeRefusalMenu {
	/** Whether the current inputs match a material upgrade that refuses the enchanted base. Synced to the client. */
	boolean mapleflavored$refusedEnchantedBase();
}

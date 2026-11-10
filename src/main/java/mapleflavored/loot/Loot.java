package mapleflavored.loot;

/** Maple's changes to loot: what chests and mobs drop instead of, and on top of, vanilla's loot. */
public final class Loot {
	private Loot() {
	}

	/** Applies every loot change from now on. Call once from mod init. */
	public static void register() {
		// Loot events call their listeners in the order registered, so this is the order drops are rewritten in.
		LootSubstitutions.register();
		LootEnchantments.register();
		LootDiamonds.register();
		StructureChestRolls.register();
		ChestBooks.register();
	}
}

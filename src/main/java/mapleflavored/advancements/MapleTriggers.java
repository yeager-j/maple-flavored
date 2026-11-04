package mapleflavored.advancements;

import mapleflavored.MapleFlavored;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The mod's advancement criteria triggers, used in advancement data as {@code maple_flavored:<name>}. */
public final class MapleTriggers {
	/** A player's Withered Hearts all returned. */
	public static final PlayerTrigger WITHERED_HEART_RECOVERED = register("withered_heart_recovered", new PlayerTrigger());

	/** A player collected one of their own Remnants. */
	public static final PlayerTrigger REMNANT_COLLECTED = register("remnant_collected", new PlayerTrigger());

	/** A player shelved an enchanted book in a Library. */
	public static final PlayerTrigger ENCHANTED_BOOK_SHELVED = register("enchanted_book_shelved", new PlayerTrigger());

	private MapleTriggers() {
	}

	/** Registers the triggers. Call once from mod init, before advancements load. */
	public static void register() {
	}

	private static <T extends CriterionTrigger<?>> T register(String name, T trigger) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, MapleFlavored.id(name), trigger);
	}
}

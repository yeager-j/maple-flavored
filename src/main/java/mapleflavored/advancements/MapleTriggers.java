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

	/** A player has an enchanting table open whose Library is Full. */
	public static final PlayerTrigger FULL_LIBRARY = register("full_library", new PlayerTrigger());

	/** A player broke a Cleared spawner with a tool that drops its Pith. */
	public static final PlayerTrigger CLEARED_SPAWNER_BROKEN = register("cleared_spawner_broken", new PlayerTrigger());

	/** A player's Max Hearts reached 30. */
	public static final PlayerTrigger MAX_HEARTS_CAP_REACHED = register("max_hearts_cap_reached", new PlayerTrigger());

	/** A Wither died while the player was inside its Subspace. */
	public static final PlayerTrigger WITHER_KILLED = register("wither_killed", new PlayerTrigger());

	/** A Wither moved through bedrock or obsidian while the player was inside its Subspace. */
	public static final PlayerTrigger WITHER_PASSED_THROUGH = register("wither_passed_through", new PlayerTrigger());

	/** An Elder Guardian died within 64 blocks of the player. */
	public static final PlayerTrigger ELDER_GUARDIAN_KILLED = register("elder_guardian_killed", new PlayerTrigger());

	/** A Warden died or dug back down near the player. */
	public static final PlayerTrigger WARDEN_SURVIVED = register("warden_survived", new PlayerTrigger());

	/** An Ender Dragon died while the player was in its fight's range. */
	public static final PlayerTrigger ENDER_DRAGON_KILLED = register("ender_dragon_killed", new PlayerTrigger());

	private MapleTriggers() {
	}

	/** Registers the triggers. Call once from mod init, before advancements load. */
	public static void register() {
	}

	private static <T extends CriterionTrigger<?>> T register(String name, T trigger) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, MapleFlavored.id(name), trigger);
	}
}

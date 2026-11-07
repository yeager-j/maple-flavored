package mapleflavored.bloodmoons;

import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.AttributeRange;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timeline;

/**
 * The Blood Moons module: on every 4th full moon the Overworld's night is a Blood Moon. Its schedule is the
 * {@code maple_flavored:blood_moon} timeline, which animates {@link #STRENGTH} on the Overworld clock; this module
 * announces each Blood Moon as it rises, and {@link Restoring} brings structures' mobs back after it.
 */
public final class BloodMoons {
	/**
	 * How fully a Blood Moon is up, from 0 (none) to 1, as the level's timelines set it. Above 0 a Blood Moon is up. Read
	 * on either side with {@link #strength}.
	 */
	public static final EnvironmentAttribute<Float> STRENGTH = Registry.register(
		BuiltInRegistries.ENVIRONMENT_ATTRIBUTE,
		MapleFlavored.id("gameplay/blood_moon"),
		EnvironmentAttribute.builder(AttributeTypes.FLOAT)
			.defaultValue(0.0F)
			.valueRange(AttributeRange.UNIT_FLOAT)
			.syncable()
			.notPositional()
			.build()
	);

	/** Fires once for each Blood Moon, as it rises, after it's announced. */
	public static final Event<Risen> RISEN = EventFactory.createArrayBacked(Risen.class, listeners -> server -> {
		for (Risen listener : listeners) {
			listener.onRisen(server);
		}
	});

	private static final ResourceKey<Timeline> TIMELINE = ResourceKey.create(Registries.TIMELINE, MapleFlavored.id("blood_moon"));

	private BloodMoons() {
	}

	/**
	 * Registers the hooks that announce each Blood Moon as it rises and Restore structures after it. Call once from mod
	 * init.
	 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(BloodMoons::announceRising);
		Restoring.register();
	}

	/** How fully a Blood Moon is up in the level now, from 0 to 1. Works on both the server and the client. */
	public static float strength(Level level) {
		return level.environmentAttributes().getDimensionValue(STRENGTH);
	}

	private static void announceRising(MinecraftServer server) {
		ServerLevel overworld = server.overworld();

		if (strength(overworld) <= 0.0F) return;

		BloodMoonState state = overworld.getDataStorage().computeIfAbsent(BloodMoonState.TYPE);
		// Which Blood Moon this is, so that time set straight from one into the next still announces the next.
		int bloodMoon = server.registryAccess()
			.lookupOrThrow(Registries.TIMELINE)
			.getOrThrow(TIMELINE)
			.value()
			.getPeriodCount(server.clockManager());

		if (bloodMoon == state.lastRisen()) return;

		state.setLastRisen(bloodMoon);
		rise(server);
	}

	private static void rise(MinecraftServer server) {
		Component message = Component.translatable("chat.maple_flavored.blood_moon_rises").withStyle(ChatFormatting.RED);

		server.getPlayerList().broadcastSystemMessage(message, false);

		for (ServerPlayer player : server.overworld().players()) {
			MapleTriggers.BLOOD_MOON_RISEN.trigger(player);
		}

		RISEN.invoker().onRisen(server);
	}

	/** Listens for each Blood Moon rising. */
	@FunctionalInterface
	public interface Risen {
		void onRisen(MinecraftServer server);
	}
}

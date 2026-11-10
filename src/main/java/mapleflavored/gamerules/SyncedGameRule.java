package mapleflavored.gamerules;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;

/**
 * An integer game rule whose value every client is sent. Game rules stay on the server, so a rule the client must also
 * apply, such as one its mining prediction or its rendering reads, is read through this on both sides.
 *
 * <p>A change to the rule reaches clients within a tick. Until a client has been sent a level's value, it reads the
 * rule's default.
 *
 * <pre>{@code
 * private static final SyncedGameRule LIGHT_LEVEL = SyncedGameRule.of(MapleGameRules.WITHER_SUBSPACE_LIGHT_LEVEL);
 *
 * int lightLevel = LIGHT_LEVEL.get(level);
 * }</pre>
 */
public final class SyncedGameRule {
	private static final List<SyncedGameRule> ALL = new CopyOnWriteArrayList<>();

	private final GameRule<Integer> rule;
	private final AttachmentType<Integer> copy;

	private SyncedGameRule(GameRule<Integer> rule) {
		this.rule = rule;
		this.copy = AttachmentRegistry.create(
			MapleFlavored.id("synced_game_rule/" + rule.getIdentifier().getPath()),
			builder -> builder.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all())
		);
	}

	/**
	 * Starts sending the rule's value to every client. Call once for a rule, from a static field of a class that both
	 * the server and the client load while the mod starts.
	 */
	public static SyncedGameRule of(GameRule<Integer> rule) {
		SyncedGameRule synced = new SyncedGameRule(rule);

		ALL.add(synced);

		return synced;
	}

	/** The rule's value in the level. Works on both the server and the client. */
	public int get(Level level) {
		return level.getAttachedOrElse(copy, rule.defaultValue());
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> ALL.forEach(synced -> synced.send(level)));
	}

	private void send(ServerLevel level) {
		Integer value = level.getGameRules().get(rule);

		if (!Objects.equals(level.getAttached(copy), value)) level.setAttached(copy, value);
	}
}

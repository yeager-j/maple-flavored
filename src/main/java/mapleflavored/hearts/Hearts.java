package mapleflavored.hearts;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.Milestones;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The Hearts module: each player's Max Hearts, their Heart Floor, what death costs and Withered Hearts.
 *
 * <p>Max Hearts are per player and survive respawn and relog. Max health follows usable hearts, the Max Hearts not
 * withered. Other modules read and change hearts only through this class.
 */
public final class Hearts {
	private static final int STARTING_MAX_HEARTS = 10;
	static final int MAX_HEARTS_CAP = 30;
	private static final int BASE_HEART_FLOOR = 10;
	private static final int LOWEST_HEART_FLOOR = 3;
	private static final int DEATH_LOSS = 1;
	private static final int HARD_DEATH_LOSS = 2;

	// Named before Withered Hearts existed; kept so saved players don't carry a stale modifier beside a renamed one.
	private static final Identifier USABLE_HEARTS_MODIFIER = MapleFlavored.id("max_hearts");
	private static final Identifier AFTER_ATTACHMENTS_COPIED = MapleFlavored.id("after_attachments_copied");

	private static final AttachmentType<Integer> MAX_HEARTS = AttachmentRegistry.create(
		MapleFlavored.id("max_hearts"),
		builder -> builder
			.persistent(Codec.INT)
			.initializer(() -> STARTING_MAX_HEARTS)
			.copyOnDeath()
			// The owner's client needs Max Hearts to predict a refused Crystal Heart.
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
	);

	private Hearts() {
	}

	/** Registers the Hearts module's state, network payload and game hooks. Call once from mod init. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(MaxHeartsLostPayload.TYPE, MaxHeartsLostPayload.CODEC);

		// Fabric copies attachments to the respawned player in the default phase, so read Max Hearts after it.
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENTS_COPIED);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENTS_COPIED, Hearts::onRespawn);
		ServerPlayerEvents.JOIN.register(Hearts::applyMaxHealth);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) onDeath(player);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> MaxHeartsCommand.register(dispatcher));
		WitheredHearts.register();
	}

	/** The player's Max Hearts, up to 30. A client knows them only for its own player. */
	public static int maxHearts(Player player) {
		return player.getAttachedOrElse(MAX_HEARTS, STARTING_MAX_HEARTS);
	}

	/** The player's Withered Hearts, which never leave fewer than 1 usable heart. A client knows them only for its own player. */
	public static int witheredHearts(Player player) {
		return WitheredHearts.count(player);
	}

	/** The player's Max Hearts that aren't withered, which set their max health. */
	public static int usableHearts(Player player) {
		return maxHearts(player) - witheredHearts(player);
	}

	/** Whether the player is at 30 Max Hearts, so {@link #addMaxHeart} would refuse. Also true on its own client. */
	public static boolean atMaxHeartsCap(Player player) {
		return maxHearts(player) >= MAX_HEARTS_CAP;
	}

	/** The lowest the player's Max Hearts can fall through death: 10, less one per Milestone held on Normal and Hard. */
	public static int heartFloor(ServerPlayer player) {
		return heartFloor(player.level().getDifficulty(), Milestones.held(player));
	}

	/**
	 * Tells the player their new Heart Floor in chat if it is now below {@code previousHeartFloor}.
	 *
	 * <p>Call after anything that can lower the floor, with the floor read just before it.
	 */
	public static void announceHeartFloorDrop(ServerPlayer player, int previousHeartFloor) {
		if (player.level().getServer().isHardcore()) return;

		int heartFloor = heartFloor(player);

		if (heartFloor < previousHeartFloor) {
			Component boldHeartFloor = Component.literal(String.valueOf(heartFloor)).withStyle(ChatFormatting.BOLD);

			player.sendSystemMessage(
				Component.translatable("chat.maple_flavored.heart_floor_lowered", boldHeartFloor).withStyle(ChatFormatting.RED)
			);
		}
	}

	/**
	 * Adds one Max Heart and raises max health to match.
	 *
	 * @return false, changing nothing, when the player is already at 30 Max Hearts
	 */
	public static boolean addMaxHeart(ServerPlayer player) {
		if (atMaxHeartsCap(player)) return false;

		setMaxHearts(player, maxHearts(player) + 1);

		return true;
	}

	private static void onDeath(ServerPlayer player) {
		if (player.level().getServer().isHardcore()) return;

		int maxHearts = maxHearts(player);
		boolean keepHearts = player.level().getGameRules().get(MapleGameRules.KEEP_HEARTS);
		int remainingHearts = keepHearts
			? maxHearts
			: maxHeartsAfterDeath(maxHearts, heartFloor(player), player.level().getDifficulty());

		setMaxHearts(player, remainingHearts);

		if (ServerPlayNetworking.canSend(player, MaxHeartsLostPayload.TYPE)) {
			ServerPlayNetworking.send(player, new MaxHeartsLostPayload(maxHearts - remainingHearts));
		}
	}

	private static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		applyMaxHealth(newPlayer);

		// Vanilla fills health before this event, while max health is still the 20 of a fresh player.
		if (!alive) newPlayer.setHealth(newPlayer.getMaxHealth());
	}

	private static int heartFloor(Difficulty difficulty, int milestonesHeld) {
		boolean milestonesCount = difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;

		if (!milestonesCount) return BASE_HEART_FLOOR;

		return Math.max(LOWEST_HEART_FLOOR, BASE_HEART_FLOOR - milestonesHeld);
	}

	private static int maxHeartsAfterDeath(int maxHearts, int heartFloor, Difficulty difficulty) {
		int loss = difficulty == Difficulty.HARD ? HARD_DEATH_LOSS : DEATH_LOSS;
		int clampedToFloor = Math.max(heartFloor, maxHearts - loss);

		return Math.min(maxHearts, clampedToFloor);
	}

	static void setMaxHearts(ServerPlayer player, int maxHearts) {
		player.setAttached(MAX_HEARTS, maxHearts);
		applyMaxHealth(player);
	}

	/** Sets max health to the player's usable hearts. Call after changing Max Hearts or Withered Hearts. */
	// A permanent modifier is saved with the player and loaded before their health, so health above 20 survives relog.
	// One modifier covers both, so no change passes through a lower max health that would cut health on the way.
	static void applyMaxHealth(ServerPlayer player) {
		double bonusHealth = (usableHearts(player) - STARTING_MAX_HEARTS) * 2.0;
		var modifier = new AttributeModifier(USABLE_HEARTS_MODIFIER, bonusHealth, AttributeModifier.Operation.ADD_VALUE);

		player.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(modifier);
	}
}

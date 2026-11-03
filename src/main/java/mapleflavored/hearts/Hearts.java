package mapleflavored.hearts;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.Milestones;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Hearts module: each player's Max Hearts, their Heart Floor and what death costs.
 *
 * <p>Max Hearts are per player, apply as max health and survive respawn and relog. Other modules read and change
 * them only through this class.
 */
public final class Hearts {
	private static final int STARTING_MAX_HEARTS = 10;
	static final int MAX_HEARTS_CAP = 30;
	private static final int BASE_HEART_FLOOR = 10;
	private static final int LOWEST_HEART_FLOOR = 3;
	private static final int DEATH_LOSS = 1;
	private static final int HARD_DEATH_LOSS = 2;

	private static final Identifier MAX_HEARTS_MODIFIER = MapleFlavored.id("max_hearts");
	private static final Identifier AFTER_ATTACHMENTS_COPIED = MapleFlavored.id("after_attachments_copied");

	private static final AttachmentType<Integer> MAX_HEARTS = AttachmentRegistry.create(
		MapleFlavored.id("max_hearts"),
		builder -> builder.persistent(Codec.INT).initializer(() -> STARTING_MAX_HEARTS).copyOnDeath()
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
	}

	/** The player's Max Hearts, up to 30. */
	public static int maxHearts(ServerPlayer player) {
		return player.getAttachedOrCreate(MAX_HEARTS);
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
			player.sendSystemMessage(Component.translatable("chat.maple_flavored.heart_floor_lowered", heartFloor));
		}
	}

	/**
	 * Adds one Max Heart and raises max health to match.
	 *
	 * @return false, changing nothing, when the player is already at 30 Max Hearts
	 */
	public static boolean addMaxHeart(ServerPlayer player) {
		int maxHearts = maxHearts(player);

		if (maxHearts >= MAX_HEARTS_CAP) return false;

		setMaxHearts(player, maxHearts + 1);

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

	// A permanent modifier is saved with the player and loaded before their health, so health above 20 survives relog.
	private static void applyMaxHealth(ServerPlayer player) {
		double bonusHealth = (maxHearts(player) - STARTING_MAX_HEARTS) * 2.0;
		var modifier = new AttributeModifier(MAX_HEARTS_MODIFIER, bonusHealth, AttributeModifier.Operation.ADD_VALUE);

		player.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(modifier);
	}
}

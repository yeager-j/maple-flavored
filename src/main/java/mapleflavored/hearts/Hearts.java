package mapleflavored.hearts;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import mapleflavored.MapleFlavored;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The Hearts module: each player's Max Hearts, their Heart Floor, what death costs, Withered Hearts and the Crystal
 * Heart.
 *
 * <p>Max Hearts are per player, stay between 1 and 30, and survive respawn and relog. Max health follows usable
 * hearts, the Max Hearts not withered. Other modules read and change hearts only through this class.
 */
public final class Hearts {
	private static final int STARTING_MAX_HEARTS = 10;
	private static final int FEWEST_MAX_HEARTS = 1;
	static final int MAX_HEARTS_CAP = 30;
	private static final double HEALTH_PER_HEART = 2.0;

	// Named before Withered Hearts existed; kept so saved players don't carry a stale modifier beside a renamed one.
	private static final Identifier USABLE_HEARTS_MODIFIER = MapleFlavored.id("max_hearts");
	private static final Identifier AFTER_ATTACHMENTS_COPIED = MapleFlavored.id("after_attachments_copied");

	private static final AttachmentType<Integer> MAX_HEARTS = AttachmentRegistry.create(
		MapleFlavored.id("max_hearts"),
		builder -> builder
			.persistent(Codec.intRange(FEWEST_MAX_HEARTS, MAX_HEARTS_CAP))
			.initializer(() -> STARTING_MAX_HEARTS)
			.copyOnDeath()
			// The owner's client needs Max Hearts to predict a refused Crystal Heart.
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
	);

	/** Consumed for 1 Max Heart; held in either hand, it saves its holder from a lethal hit like a Totem. */
	public static final Item CRYSTAL_HEART = MapleItems.register("crystal_heart", CrystalHeartItem::new, CrystalHeartItem.properties());

	private Hearts() {
	}

	/** Registers the Hearts module's state, network payloads, creative tab place and game hooks. Call once from mod init. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(MaxHeartsLostPayload.TYPE, MaxHeartsLostPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CrystalHeartUsedPayload.TYPE, CrystalHeartUsedPayload.CODEC);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.insertAfter(Items.TOTEM_OF_UNDYING, CRYSTAL_HEART));

		// Fabric copies attachments to the respawned player in the default phase, so read Max Hearts after it.
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENTS_COPIED);
		ServerPlayerEvents.AFTER_RESPAWN.register(AFTER_ATTACHMENTS_COPIED, Hearts::onRespawn);
		ServerPlayerEvents.JOIN.register(Hearts::onJoin);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) onDeath(player);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> MaxHeartsCommand.register(dispatcher));
		WitheredHearts.register();
	}

	/** The player's Max Hearts, from 1 to 30. */
	public static int maxHearts(ServerPlayer player) {
		return player.getAttachedOrElse(MAX_HEARTS, STARTING_MAX_HEARTS);
	}

	/** The player's Withered Hearts, which never leave fewer than 1 usable heart. */
	public static int witheredHearts(ServerPlayer player) {
		return WitheredHearts.count(player);
	}

	/** The player's Max Hearts that aren't withered, which set their max health. */
	public static int usableHearts(ServerPlayer player) {
		return maxHearts(player) - witheredHearts(player);
	}

	/**
	 * The player's hearts as this side knows them. Works on either side.
	 *
	 * @return empty on a client that was never sent this player's hearts, which is every player but its own
	 */
	public static Optional<HeartCounts> known(Player player) {
		// A server stores Max Hearts for every player who has joined, so a client that has none was sent none.
		boolean known = !player.level().isClientSide() || player.hasAttached(MAX_HEARTS);

		if (!known) return Optional.empty();

		int maxHearts = player.getAttachedOrElse(MAX_HEARTS, STARTING_MAX_HEARTS);

		return Optional.of(new HeartCounts(maxHearts, WitheredHearts.count(player)));
	}

	/**
	 * The lowest the player's Max Hearts can fall through death: 10, less one per Milestone held on Normal and Hard.
	 *
	 * @return empty in Hardcore, which has no Heart Floor
	 */
	public static OptionalInt heartFloor(ServerPlayer player) {
		return DeathCost.of(player).map(cost -> OptionalInt.of(cost.heartFloor())).orElseGet(OptionalInt::empty);
	}

	/**
	 * Runs a change that can lower the player's Heart Floor, such as granting an advancement, then tells them their
	 * new floor in chat if it fell.
	 *
	 * @return what {@code change} returned
	 */
	public static <T> T announcingHeartFloorDrop(ServerPlayer player, Supplier<T> change) {
		return announcingHeartFloorDrops(List.of(player), change);
	}

	/**
	 * Runs a change that can lower every player's Heart Floor, such as a new difficulty, then tells each player whose
	 * floor fell their new one in chat.
	 */
	public static void announcingHeartFloorDrops(MinecraftServer server, Runnable change) {
		// Vanilla sets the difficulty once while starting, before there is a player list.
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList == null ? List.of() : List.copyOf(playerList.getPlayers());

		announcingHeartFloorDrops(players, () -> {
			change.run();

			return null;
		});
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

	/**
	 * Sets the player's Max Hearts, kept between 1 and 30, and brings max health and Withered Hearts in line.
	 *
	 * @return the Max Hearts they now have
	 */
	static int setMaxHearts(ServerPlayer player, int requestedMaxHearts) {
		int maxHearts = Math.clamp(requestedMaxHearts, FEWEST_MAX_HEARTS, MAX_HEARTS_CAP);

		player.setAttached(MAX_HEARTS, maxHearts);
		WitheredHearts.keepWithin(player, maxHearts);
		applyMaxHealth(player);

		if (maxHearts >= MAX_HEARTS_CAP) MapleTriggers.MAX_HEARTS_CAP_REACHED.trigger(player);

		return maxHearts;
	}

	/** Sets max health to the player's usable hearts. Call after changing Max Hearts or Withered Hearts. */
	// A permanent modifier is saved with the player and loaded before their health, so health above 20 survives relog.
	// One modifier covers both, so no change passes through a lower max health that would cut health on the way.
	static void applyMaxHealth(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		double bonusHealth = usableHearts(player) * HEALTH_PER_HEART - maxHealth.getBaseValue();

		maxHealth.addOrReplacePermanentModifier(
			new AttributeModifier(USABLE_HEARTS_MODIFIER, bonusHealth, AttributeModifier.Operation.ADD_VALUE)
		);
	}

	private static <T> T announcingHeartFloorDrops(List<ServerPlayer> players, Supplier<T> change) {
		Map<ServerPlayer, OptionalInt> heartFloorsBefore = new HashMap<>();

		for (ServerPlayer player : players) {
			heartFloorsBefore.put(player, heartFloor(player));
		}

		T result = change.get();

		heartFloorsBefore.forEach(Hearts::announceHeartFloorBelow);

		return result;
	}

	private static void announceHeartFloorBelow(ServerPlayer player, OptionalInt previousHeartFloor) {
		OptionalInt heartFloor = heartFloor(player);
		boolean fell = heartFloor.isPresent() && previousHeartFloor.isPresent() && heartFloor.getAsInt() < previousHeartFloor.getAsInt();

		if (!fell) return;

		Component boldHeartFloor = Component.literal(String.valueOf(heartFloor.getAsInt())).withStyle(ChatFormatting.BOLD);

		player.sendSystemMessage(
			Component.translatable("chat.maple_flavored.heart_floor_lowered", boldHeartFloor).withStyle(ChatFormatting.RED)
		);
	}

	private static void onJoin(ServerPlayer player) {
		// Stored even at the starting value, so the player's client is sent it and can tell its own player's hearts
		// from hearts it was never sent.
		setMaxHearts(player, player.getAttachedOrCreate(MAX_HEARTS));
	}

	private static void onDeath(ServerPlayer player) {
		DeathCost.of(player).ifPresent(cost -> {
			int maxHearts = maxHearts(player);
			int remainingHearts = setMaxHearts(player, cost.maxHeartsAfterDeath(maxHearts));

			if (ServerPlayNetworking.canSend(player, MaxHeartsLostPayload.TYPE)) {
				ServerPlayNetworking.send(player, new MaxHeartsLostPayload(maxHearts - remainingHearts));
			}
		});
	}

	private static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		applyMaxHealth(newPlayer);

		// Vanilla fills health before this event, while max health is still the 20 of a fresh player.
		if (!alive) newPlayer.setHealth(newPlayer.getMaxHealth());
	}
}

package mapleflavored.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * Mock players for GameTests, with the steps to run commands as them, move them, kill them and log them out and in.
 * Every test player comes from here: vanilla's own mock players always report Creative and never finish loading.
 */
public final class MockPlayers {
	private static boolean inHardcore;

	private MockPlayers() {
	}

	/**
	 * A loaded mock player in Survival with 10 Max Hearts: items it uses get used up, damage applies and mobs can target
	 * it. Use this unless a test needs Creative.
	 */
	static ServerPlayer survivalPlayer(GameTestHelper helper) {
		return join(helper, GameType.SURVIVAL, cookie -> plainPlayer(helper, cookie)).player();
	}

	/** A loaded mock player in Creative with 10 Max Hearts. */
	static ServerPlayer creativePlayer(GameTestHelper helper) {
		return join(helper, GameType.CREATIVE, cookie -> plainPlayer(helper, cookie)).player();
	}

	/** A loaded mock player in Survival with 10 Max Hearts, holding one {@code item} in its main hand. */
	static ServerPlayer survivalPlayerHolding(GameTestHelper helper, Item item) {
		ServerPlayer player = survivalPlayer(helper);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));

		return player;
	}

	/** A loaded mock player in Survival with 10 Max Hearts that adds each chat message it is sent to {@code chat}. */
	static ServerPlayer survivalPlayerReadingChat(GameTestHelper helper, List<Component> chat) {
		return join(helper, GameType.SURVIVAL, cookie -> new MessageReadingPlayer(helper, cookie, false, chat)).player();
	}

	/** A loaded mock player in Survival with 10 Max Hearts that adds each overlay message it is sent to {@code overlay}. */
	static ServerPlayer survivalPlayerReadingOverlay(GameTestHelper helper, List<Component> overlay) {
		return join(helper, GameType.SURVIVAL, cookie -> new MessageReadingPlayer(helper, cookie, true, overlay)).player();
	}

	/**
	 * A loaded mock player in Survival with 10 Max Hearts, with the channel its connection writes to, where every packet
	 * the server sends it can be read.
	 */
	static Listening survivalPlayerWithChannel(GameTestHelper helper) {
		return join(helper, GameType.SURVIVAL, cookie -> plainPlayer(helper, cookie));
	}

	/** A mock player and the channel the server's packets to it are written to. */
	record Listening(ServerPlayer player, EmbeddedChannel channel) {
		/** The packets sent since this was last called, of the type. */
		<P> List<P> sent(Class<P> type) {
			List<P> packets = new ArrayList<>();

			for (Object message = channel.readOutbound(); message != null; message = channel.readOutbound()) {
				if (type.isInstance(message)) packets.add(type.cast(message));
			}

			return packets;
		}
	}

	// Placed the way GameTestHelper.makeMockServerPlayerInLevel places its mock player, then loaded and put in the game mode.
	private static Listening join(GameTestHelper helper, GameType gameType, Function<CommonListenerCookie, ServerPlayer> create) {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
		ServerPlayer player = create.apply(cookie);
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);

		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		finishLoading(player);
		player.setGameMode(gameType);

		return new Listening(player, channel);
	}

	private static ServerPlayer plainPlayer(GameTestHelper helper, CommonListenerCookie cookie) {
		return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
	}

	// A player that adds each message it is sent of one kind, chat or overlay, to a list.
	private static final class MessageReadingPlayer extends ServerPlayer {
		private final boolean readsOverlay;
		private final List<Component> messages;

		MessageReadingPlayer(GameTestHelper helper, CommonListenerCookie cookie, boolean readsOverlay, List<Component> messages) {
			super(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
			this.readsOverlay = readsOverlay;
			this.messages = messages;
		}

		@Override
		public void sendSystemMessage(Component message, boolean overlay) {
			if (overlay == readsOverlay) messages.add(message);

			super.sendSystemMessage(message, overlay);
		}
	}

	/**
	 * Moves the player as the server does, so the chunks around them load. A player who has moved is invulnerable until
	 * their client's first movement arrives, which a mock never sends, so that wait is cleared.
	 */
	static void teleport(ServerPlayer player, ServerLevel level, Vec3 position) {
		player.teleport(new TeleportTransition(level, position, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		player.hasChangedDimension();
	}

	/** Runs a command with the server's operator permission, as the given player so {@code @s} targets them. */
	static void runCommand(GameTestHelper helper, ServerPlayer player, String command) {
		var server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player), command);
	}

	/** Kills the player and returns the player that respawns in their place, loaded so it can die again. */
	static ServerPlayer dieAndRespawn(GameTestHelper helper, ServerPlayer player) {
		player.kill(player.level());

		return respawn(player);
	}

	/** Respawns a dead player and returns the player in their place, loaded so it can die again. */
	static ServerPlayer respawn(ServerPlayer player) {
		// Respawns the way a client asks to, which also ends the wait for the respawned client that death starts.
		player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));

		ServerPlayer respawned = player.connection.player;

		finishLoading(respawned);

		return respawned;
	}

	/**
	 * Runs {@code body} with the test world in Hardcore, then puts the world back. GameTests share their world, so
	 * nothing in {@code body} may wait for a later tick, and no player may respawn in it: a Hardcore respawn changes
	 * the world's game rules.
	 */
	static void inHardcore(GameTestHelper helper, Runnable body) {
		inHardcore = true;

		try {
			helper.assertTrue(helper.getLevel().getServer().isHardcore(), "the world is Hardcore");
			body.run();
		} finally {
			inHardcore = false;
		}
	}

	/** Whether a test is inside {@link #inHardcore}, which the test server's Hardcore answer follows. */
	public static boolean isInHardcore() {
		return inHardcore;
	}

	/** Logs the player out and back in, returning the player loaded from their saved data into the test's level. */
	static ServerPlayer relog(GameTestHelper helper, ServerPlayer player) {
		logOut(helper, player);

		return logIn(helper, player.getGameProfile());
	}

	/** Logs the player out, saving their data. */
	static void logOut(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	/** Logs a player back in from their saved data, into the test's level. */
	// Mirrors how vanilla's PrepareSpawnTask loads a joining player's saved data before placing them.
	static ServerPlayer logIn(GameTestHelper helper, GameProfile profile) {
		PlayerList playerList = helper.getLevel().getServer().getPlayerList();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		ServerPlayer relogged = new ServerPlayer(
			helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()
		);
		Optional<CompoundTag> savedData = playerList.loadPlayerData(new NameAndId(profile));

		helper.assertTrue(savedData.isPresent(), "the player's data was saved on leaving");
		relogged.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), savedData.get()));

		Connection connection = new Connection(PacketFlow.SERVERBOUND);

		new EmbeddedChannel(connection);
		playerList.placeNewPlayer(connection, relogged, cookie);

		return relogged;
	}

	// Vanilla keeps a player invulnerable until their client reports it has loaded the world, which a mock never does.
	private static void finishLoading(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
	}
}

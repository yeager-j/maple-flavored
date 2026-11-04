package mapleflavored.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mapleflavored.hearts.Hearts;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Mock players for GameTests, with the steps to give them Max Hearts, run commands as them and kill them. */
final class MockPlayers {
	private MockPlayers() {
	}

	/** A mock player that has finished loading, so it can take damage, with the given Max Hearts of at least 10. */
	static ServerPlayer playerWithMaxHearts(GameTestHelper helper, int maxHearts) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		finishLoading(player);

		while (Hearts.maxHearts(player) < maxHearts) {
			Hearts.addMaxHeart(player);
		}

		return player;
	}

	/** A loaded mock player with 10 Max Hearts that adds each chat message it is sent to {@code chat}. */
	static ServerPlayer playerReadingChat(GameTestHelper helper, List<Component> chat) {
		var server = helper.getLevel().getServer();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
		ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public void sendSystemMessage(Component message, boolean overlay) {
				if (!overlay) chat.add(message);

				super.sendSystemMessage(message, overlay);
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);

		// Placed the way GameTestHelper.makeMockServerPlayerInLevel places its mock player.
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		finishLoading(player);

		return player;
	}

	/** Fails the test unless the player has these Max Hearts and the max health they give. */
	static void assertMaxHearts(GameTestHelper helper, ServerPlayer player, int maxHearts) {
		helper.assertValueEqual(Hearts.maxHearts(player), maxHearts, "Max Hearts");
		helper.assertValueEqual(player.getMaxHealth(), maxHearts * 2.0F, "max health");
	}

	/** Runs a command with the server's operator permission, as the given player so {@code @s} targets them. */
	static void runCommand(GameTestHelper helper, ServerPlayer player, String command) {
		var server = helper.getLevel().getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player), command);
	}

	/** Kills the player and returns the player that respawns in their place, loaded so it can die again. */
	static ServerPlayer dieAndRespawn(GameTestHelper helper, ServerPlayer player) {
		player.kill(player.level());

		// Respawns the way a client asks to, which also ends the wait for the respawned client that death starts.
		player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));

		ServerPlayer respawned = player.connection.player;

		finishLoading(respawned);

		return respawned;
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

package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import mapleflavored.hearts.Hearts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Announces a lower Heart Floor to each player when a switch to Normal or Hard brings their Milestones into play. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Shadow
	public abstract PlayerList getPlayerList();

	@WrapMethod(method = "setDifficulty")
	private void announceHeartFloorDrops(Difficulty difficulty, boolean ignoreLock, Operation<Void> original) {
		PlayerList playerList = getPlayerList();

		if (playerList == null) {
			original.call(difficulty, ignoreLock);

			return;
		}

		List<ServerPlayer> players = List.copyOf(playerList.getPlayers());
		int[] heartFloorsBefore = players.stream().mapToInt(Hearts::heartFloor).toArray();

		original.call(difficulty, ignoreLock);

		for (int i = 0; i < players.size(); i++) {
			Hearts.announceHeartFloorDrop(players.get(i), heartFloorsBefore[i]);
		}
	}
}

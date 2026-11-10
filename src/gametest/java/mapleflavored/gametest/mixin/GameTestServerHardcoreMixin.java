package mapleflavored.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mapleflavored.gametest.MockPlayers;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets a test make the test server Hardcore for a moment. Vanilla's test server always answers that it isn't. */
@Mixin(GameTestServer.class)
public abstract class GameTestServerHardcoreMixin {
	@ModifyReturnValue(method = "isHardcore", at = @At("RETURN"))
	private boolean answerHardcoreWhileATestAsks(boolean hardcore) {
		return hardcore || MockPlayers.isInHardcore();
	}
}

package mapleflavored.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Collection;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves the Trade Rebalance experiment out of the test server, which otherwise enables every available pack. Its
 * trades name Curse of Binding, which Maple removes, so a world with it can't load; Maple doesn't support it.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	private static final String TRADE_REBALANCE = "trade_rebalance";

	@ModifyExpressionValue(
		method = "create",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;getAvailableIds()Ljava/util/Collection;")
	)
	private static Collection<String> leaveOutTradeRebalance(Collection<String> available) {
		return available.stream().filter(id -> !id.equals(TRADE_REBALANCE)).toList();
	}
}

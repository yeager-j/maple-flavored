package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.hearts.Hearts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;

/** Announces a lower Heart Floor to each player when a switch to Normal or Hard brings their Milestones into play. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@WrapMethod(method = "setDifficulty")
	private void announceHeartFloorDrops(Difficulty difficulty, boolean ignoreLock, Operation<Void> original) {
		Hearts.announcingHeartFloorDrops((MinecraftServer) (Object) this, () -> original.call(difficulty, ignoreLock));
	}
}

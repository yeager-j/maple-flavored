package mapleflavored.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mapleflavored.hearts.Hearts;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Announces a lower Heart Floor when a Milestone is granted, right after vanilla's advancement message. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@WrapMethod(method = "award")
	private boolean announceHeartFloorDrop(AdvancementHolder holder, String criterion, Operation<Boolean> original) {
		int heartFloorBefore = Hearts.heartFloor(player);
		boolean awarded = original.call(holder, criterion);

		Hearts.announceHeartFloorDrop(player, heartFloorBefore);

		return awarded;
	}
}

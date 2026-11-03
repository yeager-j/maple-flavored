package mapleflavored.mixin;

import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

/** Turns vanilla's Keep Inventory on by default, since death costs Max Hearts and XP instead of items. */
@Mixin(GameRules.class)
public abstract class GameRulesMixin {
	@ModifyArg(
		method = "<clinit>",
		slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=keep_inventory")),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/gamerules/GameRules;registerBoolean(Ljava/lang/String;Lnet/minecraft/world/level/gamerules/GameRuleCategory;Z)Lnet/minecraft/world/level/gamerules/GameRule;",
			ordinal = 0
		),
		index = 2
	)
	private static boolean keepInventoryByDefault(boolean defaultValue) {
		return true;
	}
}

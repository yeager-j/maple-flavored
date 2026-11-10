package mapleflavored.alloys;

import com.mojang.serialization.Codec;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.DeathProtection;

/**
 * The full Orichalcum set's save: a lethal hit leaves the player on half a heart with the Totem of Undying's effects,
 * before any held Totem or Crystal Heart is used. It works in Hardcore too.
 *
 * <p>It starts ready. After a save it recharges over the playtest game rule {@code orichalcum_save_recharge_time} of
 * online play, whether or not the set is worn. Logging out and dying keep the recharge's progress.
 */
public final class OrichalcumSave {
	// Ticks recharged since the last save; absent once the save is ready.
	private static final AttachmentType<Integer> RECHARGE_TICKS = AttachmentRegistry.create(
		MapleFlavored.id("orichalcum_save_recharge_ticks"),
		builder -> builder.persistent(Codec.INT).copyOnDeath()
	);

	private OrichalcumSave() {
	}

	/** Whether the player's save is ready, whatever armor they wear. */
	public static boolean isReady(ServerPlayer player) {
		return !player.hasAttached(RECHARGE_TICKS);
	}

	/**
	 * Saves the player from a lethal hit if they wear a full Orichalcum set and the save is ready, then starts its
	 * recharge. Returns whether they were saved. Never saves from damage a Totem can't, such as the void.
	 */
	public static boolean trySave(ServerPlayer player, DamageSource killingDamage) {
		if (killingDamage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

		if (!isReady(player) || !AlloySets.wearsFullSet(player, Alloy.ORICHALCUM)) return false;

		player.setHealth(1.0F);
		DeathProtection.TOTEM_OF_UNDYING.applyEffects(player.getItemBySlot(EquipmentSlot.CHEST), player);
		player.level().broadcastEntityEvent(player, EntityEvent.PROTECTED_FROM_DEATH);
		player.setAttached(RECHARGE_TICKS, 0);

		return true;
	}

	/** Registers the recharge. Call once from {@link Alloys#register()}. */
	static void register() {
		// Each player is in exactly one level, so this counts every online tick once.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			int rechargeTime = level.getGameRules().get(MapleGameRules.ORICHALCUM_SAVE_RECHARGE_TIME);

			for (ServerPlayer player : level.players()) {
				recharge(player, rechargeTime);
			}
		});
	}

	private static void recharge(ServerPlayer player, int rechargeTime) {
		if (isReady(player)) return;

		int ticks = player.getAttached(RECHARGE_TICKS) + 1;

		if (ticks >= rechargeTime) {
			player.removeAttached(RECHARGE_TICKS);
		} else {
			player.setAttached(RECHARGE_TICKS, ticks);
		}
	}
}

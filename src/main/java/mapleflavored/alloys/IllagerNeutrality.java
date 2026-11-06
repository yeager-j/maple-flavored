package mapleflavored.alloys;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;

/**
 * Orichalcum armor's Illager neutrality: an Illager won't attack a player wearing any Orichalcum piece, as a Piglin won't
 * attack one wearing gold. It still attacks them in a raid, and while it's angry at them: for
 * {@link MapleGameRules#ORICHALCUM_ILLAGER_ANGER_TIME} after they attack it or an Illager it can see nearby, or after a
 * book alert ({@link #anger}).
 */
public final class IllagerNeutrality {
	// Vanilla's reach for a hurt Piglin's call to the Piglins around it.
	private static final double WITNESS_RANGE = 16.0;

	// Each player the Illager is angry at, with the game time its anger at them ends. Saved with the Illager.
	private static final AttachmentType<Map<UUID, Long>> ANGER = AttachmentRegistry.create(
		MapleFlavored.id("orichalcum_anger"),
		builder -> builder.persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG))
	);

	private IllagerNeutrality() {
	}

	/** Registers the Illagers' saved anger. Call once from {@link Alloys#register()}. */
	static void register() {
	}

	/** Whether the Illager leaves the target alone: a player in Orichalcum it isn't raiding or angry at. */
	public static boolean isNeutralTo(AbstractIllager illager, LivingEntity target) {
		if (!(target instanceof Player player)) return false;

		if (AlloyGear.piecesWorn(player, Alloy.ORICHALCUM) == 0 || illager.hasActiveRaid()) return false;

		return !isAngryAt(illager, player);
	}

	/**
	 * Angers the Illager, and every Illager nearby that can see it, at the player behind the damage, if a player is. Call
	 * whenever an Illager takes damage, killing blows included.
	 */
	public static void wasHurt(ServerLevel level, AbstractIllager illager, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player)) return;

		anger(level, illager, player);

		level.getEntitiesOfClass(
			AbstractIllager.class,
			illager.getBoundingBox().inflate(WITNESS_RANGE),
			witness -> witness != illager && witness.hasLineOfSight(illager)
		).forEach(witness -> anger(level, witness, player));
	}

	/**
	 * Makes the Illager hostile to the player, Orichalcum or not, for {@link MapleGameRules#ORICHALCUM_ILLAGER_ANGER_TIME},
	 * renewing any anger it already has at them. For a structure book's alert.
	 */
	public static void anger(ServerLevel level, AbstractIllager illager, ServerPlayer player) {
		long now = level.getGameTime();
		long until = now + level.getGameRules().get(MapleGameRules.ORICHALCUM_ILLAGER_ANGER_TIME);
		Map<UUID, Long> anger = new HashMap<>(illager.getAttachedOrElse(ANGER, Map.of()));

		anger.values().removeIf(end -> end <= now);
		anger.put(player.getUUID(), until);
		illager.setAttached(ANGER, anger);
	}

	private static boolean isAngryAt(AbstractIllager illager, Player player) {
		Long until = illager.getAttachedOrElse(ANGER, Map.<UUID, Long>of()).get(player.getUUID());

		return until != null && illager.level().getGameTime() < until;
	}
}

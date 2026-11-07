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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;

/**
 * Orichalcum armor's Illager neutrality: an Illager, and an Evoker's Vexes, won't attack a player wearing any Orichalcum
 * piece, as a Piglin won't attack one wearing gold. It still attacks them in a raid, and while it's angry at them: for
 * {@link MapleGameRules#ORICHALCUM_ILLAGER_ANGER_TIME} after they attack it or an Illager it can see nearby, and while a
 * structure book's alert has it hunting them.
 */
public final class IllagerNeutrality {
	// Vanilla's reach for a hurt Piglin's call to the Piglins around it.
	private static final double WITNESS_RANGE = 16.0;

	// Each player the Illager is angry at, with the game time its anger at them ends. Saved with the Illager.
	private static final AttachmentType<Map<UUID, Long>> ANGER = AttachmentRegistry.create(
		MapleFlavored.id("orichalcum_anger"),
		builder -> builder.persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG))
	);

	// Marks a Vex an Illager summoned, since its owner stops resolving once the Illager dies or unloads. Saved with the Vex.
	private static final AttachmentType<Boolean> ILLAGER_VEX = AttachmentRegistry.create(
		MapleFlavored.id("illager_vex"),
		builder -> builder.persistent(Codec.BOOL)
	);

	private IllagerNeutrality() {
	}

	/** Registers the Illagers' saved anger and Vexes' Illager owners. Call once from {@link Alloys#register()}. */
	static void register() {
	}

	/**
	 * Whether the mob leaves the target alone: an Illager, or a Vex an Illager summoned, facing a player in Orichalcum the
	 * Illager isn't raiding or angry at. A Vex whose Illager is gone leaves every Orichalcum wearer alone.
	 */
	public static boolean isNeutralTo(Mob attacker, LivingEntity target) {
		if (!(target instanceof Player player) || AlloyGear.piecesWorn(player, Alloy.ORICHALCUM) == 0) return false;

		if (attacker instanceof AbstractIllager illager) return isNeutral(illager, player);

		if (!(attacker instanceof Vex vex)) return false;

		return vex.getOwner() instanceof AbstractIllager owner ? isNeutral(owner, player) : vex.hasAttached(ILLAGER_VEX);
	}

	/**
	 * Remembers that an Illager summoned the Vex, so it stays neutral once the Illager is gone. Call whenever a Vex gets an
	 * owner.
	 */
	public static void ownerSet(Vex vex, Mob owner) {
		if (owner instanceof AbstractIllager) vex.setAttached(ILLAGER_VEX, true);
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
	 * Makes the Illager hostile to the player, Orichalcum or not, for {@link MapleGameRules#ORICHALCUM_ILLAGER_ANGER_TIME}.
	 * Renews any shorter anger it already has at them.
	 */
	public static void anger(ServerLevel level, AbstractIllager illager, ServerPlayer player) {
		anger(level, illager, player, level.getGameRules().get(MapleGameRules.ORICHALCUM_ILLAGER_ANGER_TIME));
	}

	/**
	 * Makes the Illager hostile to the player, Orichalcum or not, for {@code ticks}. Renews any shorter anger it already
	 * has at them, and never cuts a longer one short.
	 */
	public static void anger(ServerLevel level, AbstractIllager illager, ServerPlayer player, long ticks) {
		long now = level.getGameTime();
		Map<UUID, Long> anger = new HashMap<>(illager.getAttachedOrElse(ANGER, Map.of()));

		anger.values().removeIf(end -> end <= now);
		anger.merge(player.getUUID(), now + ticks, Math::max);
		illager.setAttached(ANGER, anger);
	}

	/** Ends the Illager's anger at the player, if it has any. */
	public static void calm(AbstractIllager illager, Player player) {
		Map<UUID, Long> anger = new HashMap<>(illager.getAttachedOrElse(ANGER, Map.of()));

		if (anger.remove(player.getUUID()) == null) return;

		illager.setAttached(ANGER, anger);
	}

	private static boolean isNeutral(AbstractIllager illager, Player player) {
		return !illager.hasActiveRaid() && !isAngryAt(illager, player);
	}

	private static boolean isAngryAt(AbstractIllager illager, Player player) {
		Long until = illager.getAttachedOrElse(ANGER, Map.<UUID, Long>of()).get(player.getUUID());

		return until != null && illager.level().getGameTime() < until;
	}
}

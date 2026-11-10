package mapleflavored.structurebooks;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/**
 * The Bastion's wake event: taking its book sets every Piglin and Piglin Brute in the Bastion hunting the player, however
 * far away they are and whatever the player wears.
 *
 * <p>A hunting Piglin is angry at the player and attacks them from anywhere in the dimension, pathing across the whole
 * Bastion to reach them, for as long as its {@link Hunt} lasts and its anger still names them. Vanilla's own anger, from
 * opening a chest or breaking gold, keeps its 16-block reach.
 */
public final class PiglinAnger {
	private static final Hunt.Hunters<AbstractPiglin> HUNTERS = new Hunt.Hunters<>(AbstractPiglin.class, "hunt", "bastion_hunt");

	private PiglinAnger() {
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> setBastionHunting(player, level, pos));
	}

	/**
	 * The player the Piglin is hunting, if it is and they're here to attack, for its target choice to put first. Keeps the
	 * Piglin's longer follow range only while that's so. The hunt itself lasts until it ends by the {@link Hunt} rule or
	 * the Piglin's anger stops naming its quarry, so a quarry who steps through a portal is hunted again on their
	 * return. Call wherever the Piglin picks or checks its attack target.
	 */
	public static Optional<LivingEntity> updateHunt(ServerLevel level, AbstractPiglin piglin) {
		Brain<?> brain = piglin.getBrain();
		Optional<UUID> quarry = HUNTERS.quarry(piglin);

		// Anger that has moved to someone else is vanilla's now, and outlives the quarry's death.
		if (quarry.isPresent() && !brain.getMemory(MemoryModuleType.ANGRY_AT).equals(quarry)) HUNTERS.end(piglin);

		Hunt.Status status = HUNTERS.update(level, piglin);

		// As a Piglin forgets a dead target, here even when it never saw the death.
		if (status == Hunt.Status.QUARRY_DIED) brain.eraseMemory(MemoryModuleType.ANGRY_AT);

		Optional<LivingEntity> target = status == Hunt.Status.ON
			? BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT).filter(piglin::canAttack)
			: Optional.empty();

		HUNTERS.keepRange(piglin, target.isPresent());

		return target;
	}

	private static void setBastionHunting(ServerPlayer player, ServerLevel level, BlockPos book) {
		HUNTERS.wake(level, book, structure -> structure.is(BuiltinStructures.BASTION_REMNANT), player)
			.forEach(piglin -> angerAt(level, piglin, player));
	}

	// Vanilla's PiglinAi.setAngerTarget without its 16-block range check, for the length of a hunt.
	private static void angerAt(ServerLevel level, AbstractPiglin piglin, ServerPlayer player) {
		Brain<?> brain = piglin.getBrain();

		brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), Hunt.TICKS);

		if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) brain.setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, Hunt.TICKS);
	}
}

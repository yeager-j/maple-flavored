package mapleflavored.structurebooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * The Bastion's wake event: taking its book angers every Piglin and Piglin Brute in the Bastion at the player, however
 * far away they are and whatever the player wears.
 */
final class PiglinAnger {
	// Vanilla's anger duration, as when a Piglin sees a player open its Bastion's chests.
	private static final long ANGER_TICKS = 600L;

	private PiglinAnger() {
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> angerBastionAt(player, level, pos));
	}

	private static void angerBastionAt(ServerPlayer player, ServerLevel level, BlockPos book) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(book, structure -> structure.is(BuiltinStructures.BASTION_REMNANT));

		if (!start.isValid()) return;

		level.getEntitiesOfClass(
			AbstractPiglin.class,
			AABB.of(start.getBoundingBox()),
			piglin -> level.structureManager().structureHasPieceAt(piglin.blockPosition(), start)
		).forEach(piglin -> anger(level, piglin, player));
	}

	// Vanilla's PiglinAi.setAngerTarget without its 16-block range check, so the whole Bastion hears it.
	private static void anger(ServerLevel level, AbstractPiglin piglin, ServerPlayer player) {
		Brain<?> brain = piglin.getBrain();

		brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), ANGER_TICKS);

		if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) brain.setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, ANGER_TICKS);
	}
}

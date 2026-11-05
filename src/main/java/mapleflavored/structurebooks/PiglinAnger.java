package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * The Bastion's wake event: taking its book sets every Piglin and Piglin Brute in the Bastion hunting the player, however
 * far away they are and whatever the player wears.
 *
 * <p>A hunting Piglin is angry at the player and attacks them from anywhere in the dimension, pathing across the whole
 * Bastion to reach them, until the hunt runs out or the player dies. Vanilla's own anger, from opening a chest or
 * breaking gold, keeps its 16-block reach.
 */
public final class PiglinAnger {
	// Ten minutes, long enough to cross the Bastion with all of it still after you. Vanilla's 30 seconds of chest anger
	// would let the far side calm down first. Dying still ends it, as vanilla forgets a dead target.
	private static final long HUNT_TICKS = 10L * 60L * 20L;
	// Lets a hunter plan a path, and sense the player, across a whole Bastion; vanilla's 16 blocks (12 for a Brute) reach
	// only nearby rooms.
	private static final double HUNT_RANGE_BONUS = 64.0;
	private static final Identifier HUNT_RANGE_ID = MapleFlavored.id("bastion_hunt");
	private static final AttributeModifier HUNT_RANGE = new AttributeModifier(HUNT_RANGE_ID, HUNT_RANGE_BONUS, AttributeModifier.Operation.ADD_VALUE);

	private static final AttachmentType<Hunt> HUNT = AttachmentRegistry.create(MapleFlavored.id("hunt"), builder -> builder.persistent(Hunt.CODEC));

	private PiglinAnger() {
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> setBastionHunting(player, level, pos));
	}

	/**
	 * The player the Piglin is hunting, if it is and they're here to attack, for its target choice to put first. Keeps the
	 * Piglin's longer follow range only while that's so. The hunt itself lasts until it runs out or the Piglin's anger
	 * stops naming its quarry, as when the quarry dies, so a quarry who steps through a portal is hunted again on their
	 * return. Call wherever the Piglin picks or checks its attack target.
	 */
	public static Optional<LivingEntity> updateHunt(ServerLevel level, AbstractPiglin piglin) {
		Optional<Hunt> hunt = Optional.ofNullable(piglin.getAttached(HUNT)).filter(active -> active.isOn(level, piglin));

		if (hunt.isEmpty()) piglin.removeAttached(HUNT);

		Optional<LivingEntity> quarry = hunt
			.flatMap(active -> BehaviorUtils.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT))
			.filter(piglin::canAttack);

		keepHuntRange(piglin, quarry.isPresent());

		return quarry;
	}

	private static void keepHuntRange(AbstractPiglin piglin, boolean hunting) {
		AttributeInstance followRange = piglin.getAttribute(Attributes.FOLLOW_RANGE);

		if (followRange == null || followRange.hasModifier(HUNT_RANGE_ID) == hunting) return;

		if (hunting) {
			followRange.addTransientModifier(HUNT_RANGE);
		} else {
			followRange.removeModifier(HUNT_RANGE_ID);
		}
	}

	private static void setBastionHunting(ServerPlayer player, ServerLevel level, BlockPos book) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(book, structure -> structure.is(BuiltinStructures.BASTION_REMNANT));

		if (!start.isValid()) return;

		level.getEntitiesOfClass(
			AbstractPiglin.class,
			AABB.of(start.getBoundingBox()),
			piglin -> level.structureManager().structureHasPieceAt(piglin.blockPosition(), start)
		).forEach(piglin -> hunt(level, piglin, player));
	}

	// Vanilla's PiglinAi.setAngerTarget without its 16-block range check, for the length of a hunt.
	private static void hunt(ServerLevel level, AbstractPiglin piglin, ServerPlayer player) {
		Brain<?> brain = piglin.getBrain();

		brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		brain.setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), HUNT_TICKS);

		if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) brain.setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, HUNT_TICKS);

		piglin.setAttached(HUNT, new Hunt(player.getUUID(), level.getGameTime() + HUNT_TICKS));
		keepHuntRange(piglin, true);
	}

	/**
	 * A Piglin's hunt for the player who took its Bastion's book. Saved with the Piglin.
	 *
	 * @param quarry the player hunted; a later, vanilla anger at someone else is never a hunt
	 * @param until the game time the hunt ends
	 */
	private record Hunt(UUID quarry, long until) {
		static final Codec<Hunt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("quarry").forGetter(Hunt::quarry),
			Codec.LONG.fieldOf("until").forGetter(Hunt::until)
		).apply(instance, Hunt::new));

		boolean isOn(ServerLevel level, AbstractPiglin piglin) {
			Optional<UUID> angryAt = piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT);

			return level.getGameTime() < until && angryAt.equals(Optional.of(quarry));
		}
	}
}

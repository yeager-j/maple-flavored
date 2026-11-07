package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import mapleflavored.MapleFlavored;
import mapleflavored.alloys.IllagerNeutrality;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

/**
 * The Woodland Mansion's wake event: taking its book sets every Illager in the mansion hunting the player, however far
 * away they are and whatever the player wears.
 *
 * <p>A hunting Illager targets the player ahead of anything else and paths across the whole mansion to reach them, and
 * Orichalcum doesn't make it neutral, until the hunt runs out or the player dies. It matches the Bastion's
 * {@link PiglinAnger}.
 */
public final class IllagerAlert {
	// The structures whose Illagers a taken book alerts.
	private static final Set<ResourceKey<Structure>> ALERTED_STRUCTURES = Set.of(BuiltinStructures.WOODLAND_MANSION);

	// As long as a Bastion's hunt, long enough to get out of the mansion with all of it still after you.
	private static final long HUNT_TICKS = 10L * 60L * 20L;
	// Lets a hunter plan a path across a whole mansion; vanilla's 12 blocks (32 for a Pillager) reach only nearby rooms.
	private static final double HUNT_RANGE_BONUS = 64.0;
	private static final Identifier HUNT_RANGE_ID = MapleFlavored.id("illager_hunt");
	private static final AttributeModifier HUNT_RANGE = new AttributeModifier(HUNT_RANGE_ID, HUNT_RANGE_BONUS, AttributeModifier.Operation.ADD_VALUE);

	private static final AttachmentType<Hunt> HUNT = AttachmentRegistry.create(MapleFlavored.id("illager_hunt"), builder -> builder.persistent(Hunt.CODEC));

	private IllagerAlert() {
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> alert(player, level, pos));
	}

	/** The target goal that puts the Illager's hunt first. Add it to every Illager's target selector, ahead of the rest. */
	public static Goal huntGoal(AbstractIllager illager) {
		return new HuntGoal(illager);
	}

	private static void alert(ServerPlayer player, ServerLevel level, BlockPos book) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(book, structure -> ALERTED_STRUCTURES.stream().anyMatch(structure::is));

		if (!start.isValid()) return;

		level.getEntitiesOfClass(
			AbstractIllager.class,
			AABB.of(start.getBoundingBox()),
			illager -> level.structureManager().structureHasPieceAt(illager.blockPosition(), start)
		).forEach(illager -> hunt(level, illager, player));
	}

	private static void hunt(ServerLevel level, AbstractIllager illager, ServerPlayer player) {
		IllagerNeutrality.anger(level, illager, player, HUNT_TICKS);
		illager.setAttached(HUNT, new Hunt(player.getUUID(), level.getGameTime() + HUNT_TICKS, deaths(player)));
		keepHuntRange(illager, true);
	}

	/**
	 * The player the Illager is hunting, if it is and they're here to attack. Keeps the Illager's longer follow range only
	 * while that's so. The hunt itself lasts until it runs out or its quarry dies, wherever they die and whether or not
	 * the Illager was loaded then, so a quarry who steps through a portal is hunted again on their return.
	 */
	private static Optional<Player> updateHunt(AbstractIllager illager) {
		ServerLevel level = (ServerLevel) illager.level();
		Hunt hunt = illager.getAttached(HUNT);
		ServerPlayer quarry = hunt == null ? null : level.getServer().getPlayerList().getPlayer(hunt.quarry());
		OptionalInt quarryDeaths = quarry == null ? OptionalInt.empty() : OptionalInt.of(deaths(quarry));
		HuntStatus status = hunt == null ? HuntStatus.OVER : hunt.statusAt(level.getGameTime(), quarryDeaths);

		if (status != HuntStatus.ON) {
			illager.removeAttached(HUNT);
			keepHuntRange(illager, false);

			// As a Piglin forgets a dead target, so they aren't hunted again once they respawn.
			if (status == HuntStatus.QUARRY_DIED) IllagerNeutrality.calm(illager, quarry);

			return Optional.empty();
		}

		Optional<Player> target = Optional.<Player>ofNullable(quarry)
			.filter(player -> player.level() == level)
			.filter(illager::canAttack);

		keepHuntRange(illager, target.isPresent());

		return target;
	}

	private static int deaths(ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
	}

	private static void keepHuntRange(AbstractIllager illager, boolean hunting) {
		AttributeInstance followRange = illager.getAttribute(Attributes.FOLLOW_RANGE);

		if (followRange == null || followRange.hasModifier(HUNT_RANGE_ID) == hunting) return;

		if (hunting) {
			followRange.addTransientModifier(HUNT_RANGE);
		} else {
			followRange.removeModifier(HUNT_RANGE_ID);
		}
	}

	// Vanilla's target goals only pick a player the Illager can see nearby, so this one hands it its quarry instead.
	private static final class HuntGoal extends TargetGoal {
		private final AbstractIllager illager;

		HuntGoal(AbstractIllager illager) {
			super(illager, false);
			this.illager = illager;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			targetMob = updateHunt(illager).orElse(null);

			return targetMob != null;
		}

		@Override
		public void start() {
			illager.setTarget(targetMob);
			super.start();
		}

		@Override
		public boolean canContinueToUse() {
			Optional<Player> quarry = updateHunt(illager);

			if (quarry.isEmpty() || illager.getTarget() != quarry.get()) return false;

			targetMob = quarry.get();

			return true;
		}
	}

	/**
	 * An Illager's hunt for the player who took its structure's book. Saved with the Illager.
	 *
	 * @param quarry the player hunted
	 * @param until the game time the hunt ends
	 * @param quarryDeaths how many times the quarry had died when the hunt began; a death since ends it
	 */
	private record Hunt(UUID quarry, long until, int quarryDeaths) {
		static final Codec<Hunt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("quarry").forGetter(Hunt::quarry),
			Codec.LONG.fieldOf("until").forGetter(Hunt::until),
			Codec.INT.fieldOf("quarry_deaths").forGetter(Hunt::quarryDeaths)
		).apply(instance, Hunt::new));

		/**
		 * Whether the hunt is still on at game time {@code now}.
		 *
		 * @param quarryDeathsNow how many times the quarry has died by now, or empty when they're offline
		 */
		HuntStatus statusAt(long now, OptionalInt quarryDeathsNow) {
			if (quarryDeathsNow.isPresent() && quarryDeathsNow.getAsInt() != quarryDeaths) return HuntStatus.QUARRY_DIED;

			return now < until ? HuntStatus.ON : HuntStatus.OVER;
		}
	}

	private enum HuntStatus {
		ON,
		/** Ran out, or there was none. */
		OVER,
		QUARRY_DIED
	}
}

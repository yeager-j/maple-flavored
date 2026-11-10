package mapleflavored.structurebooks;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The Woodland Mansion's and Stronghold's wake event: taking the book sets every Illager in the structure hunting the
 * player, however far away they are and whatever the player wears.
 *
 * <p>A hunting Illager targets the player ahead of anything else and paths across the whole structure to reach them, and
 * Orichalcum doesn't make it neutral, for as long as its {@link Hunt} lasts. It matches the Bastion's
 * {@link PiglinAnger}.
 */
public final class IllagerAlert {
	// The structures whose Illagers a taken book alerts.
	private static final Set<ResourceKey<Structure>> ALERTED_STRUCTURES = Set.of(BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.STRONGHOLD);

	private static final Hunt.Hunters<AbstractIllager> HUNTERS = new Hunt.Hunters<>(AbstractIllager.class, "illager_hunt", "illager_hunt");

	private IllagerAlert() {
	}

	static void register() {
		StructureBooks.TAKEN.register((player, level, pos, enchantment) -> alert(player, level, pos));
	}

	/** The target goal that puts the Illager's hunt first. Add it to every Illager's target selector, ahead of the rest. */
	public static Goal huntGoal(AbstractIllager illager) {
		return new HuntGoal(illager);
	}

	/**
	 * Whether the Illager has a hunt for the player who took its structure's book. One that has run out still counts
	 * until the Illager next looks for its quarry.
	 */
	public static boolean isHunting(AbstractIllager illager) {
		return HUNTERS.isHunting(illager);
	}

	/** Whether the Illager's hunt for this player is on right now. */
	public static boolean isHunting(AbstractIllager illager, Player player) {
		return HUNTERS.isHunting(illager, player);
	}

	private static void alert(ServerPlayer player, ServerLevel level, BlockPos book) {
		HUNTERS.wake(level, book, structure -> ALERTED_STRUCTURES.stream().anyMatch(structure::is), player);
	}

	/**
	 * The player the Illager is hunting, if it is and they're here to attack. Keeps the Illager's longer follow range only
	 * while that's so. The hunt itself lasts until it ends by the {@link Hunt} rule, so a quarry who steps through a
	 * portal is hunted again on their return.
	 */
	private static Optional<Player> updateHunt(AbstractIllager illager) {
		ServerLevel level = (ServerLevel) illager.level();
		Optional<Player> target = HUNTERS.update(level, illager) == Hunt.Status.ON
			? HUNTERS.quarry(illager).map(level::getPlayerByUUID).filter(illager::canAttack)
			: Optional.empty();

		HUNTERS.keepRange(illager, target.isPresent());

		return target;
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
}

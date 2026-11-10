package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A mob's hunt for the player who took its structure's book, and the one rule every such hunt follows: it lasts ten
 * minutes, ends early when its quarry dies, wherever they die and whether or not the hunter was loaded then, and gives
 * the hunter the reach to cross its structure. Saved with the hunter.
 *
 * <p>How a hunter is made to attack its quarry differs by mob and stays with each wake event: {@link PiglinAnger} and
 * {@link IllagerAlert}.
 *
 * @param quarry the player hunted
 * @param until the game time the hunt ends
 * @param quarryDeaths how many times the quarry had died when the hunt began; a death since ends it
 */
public record Hunt(UUID quarry, long until, int quarryDeaths) {
	/**
	 * How long a hunt lasts: ten minutes, long enough to cross the structure with all of it still after you. Vanilla's 30
	 * seconds of Piglin anger would let the far side calm down first.
	 */
	static final long TICKS = 10L * 60L * 20L;
	// Lets a hunter plan a path, and sense its quarry, across a whole Bastion or mansion. Vanilla's follow ranges, 12 to
	// 32 blocks, reach only nearby rooms.
	private static final double RANGE_BONUS = 64.0;

	static final Codec<Hunt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("quarry").forGetter(Hunt::quarry),
		Codec.LONG.fieldOf("until").forGetter(Hunt::until),
		Codec.INT.fieldOf("quarry_deaths").forGetter(Hunt::quarryDeaths)
	).apply(instance, Hunt::new));

	private static Hunt startingNow(ServerLevel level, ServerPlayer quarry) {
		return new Hunt(quarry.getUUID(), level.getGameTime() + TICKS, deaths(quarry));
	}

	/**
	 * Whether the hunt is still on at game time {@code now}.
	 *
	 * @param quarryDeathsNow how many times the quarry has died by now, or empty when that can't be read, as while
	 *     they're offline
	 */
	public Status statusAt(long now, OptionalInt quarryDeathsNow) {
		if (quarryDeathsNow.isPresent() && quarryDeathsNow.getAsInt() != quarryDeaths) return Status.QUARRY_DIED;

		return now < until ? Status.ON : Status.OVER;
	}

	private static int deaths(ServerPlayer player) {
		return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
	}

	/** How a hunt stands. */
	public enum Status {
		ON,
		/** Ran out, or there was none. */
		OVER,
		QUARRY_DIED
	}

	/**
	 * One kind of mob that hunts, with where its hunts are saved. Create one per kind, during mod init.
	 *
	 * @param <M> the mobs that hunt
	 */
	static final class Hunters<M extends Mob> {
		private final Class<M> type;
		private final AttachmentType<Hunt> hunt;
		private final AttributeModifier range;

		/**
		 * @param huntName the path of the id the hunters' hunts are saved under
		 * @param rangeName the path of the id of the follow range a hunter gets
		 */
		Hunters(Class<M> type, String huntName, String rangeName) {
			this.type = type;
			this.hunt = AttachmentRegistry.create(MapleFlavored.id(huntName), builder -> builder.persistent(CODEC));
			this.range = new AttributeModifier(MapleFlavored.id(rangeName), RANGE_BONUS, AttributeModifier.Operation.ADD_VALUE);
		}

		/**
		 * Sets every one of these mobs inside the structure that has a piece at the book hunting the player, wherever in
		 * it they are. Does nothing when no structure that wakes has a piece there.
		 *
		 * @param wakes whether a structure's mobs hunt when its book is taken
		 * @return the mobs now hunting
		 */
		List<M> wake(ServerLevel level, BlockPos book, Predicate<Holder<Structure>> wakes, ServerPlayer quarry) {
			StructureStart start = level.structureManager().getStructureWithPieceAt(book, wakes);

			if (!start.isValid()) return List.of();

			List<M> hunters = level.getEntitiesOfClass(
				type,
				AABB.of(start.getBoundingBox()),
				mob -> level.structureManager().structureHasPieceAt(mob.blockPosition(), start)
			);

			for (M hunter : hunters) {
				hunter.setAttached(hunt, startingNow(level, quarry));
				keepRange(hunter, true);
			}

			return hunters;
		}

		/**
		 * How the mob's hunt stands now. Ends the hunt once it's over or its quarry has died.
		 */
		Status update(ServerLevel level, M mob) {
			Hunt current = mob.getAttached(hunt);

			if (current == null) return Status.OVER;

			Status status = current.statusAt(level.getGameTime(), deathsNow(level.getServer().getPlayerList().getPlayer(current.quarry())));

			if (status != Status.ON) end(mob);

			return status;
		}

		/** Ends the mob's hunt, if it has one, along with its longer follow range. */
		void end(M mob) {
			mob.removeAttached(hunt);
			keepRange(mob, false);
		}

		/** Who the mob hunts, if it has a hunt, whether or not it has run out since it was last updated. */
		Optional<UUID> quarry(M mob) {
			return Optional.ofNullable(mob.getAttached(hunt)).map(Hunt::quarry);
		}

		/** Whether the mob has a hunt, whether or not it has run out since it was last updated. */
		boolean isHunting(M mob) {
			return mob.hasAttached(hunt);
		}

		/** Whether the mob's hunt for the player is on right now. Changes nothing. */
		boolean isHunting(M mob, Player player) {
			Hunt current = mob.getAttached(hunt);

			if (current == null || !current.quarry().equals(player.getUUID())) return false;

			return current.statusAt(mob.level().getGameTime(), deathsNow(player)) == Status.ON;
		}

		/**
		 * Gives the mob the longer follow range, or takes it away. A hunter keeps it only while its quarry is there to
		 * attack. The range isn't saved, so a loaded hunter gets it back the next time this is called.
		 */
		void keepRange(M mob, boolean hunting) {
			AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);

			if (followRange == null || followRange.hasModifier(range.id()) == hunting) return;

			if (hunting) {
				followRange.addTransientModifier(range);
			} else {
				followRange.removeModifier(range.id());
			}
		}

		// Empty for a player who's offline, whose deaths can't be read.
		private static OptionalInt deathsNow(@Nullable Player player) {
			return player instanceof ServerPlayer online ? OptionalInt.of(deaths(online)) : OptionalInt.empty();
		}
	}
}

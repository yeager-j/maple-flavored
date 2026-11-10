package mapleflavored.client.subspaces;

import java.util.HashSet;
import java.util.Set;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dim light inside a Wither's Subspace: every block and entity inside is drawn at the light level its game rule
 * sets, from the sky and from blocks alike, so the sphere shows from inside and out. Blocks and entities that glow on
 * their own still glow. It changes only what is drawn; the server's light, and so mob spawning, stays vanilla.
 *
 * <p>The chunk mesher asks for light from its own threads, where a level's Subspaces can't be read safely while the
 * client thread updates them. So each client tick publishes what is dim as one unchanging {@link Dimmed}, and everything
 * drawn reads that.
 */
public final class WitherSubspaceLight {
	private static volatile Dimmed dimmed = Dimmed.NOTHING;

	private WitherSubspaceLight() {
	}

	/**
	 * Redraws the blocks a Wither's Subspace covers as it opens or ends, and every one when the light level changes. Call
	 * once from client init.
	 */
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> publish(client.level));
	}

	/**
	 * The packed light to draw at the position: the Subspace's dim light inside a Wither's Subspace, else {@code light}.
	 * Safe on any thread.
	 */
	public static int packedLight(BlockPos position, int light) {
		Dimmed drawn = dimmed;

		return drawn.contains(position) ? LightCoordsUtil.pack(drawn.lightLevel(), drawn.lightLevel()) : light;
	}

	/**
	 * The light level of one layer to draw at the position: the Subspace's inside a Wither's Subspace, else
	 * {@code light}. Safe on any thread.
	 */
	public static int lightLevel(BlockPos position, int light) {
		Dimmed drawn = dimmed;

		return drawn.contains(position) ? drawn.lightLevel() : light;
	}

	// A section's mesh bakes its light in, so the sections whose light changed are rebuilt. What is dim is published
	// first, so the rebuilt meshes read it.
	private static void publish(@Nullable ClientLevel level) {
		Dimmed previous = dimmed;
		Dimmed current = level == null ? Dimmed.NOTHING : Dimmed.in(level);

		if (current.equals(previous)) return;

		dimmed = current;

		if (level != null) current.changedSince(previous).forEach(subspace -> redraw(level, subspace));
	}

	private static void redraw(ClientLevel level, Subspace subspace) {
		Vec3 centre = subspace.centre();
		// A block's faces take their light from the blocks beside them, so sections just outside the sphere change too.
		double radius = subspace.radius() + 1.0;

		level.setSectionRangeDirty(
			SectionPos.blockToSectionCoord(centre.x - radius),
			SectionPos.blockToSectionCoord(centre.y - radius),
			SectionPos.blockToSectionCoord(centre.z - radius),
			SectionPos.blockToSectionCoord(centre.x + radius),
			SectionPos.blockToSectionCoord(centre.y + radius),
			SectionPos.blockToSectionCoord(centre.z + radius)
		);
	}

	/** The Withers' Subspaces in the level being drawn and the light level drawn inside them, as of one client tick. */
	private record Dimmed(@Nullable ClientLevel level, Set<Subspace> subspaces, int lightLevel) {
		static final Dimmed NOTHING = new Dimmed(null, Set.of(), -1);

		// Reads the level's attachments, so only the client thread may call this.
		static Dimmed in(ClientLevel level) {
			return new Dimmed(level, Set.copyOf(Subspaces.inLevel(level, Subspace.Kind.WITHER)), WitherSubspaces.lightLevel(level));
		}

		boolean contains(BlockPos position) {
			if (subspaces.isEmpty()) return false;

			Vec3 centre = Vec3.atCenterOf(position);

			for (Subspace subspace : subspaces) {
				if (subspace.contains(centre)) return true;
			}

			return false;
		}

		/**
		 * The Subspaces whose light is drawn differently than under {@code previous}: those that opened or ended, or every
		 * one of both when the light level or the level itself changed. A new level may have meshed sections while the
		 * last level's Subspaces were still published.
		 */
		Set<Subspace> changedSince(Dimmed previous) {
			Set<Subspace> changed = new HashSet<>(subspaces);

			changed.addAll(previous.subspaces);

			if (level == previous.level && lightLevel == previous.lightLevel) {
				changed.removeIf(subspace -> subspaces.contains(subspace) && previous.subspaces.contains(subspace));
			}

			return changed;
		}
	}
}

package mapleflavored.client.subspaces;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dim light inside a Wither's Subspace: every block and entity inside is drawn at light level 3, from the sky and
 * from blocks alike, so the sphere shows from inside and out. Blocks and entities that glow on their own still glow. It
 * changes only what is drawn; the server's light, and so mob spawning, stays vanilla.
 */
public final class WitherSubspaceLight {
	// Hand-check value.
	private static final int LIGHT_LEVEL = 3;
	private static final int PACKED_LIGHT = LightCoordsUtil.pack(LIGHT_LEVEL, LIGHT_LEVEL);

	private static @Nullable ClientLevel litLevel;
	private static Set<Subspace> lit = Set.of();

	private WitherSubspaceLight() {
	}

	/** Redraws the blocks a Wither's Subspace covers as it opens or ends. Call once from client init. */
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> redrawChanged(client.level));
	}

	/** The packed light to draw at the position: the Subspace's dim light inside a Wither's Subspace, else {@code light}. */
	public static int packedLight(BlockPos position, int light) {
		return isInside(position) ? PACKED_LIGHT : light;
	}

	/** The light level of one layer to draw at the position: the Subspace's inside a Wither's Subspace, else {@code level}. */
	public static int lightLevel(BlockPos position, int level) {
		return isInside(position) ? LIGHT_LEVEL : level;
	}

	// The chunk mesher calls this from its own threads, for the level being drawn.
	private static boolean isInside(BlockPos position) {
		ClientLevel level = Minecraft.getInstance().level;

		if (level == null) return false;

		List<Subspace> subspaces = Subspaces.inLevel(level);

		if (subspaces.isEmpty()) return false;

		Vec3 centre = Vec3.atCenterOf(position);

		for (Subspace subspace : subspaces) {
			if (subspace.kind() == Subspace.Kind.WITHER && subspace.contains(centre)) return true;
		}

		return false;
	}

	// A section's mesh bakes its light in, so the sections a Subspace covers are rebuilt when it opens or ends. A new
	// level builds every mesh from scratch.
	private static void redrawChanged(@Nullable ClientLevel level) {
		Set<Subspace> current = level == null ? Set.of() : witherSubspaces(level);

		if (level != litLevel) {
			litLevel = level;
			lit = current;

			return;
		}

		if (current.equals(lit)) return;

		openedOrEnded(current, lit).forEach(subspace -> redraw(level, subspace));
		lit = current;
	}

	private static Set<Subspace> witherSubspaces(ClientLevel level) {
		Set<Subspace> withers = new HashSet<>();

		for (Subspace subspace : Subspaces.inLevel(level)) {
			if (subspace.kind() == Subspace.Kind.WITHER) withers.add(subspace);
		}

		return withers;
	}

	// The Subspaces in one set but not both.
	private static Set<Subspace> openedOrEnded(Set<Subspace> current, Set<Subspace> previous) {
		Set<Subspace> changed = new HashSet<>(current);

		changed.addAll(previous);
		changed.removeIf(subspace -> current.contains(subspace) && previous.contains(subspace));

		return changed;
	}

	private static void redraw(ClientLevel level, Subspace subspace) {
		Vec3 centre = subspace.centre();
		double radius = subspace.radius();

		level.setSectionRangeDirty(
			SectionPos.blockToSectionCoord(centre.x - radius),
			SectionPos.blockToSectionCoord(centre.y - radius),
			SectionPos.blockToSectionCoord(centre.z - radius),
			SectionPos.blockToSectionCoord(centre.x + radius),
			SectionPos.blockToSectionCoord(centre.y + radius),
			SectionPos.blockToSectionCoord(centre.z + radius)
		);
	}
}

package mapleflavored.client.subspaces;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mapleflavored.subspaces.Subspace;
import mapleflavored.subspaces.Subspaces;
import mapleflavored.subspaces.WitherSubspaces;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dim light inside a Wither's Subspace: every block and entity inside is drawn at the light level its game rule
 * sets, from the sky and from blocks alike, so the sphere shows from inside and out. Blocks and entities that glow on their own still glow. It
 * changes only what is drawn; the server's light, and so mob spawning, stays vanilla.
 */
public final class WitherSubspaceLight {
	private static @Nullable ClientLevel litLevel;
	private static Set<Subspace> lit = Set.of();
	private static int litLightLevel = -1;

	private WitherSubspaceLight() {
	}

	/**
	 * Redraws the blocks a Wither's Subspace covers as it opens or ends, and every one when the light level changes. Call
	 * once from client init.
	 */
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> redrawChanged(client.level));
	}

	/** The packed light to draw at the position: the Subspace's dim light inside a Wither's Subspace, else {@code light}. */
	public static int packedLight(BlockPos position, int light) {
		ClientLevel level = Minecraft.getInstance().level;

		if (level == null || !isInside(level, position)) return light;

		int dim = WitherSubspaces.lightLevel(level);

		return LightCoordsUtil.pack(dim, dim);
	}

	/** The light level of one layer to draw at the position: the Subspace's inside a Wither's Subspace, else {@code level}. */
	public static int lightLevel(BlockPos position, int light) {
		ClientLevel level = Minecraft.getInstance().level;

		return level != null && isInside(level, position) ? WitherSubspaces.lightLevel(level) : light;
	}

	// The chunk mesher calls this from its own threads, for the level being drawn.
	private static boolean isInside(ClientLevel level, BlockPos position) {
		List<Subspace> subspaces = Subspaces.inLevel(level);

		if (subspaces.isEmpty()) return false;

		Vec3 centre = Vec3.atCenterOf(position);

		for (Subspace subspace : subspaces) {
			if (subspace.kind() == Subspace.Kind.WITHER && subspace.contains(centre)) return true;
		}

		return false;
	}

	// A section's mesh bakes its light in, so the sections a Subspace covers are rebuilt when it opens or ends, and every
	// Subspace's when the light level changes. A new level builds every mesh from scratch.
	private static void redrawChanged(@Nullable ClientLevel level) {
		Set<Subspace> current = level == null ? Set.of() : witherSubspaces(level);
		int lightLevel = level == null ? -1 : WitherSubspaces.lightLevel(level);
		boolean isSameLevel = level == litLevel;
		Set<Subspace> changed = lightLevel == litLightLevel ? openedOrEnded(current, lit) : everyOne(current, lit);

		litLevel = level;
		lit = current;
		litLightLevel = lightLevel;

		if (isSameLevel) changed.forEach(subspace -> redraw(level, subspace));
	}

	private static Set<Subspace> witherSubspaces(ClientLevel level) {
		Set<Subspace> withers = new HashSet<>();

		for (Subspace subspace : Subspaces.inLevel(level)) {
			if (subspace.kind() == Subspace.Kind.WITHER) withers.add(subspace);
		}

		return withers;
	}

	private static Set<Subspace> everyOne(Set<Subspace> current, Set<Subspace> previous) {
		Set<Subspace> every = new HashSet<>(current);

		every.addAll(previous);

		return every;
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
}

package mapleflavored.bloodmoons;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Opens Restoring's package to the GameTests, which live in another package: a level's spots, how they're saved, and a
 * Blood Moon that rises over only some of them. It sits in the tests' source set, so none of this is in the mod.
 */
public final class RestoringAccess {
	private RestoringAccess() {
	}

	/** The spot the entity holds, if it holds one. */
	public static Optional<RestoreSpot> spotOf(MinecraftServer server, Entity entity) {
		return Restoring.spotOf(server, entity);
	}

	/** Every spot in the level, in the order they were recorded. */
	public static List<RestoreSpot> spots(ServerLevel level) {
		return RestoreSpots.of(level).all();
	}

	/** The level's spots as it would save them now, in the order of {@link #spots}. */
	public static ListTag saved(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		return (ListTag) RestoreSpots.TYPE.codec().encodeStart(ops, RestoreSpots.of(level)).getOrThrow();
	}

	/**
	 * Loads saved spots in place of the level's, the way the game reads a level's saved data as it starts: what can be
	 * read is kept, and saved data that can't be read at all starts empty.
	 */
	public static void load(ServerLevel level, ListTag saved) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Optional<RestoreSpots> read = RestoreSpots.TYPE.codec().parse(ops, saved).resultOrPartial(error -> {});

		level.getDataStorage().set(RestoreSpots.TYPE, read.orElseGet(RestoreSpots.TYPE.constructor()));
	}

	/**
	 * A Blood Moon that rises over only the level's spots that are {@code under} it, so a test's Blood Moon leaves every
	 * other test's spots alone.
	 */
	public static void riseBloodMoonOver(ServerLevel level, Predicate<RestoreSpot> under) {
		Restoring.makeEmptySpotsPending(RestoreSpots.of(level), under);
	}
}

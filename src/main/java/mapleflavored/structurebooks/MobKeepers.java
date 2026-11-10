package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.mobs.GoneForGood;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

/**
 * A structure's keeper mobs, whose deaths unlock its book: a Bastion's keeper Piglin Brutes and a Stronghold's Evokers.
 * Each is tagged as its piece places it, and counted once it's {@linkplain GoneForGood gone for good}, however it
 * goes: killed by anything, converted, removed as Peaceful removes monsters, or dropped as its save fails to load. A
 * keeper that only unloads or changes dimension still counts as alive.
 *
 * <p>Code that spawns a keeper itself asks {@link #keeperPlacedBy} which keeper its piece places and tags the mob with
 * the answer. A keeper the game spawns from a template, as a Bastion's Brutes are, is tagged through {@link #build}.
 */
public final class MobKeepers {
	private static final AttachmentType<Keeper> KEEPER = AttachmentRegistry.create(
		MapleFlavored.id("keeper"),
		builder -> builder.persistent(Keeper.CODEC)
	);

	// Set on a worldgen thread while a keeper's piece is built, for a keeper the game spawns from it to pick up.
	private static final ThreadLocal<@Nullable Keeper> PLACING = new ThreadLocal<>();

	private MobKeepers() {
	}

	static void register() {
		GoneForGood.EVENT.register(MobKeepers::count);
	}

	/**
	 * Builds a structure's piece, so a keeper the game spawns from it is tagged by {@link #tagIfPlacing} when the piece
	 * places one of the book's. Call around every piece's build.
	 *
	 * @param pieces the structure's pieces
	 * @param dimension where the structure is being built
	 */
	public static void build(StructurePiece piece, List<StructurePiece> pieces, ResourceKey<Level> dimension, Runnable buildPiece) {
		Optional<Keeper> keeper = keeperPlacedBy(piece, pieces, dimension);

		if (keeper.isEmpty()) {
			buildPiece.run();

			return;
		}

		PLACING.set(keeper.get());

		try {
			buildPiece.run();
		} finally {
			PLACING.remove();
		}
	}

	/** Tags a keeper the game is spawning from the piece being built, if that piece places one. */
	public static void tagIfPlacing(Mob keeper) {
		Keeper placing = PLACING.get();

		if (placing != null) placing.tag(keeper);
	}

	/**
	 * The keeper the piece places, if it places one of its structure's book's keeper mobs.
	 *
	 * @param pieces the structure's pieces
	 * @param dimension where the structure is being built
	 */
	public static Optional<Keeper> keeperPlacedBy(StructurePiece piece, List<StructurePiece> pieces, ResourceKey<Level> dimension) {
		return StructureBookPiece.among(pieces).flatMap(book -> book.keeperMobPlacedBy(piece)
			.map(key -> new Keeper(GlobalPos.of(dimension, book.plannedSpot()), key))
		);
	}

	/**
	 * The key of a keeper mob that this mod spawns in a room of its structure, as a Stronghold's Evokers are: the room's
	 * origin, the lowest corner of its box.
	 */
	public static BlockPos roomKey(StructurePiece room) {
		BoundingBox box = room.getBoundingBox();

		return new BlockPos(box.minX(), box.minY(), box.minZ());
	}

	/** The loaded keepers of the book at {@code book} in its level that are still alive there. */
	public static List<Mob> liveKeepers(ServerLevel level, BlockPos book) {
		GlobalPos bookPos = GlobalPos.of(level.dimension(), book);

		List<? extends Mob> keepers = level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> {
			Keeper keeper = mob.getAttached(KEEPER);

			return keeper != null && keeper.book().equals(bookPos) && mob.isAlive();
		});

		return List.copyOf(keepers);
	}

	/** The key in its book's lock of the keeper, if the mob is one. */
	public static Optional<BlockPos> keyOf(Mob mob) {
		return Optional.ofNullable(mob.getAttached(KEEPER)).map(Keeper::key);
	}

	/** The loaded keepers of the book at {@code book} in its level that are still alive there, each by its key. */
	static Map<BlockPos, Mob> liveKeepersByKey(ServerLevel level, BlockPos book) {
		Map<BlockPos, Mob> liveByKey = new HashMap<>();

		for (Mob keeper : liveKeepers(level, book)) {
			keyOf(keeper).ifPresent(key -> liveByKey.put(key, keeper));
		}

		return liveByKey;
	}

	/**
	 * How the keeper with this key in the lock is, and where it was last known to be. Only sees a keeper that's loaded in
	 * the book's level.
	 *
	 * @param lock the lock the key is one of
	 */
	public static Sighting sighting(ServerLevel level, BookLocks.Lock lock, BlockPos key) {
		if (lock.done().contains(key)) return new Sighting(Status.GONE, key);

		Mob keeper = liveKeepersByKey(level, lock.book()).get(key);

		return keeper == null ? new Sighting(Status.UNLOADED, key) : new Sighting(Status.ALIVE, keeper.blockPosition());
	}

	// Counting a keeper twice changes nothing.
	private static void count(MinecraftServer server, GoneForGood.Gone gone) {
		gone.attached(KEEPER).ifPresent(keeper -> {
			ServerLevel bookLevel = server.getLevel(keeper.book().dimension());

			if (bookLevel != null) BookLocks.markKeeperGone(bookLevel, keeper.book().pos(), keeper.key());
		});
	}

	/** How a keeper mob is. */
	public enum Status {
		/** Loaded and alive in its book's level. */
		ALIVE,
		/** Its lock has counted it gone for good. */
		GONE,
		/** Not counted gone and not loaded in its book's level: unloaded, in another dimension, or never spawned. */
		UNLOADED
	}

	/**
	 * One keeper mob, as it is now.
	 *
	 * @param at where it is, or its key, which is where its piece placed it, when it isn't loaded
	 */
	public record Sighting(Status status, BlockPos at) {
	}

	/**
	 * A keeper's place in its book's lock. Saved with the keeper.
	 *
	 * @param book where its book was planned
	 * @param key its key in the book's lock, which the piece that places it gives it
	 */
	public record Keeper(GlobalPos book, BlockPos key) {
		static final Codec<Keeper> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("book").forGetter(Keeper::book),
			BlockPos.CODEC.fieldOf("key").forGetter(Keeper::key)
		).apply(instance, Keeper::new));

		/** Makes the mob this keeper, so its book unlocks this key once the mob is gone for good. */
		public void tag(Mob mob) {
			mob.setAttached(KEEPER, this);
		}

		/**
		 * Unlocks this key because its piece couldn't place the keeper, as where a cave left it no floor, so the book can
		 * still be taken. Safe to call from any thread.
		 */
		public void forgive() {
			BookLocks.forgiveKeeper(book, key);
		}
	}
}

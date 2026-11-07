package mapleflavored.structurebooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

/**
 * A structure's keeper mobs, whose deaths unlock its book: a Bastion's keeper Piglin Brutes and a Stronghold's Evokers.
 * Each is tagged as its piece places it, and counted once it's gone for good, however it goes: killed by anything,
 * converted, or removed as Peaceful removes monsters. A keeper that only unloads or changes dimension still counts as
 * alive.
 */
public final class MobKeepers {
	private static final AttachmentType<Keeper> KEEPER = AttachmentRegistry.create(
		MapleFlavored.id("keeper"),
		builder -> builder.persistent(Keeper.CODEC)
	);

	// Set on a worldgen thread while a keeper's piece is built, for the keeper it places to pick up.
	private static final ThreadLocal<@Nullable Keeper> PLACING = new ThreadLocal<>();

	private MobKeepers() {
	}

	static void register() {
		// A dying keeper counts the moment it dies; it's only removed once its death animation ends.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) count(entity, level);
		});
		ServerEntityEvents.ENTITY_UNLOAD.register(MobKeepers::countIfGone);
	}

	/**
	 * Builds a structure's piece, so the keeper it places is tagged when the piece places one of the book's. Call around
	 * every piece's build.
	 *
	 * @param pieces the structure's pieces, its book last if it has one
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

	/** Tags a keeper a structure's piece is placing, if the piece places one. */
	public static void tagIfPlacing(Mob keeper) {
		Keeper placing = PLACING.get();

		if (placing != null) keeper.setAttached(KEEPER, placing);
	}

	/**
	 * Unlocks the key of the keeper the piece being built couldn't place, as where a cave left it no floor, so the book
	 * can still be taken. Safe to call from any thread.
	 */
	public static void forgiveIfPlacing() {
		Keeper placing = PLACING.get();

		if (placing != null) BookLocks.forgiveKeeper(placing.book(), placing.key());
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

	private static Optional<Keeper> keeperPlacedBy(StructurePiece piece, List<StructurePiece> pieces, ResourceKey<Level> dimension) {
		if (pieces.isEmpty() || !(pieces.getLast() instanceof StructureBookPiece book)) return Optional.empty();

		return StructureBookSpots.keeperKey(piece, book.keepers()).map(key -> new Keeper(GlobalPos.of(dimension, book.spot()), key));
	}

	// Removed without dying, as by converting or Peaceful's removal of monsters. Counting a keeper twice changes nothing.
	private static void countIfGone(Entity entity, ServerLevel level) {
		Entity.RemovalReason reason = entity.getRemovalReason();

		if (reason != null && reason.shouldDestroy()) count(entity, level);
	}

	private static void count(Entity entity, ServerLevel level) {
		Keeper keeper = entity.getAttached(KEEPER);

		if (keeper == null) return;

		ServerLevel bookLevel = level.getServer().getLevel(keeper.book().dimension());

		if (bookLevel != null) BookLocks.markKeeperGone(bookLevel, keeper.book().pos(), keeper.key());
	}

	/**
	 * A keeper's place in its book's lock. Saved with the keeper.
	 *
	 * @param book where its book is
	 * @param key its key in the book's lock: the origin of the piece that placed it
	 */
	private record Keeper(GlobalPos book, BlockPos key) {
		static final Codec<Keeper> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("book").forGetter(Keeper::book),
			BlockPos.CODEC.fieldOf("key").forGetter(Keeper::key)
		).apply(instance, Keeper::new));
	}
}

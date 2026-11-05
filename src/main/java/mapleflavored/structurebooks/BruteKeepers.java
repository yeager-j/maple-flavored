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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

/**
 * A Bastion's keepers: the Piglin Brutes whose deaths unlock its book. Each is tagged as its template places it, and
 * counted once it's gone for good, however it goes: killed by anything, converted, or removed as Peaceful removes
 * monsters. A keeper that only unloads or changes dimension still counts as alive.
 */
public final class BruteKeepers {
	private static final AttachmentType<Keeper> KEEPER = AttachmentRegistry.create(
		MapleFlavored.id("keeper"),
		builder -> builder.persistent(Keeper.CODEC)
	);

	// Set on a worldgen thread while a keeper's piece is built, for the Brute it places to pick up.
	private static final ThreadLocal<@Nullable Keeper> PLACING = new ThreadLocal<>();

	private BruteKeepers() {
	}

	static void register() {
		// A dying keeper counts the moment it dies; it's only removed once its death animation ends.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) count(entity, level);
		});
		ServerEntityEvents.ENTITY_UNLOAD.register(BruteKeepers::countIfGone);
	}

	/**
	 * Builds a structure's piece, tagging the Brute it places as a keeper when it's one of the book's. Call around every
	 * piece's build.
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

	/** Tags a Brute a structure template is placing as a keeper, if its piece places one. */
	public static void tagIfPlacing(PiglinBrute brute) {
		Keeper keeper = PLACING.get();

		if (keeper != null) brute.setAttached(KEEPER, keeper);
	}

	/** The loaded keepers of the book at {@code book} in its level that are still alive there. */
	public static List<PiglinBrute> liveKeepers(ServerLevel level, BlockPos book) {
		GlobalPos bookPos = GlobalPos.of(level.dimension(), book);

		List<? extends PiglinBrute> keepers = level.getEntities(EntityTypes.PIGLIN_BRUTE, brute -> {
			Keeper keeper = brute.getAttached(KEEPER);

			return keeper != null && keeper.book().equals(bookPos) && brute.isAlive();
		});

		return List.copyOf(keepers);
	}

	/** The key in its book's lock of the keeper, if the Brute is one. */
	public static Optional<BlockPos> keyOf(PiglinBrute brute) {
		return Optional.ofNullable(brute.getAttached(KEEPER)).map(Keeper::key);
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
	 * A keeper Brute's place in its book's lock. Saved with the Brute.
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

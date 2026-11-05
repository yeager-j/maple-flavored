package mapleflavored.subspaces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Shriekers grown by a Warden Subspace's sculk. Each can summon while the Subspace that grew it lasts, and is silenced
 * when that Subspace ends, so a city never stays more dangerous after a fight. A shrieker unloaded at the end is
 * silenced the next time it loads.
 */
public final class GrownShriekers {
	/** The id of the Subspace that grew the shrieker. */
	private static final AttachmentType<UUID> GROWN_IN = AttachmentRegistry.create(
		MapleFlavored.id("grown_in_subspace"),
		builder -> builder.persistent(UUIDUtil.CODEC)
	);

	/**
	 * Where shriekers loaded whose Subspace has ended, waiting for their chunk to finish loading: a chunk can't take block
	 * changes until it has.
	 */
	private static final AttachmentType<Set<BlockPos>> LOADED_STALE = AttachmentRegistry.create(
		MapleFlavored.id("loaded_stale_shriekers"),
		builder -> builder.initializer(HashSet::new)
	);

	private GrownShriekers() {
	}

	static void register() {
		Subspaces.ENDED.register(GrownShriekers::silenceLoaded);
		// A chunk can stop being loaded and load again while it's still held in memory, which neither re-adds its block
		// entities nor counts as a chunk load, so this watches chunks becoming accessible.
		ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.register((level, chunk, from, to) -> {
			boolean wasLoaded = from.isOrAfter(FullChunkStatus.FULL);
			boolean isLoaded = to.isOrAfter(FullChunkStatus.FULL);

			if (isLoaded && !wasLoaded) noteStale(level, chunk);
			if (wasLoaded && !isLoaded) forgetStale(level, chunk);
		});
		ServerTickEvents.END_LEVEL_TICK.register(GrownShriekers::silenceLoadedStale);
	}

	/**
	 * Places a sculk growth with {@code place}, vanilla's call. A shrieker grown by a Subspace's sculk can summon and
	 * records that Subspace; every other growth is placed as vanilla places it.
	 */
	public static boolean placeGrowth(
		LevelAccessor level,
		BlockPos position,
		BlockState growth,
		SculkSpreader spreader,
		BiPredicate<BlockPos, BlockState> place
	) {
		if (!(spreader instanceof SubspaceSculkSpreader seed) || !growth.is(Blocks.SCULK_SHRIEKER)) return place.test(position, growth);

		boolean placed = place.test(position, growth.setValue(SculkShriekerBlock.CAN_SUMMON, true));

		if (placed && level.getBlockEntity(position) instanceof SculkShriekerBlockEntity shrieker) {
			shrieker.setAttached(GROWN_IN, seed.subspace().id());
		}

		return placed;
	}

	// The Subspace grew shriekers only inside itself, so only the chunks it covers can hold them.
	private static void silenceLoaded(ServerLevel level, Subspace subspace) {
		List<SculkShriekerBlockEntity> grown = new ArrayList<>();
		Vec3 centre = subspace.centre();
		int minX = SectionPos.blockToSectionCoord(centre.x - subspace.radius());
		int maxX = SectionPos.blockToSectionCoord(centre.x + subspace.radius());
		int minZ = SectionPos.blockToSectionCoord(centre.z - subspace.radius());
		int maxZ = SectionPos.blockToSectionCoord(centre.z + subspace.radius());

		for (int chunkX = minX; chunkX <= maxX; chunkX++) {
			for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

				if (chunk != null) grown.addAll(grownBy(chunk, subspace.id()));
			}
		}

		grown.forEach(shrieker -> silence(level, shrieker));
	}

	private static List<SculkShriekerBlockEntity> grownBy(LevelChunk chunk, UUID subspace) {
		List<SculkShriekerBlockEntity> grown = new ArrayList<>();

		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			if (blockEntity instanceof SculkShriekerBlockEntity shrieker && subspace.equals(shrieker.getAttached(GROWN_IN))) {
				grown.add(shrieker);
			}
		}

		return grown;
	}

	private static void noteStale(ServerLevel level, LevelChunk chunk) {
		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			if (blockEntity instanceof SculkShriekerBlockEntity shrieker && isStale(level, shrieker)) {
				level.getAttachedOrCreate(LOADED_STALE).add(shrieker.getBlockPos());
			}
		}
	}

	private static void forgetStale(ServerLevel level, LevelChunk chunk) {
		Set<BlockPos> stale = level.getAttached(LOADED_STALE);

		if (stale != null) stale.removeIf(position -> chunk.getPos().equals(ChunkPos.containing(position)));
	}

	private static void silenceLoadedStale(ServerLevel level) {
		Set<BlockPos> stale = level.getAttached(LOADED_STALE);

		if (stale == null || stale.isEmpty()) return;

		for (Iterator<BlockPos> iterator = stale.iterator(); iterator.hasNext(); ) {
			BlockPos position = iterator.next();

			if (!isChunkLoaded(level, position)) continue;

			iterator.remove();

			if (level.getBlockEntity(position) instanceof SculkShriekerBlockEntity shrieker && isStale(level, shrieker)) {
				silence(level, shrieker);
			}
		}
	}

	private static boolean isChunkLoaded(ServerLevel level, BlockPos position) {
		int chunkX = SectionPos.blockToSectionCoord(position.getX());
		int chunkZ = SectionPos.blockToSectionCoord(position.getZ());

		return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
	}

	private static boolean isStale(ServerLevel level, SculkShriekerBlockEntity shrieker) {
		UUID grownIn = shrieker.getAttached(GROWN_IN);

		return grownIn != null && !Subspaces.isActive(level, grownIn);
	}

	private static void silence(ServerLevel level, SculkShriekerBlockEntity shrieker) {
		BlockPos position = shrieker.getBlockPos();
		BlockState state = level.getBlockState(position);

		if (state.is(Blocks.SCULK_SHRIEKER)) level.setBlock(position, state.setValue(SculkShriekerBlock.CAN_SUMMON, false), Block.UPDATE_ALL);

		shrieker.removeAttached(GROWN_IN);
	}
}

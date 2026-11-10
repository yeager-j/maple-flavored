package mapleflavored.spawners;

import java.util.List;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.access.BaseSpawnerAccessor;
import mapleflavored.advancements.MapleTriggers;
import mapleflavored.effects.CreakingTrails;
import mapleflavored.items.MapleItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Spawners module: normal spawners ignore light, and can't be broken or blown up until they are Cleared by the
 * deaths of their own mobs. Until then they spawn twice as fast and glow hot, cooling a little with each of those
 * deaths. Breaking a Cleared spawner gives 1 Pith, and the extra drop it holds, if any.
 *
 * <p>Trial spawners and spawner minecarts are left vanilla. Each spawner's state is a {@link SpawnerClearing} on its
 * block entity, and clients see a {@link SpawnerView} of it.
 */
public final class Spawners {
	private static final ResourceKey<LootTable> SPAWNER_LOOT = Blocks.SPAWNER.getLootTable().orElseThrow();

	// Where the mob was spawned from, kept through saves and portals so its death finds its spawner.
	private static final AttachmentType<GlobalPos> SPAWNED_BY = AttachmentRegistry.create(
		MapleFlavored.id("spawned_by"),
		builder -> builder.persistent(GlobalPos.CODEC)
	);

	// Trails to and from a spawner run in the orange of its glow.
	private static final int TRAIL_COLOR = 0xFF8A1C;
	// How far from a spawner its living mobs are pointed out.
	private static final double TRAIL_RANGE = 32.0;

	/** Fires once per spawner, on the death that Clears it. The Fortress lock listens here. */
	public static final Event<Cleared> CLEARED = EventFactory.createArrayBacked(Cleared.class, listeners -> (level, pos) -> {
		for (Cleared listener : listeners) {
			listener.onCleared(level, pos);
		}
	});

	private Spawners() {
	}

	/** Registers the death counting and the Cleared drops. Call once from mod init. */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> countDeath(entity));
		ServerLivingEntityEvents.MOB_CONVERSION.register(Spawners::keepSpawnerThroughConversion);
		LootTableEvents.MODIFY_DROPS.register(Spawners::addClearedDrops);
	}

	/** Whether the block at {@code pos} is a Cleared spawner. Works on the client too, from the view it was sent. */
	public static boolean isCleared(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ClearableSpawner spawner && spawner.maple_flavored$view().cleared();
	}

	/**
	 * The Clearing of the spawner at {@code pos}, or empty when there's no normal spawner there. Server only: a client's
	 * copy is never sent it and stays unstarted, so client code asks {@link #isCleared} or reads the spawner's view.
	 */
	public static Optional<SpawnerClearing> clearing(BlockGetter level, BlockPos pos) {
		return clearing(level.getBlockEntity(pos));
	}

	/** Makes the spawner drop a copy of {@code drop} with its Pith, once Cleared and broken. */
	public static void holdDrop(SpawnerBlockEntity spawner, ItemStack drop) {
		clearingOf(spawner).holdDrop(drop);
		spawner.setChanged();
	}

	/**
	 * Marks {@code entity} and its passengers as spawned by the spawner at {@code pos}, so their deaths count toward
	 * Clearing it. Only a spawner block's own {@code spawner} counts; a spawner minecart's mobs are left unmarked.
	 */
	public static void markSpawned(ServerLevel level, BlockPos pos, BaseSpawner spawner, Entity entity) {
		if (blockClearing(level, pos, spawner).isEmpty()) return;

		GlobalPos origin = GlobalPos.of(level.dimension(), pos.immutable());

		entity.getSelfAndPassengers().forEach(spawned -> spawned.setAttached(SPAWNED_BY, origin));
	}

	/**
	 * The wait before the next wave of the spawner at {@code pos}, given the wait vanilla chose: halved while a spawner
	 * block isn't Cleared. A spawner minecart keeps vanilla's wait.
	 */
	public static int spawnDelay(Level level, BlockPos pos, BaseSpawner spawner, int vanillaDelay) {
		return blockClearing(level, pos, spawner).map(clearing -> clearing.spawnDelay(vanillaDelay)).orElse(vanillaDelay);
	}

	/**
	 * Whether the spawner at {@code pos} spawns its mobs whatever the light: a spawner block does, and a spawner
	 * minecart keeps vanilla's light rules.
	 */
	public static boolean ignoresLight(Level level, BlockPos pos, BaseSpawner spawner) {
		return blockClearing(level, pos, spawner).isPresent();
	}

	/** Where the spawner block that spawned {@code entity} is, if one did. */
	public static Optional<GlobalPos> spawnedBy(Entity entity) {
		return Optional.ofNullable(entity.getAttached(SPAWNED_BY));
	}

	/** Records every player now in the spawner's activation range. Call each server tick of the spawner. */
	public static void recordPlayersInRange(ServerLevel level, BlockPos pos, SpawnerBlockEntity spawner) {
		SpawnerClearing clearing = clearingOf(spawner);

		if (clearing.isCleared()) return;

		Vec3 centre = Vec3.atCenterOf(pos);
		double range = activationRange(spawner);
		boolean anyNewPlayer = false;

		for (ServerPlayer player : level.players()) {
			if (isInRange(player, centre, range)) anyNewPlayer |= clearing.recordPlayer(player.getUUID());
		}

		if (anyNewPlayer) sendChange(level, pos, spawner);
	}

	/**
	 * Answers a hit on a spawner that isn't Cleared: it flares up, and trails lead from it to each of its mobs alive
	 * nearby.
	 */
	public static void refuseHit(ServerLevel level, BlockPos pos) {
		GlobalPos spawner = GlobalPos.of(level.dimension(), pos);
		AABB nearby = new AABB(pos).inflate(TRAIL_RANGE);

		level.levelEvent(LevelEvent.PARTICLES_MOBBLOCK_SPAWN, pos, 0);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

		for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, nearby, entity -> isLivingMobOf(entity, spawner))) {
			CreakingTrails.show(level, pos, mob.blockPosition(), TRAIL_COLOR);
		}
	}

	/** Fires the advancement for a player who broke a Cleared spawner and so got its Pith. */
	public static void onBrokenByPlayer(ServerPlayer player, @Nullable BlockEntity blockEntity) {
		if (clearing(blockEntity).map(SpawnerClearing::isCleared).orElse(false)) {
			MapleTriggers.CLEARED_SPAWNER_BROKEN.trigger(player);
		}
	}

	/** The spawner's activation range, which is also the range that adds players to its requirement. */
	private static double activationRange(SpawnerBlockEntity spawner) {
		return ((BaseSpawnerAccessor) spawner.getSpawner()).getRequiredPlayerRange();
	}

	/** Whether the player activates a spawner whose centre is {@code centre}, as vanilla decides it. */
	private static boolean isInRange(Player player, Vec3 centre, double range) {
		return !player.isSpectator() && player.isAlive() && player.distanceToSqr(centre) < range * range;
	}

	private static void countDeath(LivingEntity entity) {
		Optional<GlobalPos> spawnedBy = spawnedBy(entity);

		if (spawnedBy.isEmpty() || !(entity.level() instanceof ServerLevel deathLevel)) return;

		GlobalPos origin = spawnedBy.get();
		// The spawner's chunk may be unloaded when its mob died far away; getBlockEntity loads it.
		ServerLevel level = deathLevel.getServer().getLevel(origin.dimension());

		if (level == null || !(level.getBlockEntity(origin.pos()) instanceof SpawnerBlockEntity spawner)) return;

		SpawnerClearing clearing = clearingOf(spawner);

		if (clearing.isCleared()) return;

		clearing.recordDeath();
		sendChange(level, origin.pos(), spawner);
		showDeathCounted(level, origin.pos(), entity);

		if (clearing.isCleared()) CLEARED.invoker().onCleared(level, origin.pos());
	}

	// A trail runs from where the mob died to its spawner, which hisses and steams as a patch of it cools.
	private static void showDeathCounted(ServerLevel level, BlockPos spawner, LivingEntity dead) {
		if (dead.level() == level) CreakingTrails.show(level, dead.blockPosition(), spawner, TRAIL_COLOR);

		level.levelEvent(LevelEvent.LAVA_FIZZ, spawner, 0);
	}

	private static boolean isLivingMobOf(LivingEntity entity, GlobalPos spawner) {
		return entity.isAlive() && spawnedBy(entity).filter(spawner::equals).isPresent();
	}

	// A zombie that drowns is still its spawner's mob, so its death as a drowned counts. A slime's split-off children
	// are new mobs and don't.
	private static void keepSpawnerThroughConversion(Mob previous, Mob converted, ConversionParams conversion) {
		if (conversion.type() != ConversionType.SINGLE) return;

		spawnedBy(previous).ifPresent(origin -> converted.setAttached(SPAWNED_BY, origin));
	}

	private static void addClearedDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		if (!table.is(SPAWNER_LOOT)) return;

		Optional<SpawnerClearing> clearing = clearing(context.getOptional(LootContextParams.BLOCK_ENTITY));

		if (clearing.isEmpty() || !clearing.get().isCleared()) return;

		drops.add(new ItemStack(MapleItems.PITH));
		clearing.get().heldDrop().ifPresent(drops::add);
	}

	// Saves the change and sends it to clients, which draw the glow and predict whether the spawner can be mined.
	private static void sendChange(ServerLevel level, BlockPos pos, SpawnerBlockEntity spawner) {
		BlockState state = spawner.getBlockState();

		spawner.setChanged();
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
	}

	// The Clearing of the spawner block whose own spawner is {@code spawner}; empty for a spawner minecart.
	private static Optional<SpawnerClearing> blockClearing(Level level, BlockPos pos, BaseSpawner spawner) {
		if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity block) || block.getSpawner() != spawner) return Optional.empty();

		return Optional.of(clearingOf(block));
	}

	private static Optional<SpawnerClearing> clearing(@Nullable BlockEntity blockEntity) {
		return blockEntity instanceof ClearableSpawner spawner ? Optional.of(spawner.maple_flavored$clearing()) : Optional.empty();
	}

	private static SpawnerClearing clearingOf(SpawnerBlockEntity spawner) {
		return ((ClearableSpawner) spawner).maple_flavored$clearing();
	}

	/** Listens for a spawner becoming Cleared. */
	@FunctionalInterface
	public interface Cleared {
		/** Called once, after the death that Cleared the spawner at {@code pos}. */
		void onCleared(ServerLevel level, BlockPos pos);
	}
}

package mapleflavored.structurebooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

/**
 * The Structure books module: each structure holds one level I book in a fixed, visible spot, and taking it can wake
 * the structure.
 *
 * <p>{@link StructureBookRules} plans each book's spot when a structure's pieces are planned. The book is saved with
 * the structure as a {@link StructureBookPiece} and placed when that chunk is built.
 */
public final class StructureBooks {
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, MapleFlavored.id("structure_book"));
	private static final int LIGHT = 7;
	private static final float UNBREAKABLE = -1.0F;
	private static final float BLAST_PROOF = 3_600_000.0F;

	// Trails to what still locks a book, drawn like the Creaking heart's in the forcefield's colour.
	private static final int TRAILS_PER_TARGET = 5;
	// As long as a bell makes nearby raiders glow.
	private static final int KEEPER_GLOW_TICKS = 60;
	private static final int TRAIL_COLOR = BorderStatus.STATIONARY.getColor();
	private static final int TRAIL_MIN_TICKS = 10;
	private static final int TRAIL_EXTRA_TICKS = 40;

	/** The structure book block. It has no item. */
	public static final Block BLOCK = Registry.register(
		BuiltInRegistries.BLOCK,
		BLOCK_KEY,
		new StructureBookBlock(
			BlockBehaviour.Properties.of()
				.setId(BLOCK_KEY)
				.strength(UNBREAKABLE, BLAST_PROOF)
				.noLootTable()
				.noCollision()
				.pushReaction(PushReaction.IMMOVEABLE)
				.isValidSpawn(Blocks::never)
				.lightLevel(state -> LIGHT)
				.sound(SoundType.WOOD)
		)
	);

	/** The block entity that holds a structure book's enchantment. */
	public static final BlockEntityType<StructureBookBlockEntity> BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		MapleFlavored.id("structure_book"),
		new BlockEntityType<>(StructureBookBlockEntity::new, Set.of(BLOCK))
	);

	/** The structure piece that saves a planned book's spot and enchantment with its structure. */
	public static final StructurePieceType PIECE = Registry.register(
		BuiltInRegistries.STRUCTURE_PIECE,
		MapleFlavored.id("structure_book"),
		(context, tag) -> new StructureBookPiece(tag)
	);

	/**
	 * Fires once a player has taken a structure book: the book is in their inventory and the block is gone. A
	 * structure that wakes when its book is taken listens here, finding itself from the position.
	 */
	public static final Event<Taken> TAKEN = EventFactory.createArrayBacked(Taken.class, listeners -> (player, level, pos, enchantment) -> {
		for (Taken listener : listeners) {
			listener.onTaken(player, level, pos, enchantment);
		}
	});

	private StructureBooks() {
	}

	/**
	 * Registers the block, block entity, structure piece, book locks, the Bastion's and Woodland Mansion's wake events, the
	 * Ancient City's Grounding and keepers, and {@code /keepers}. Call once from mod init.
	 */
	public static void register() {
		// The block, block entity and piece register in the static fields above; this call makes sure they run.
		BookLocks.register();
		TakenBooks.register();
		PiglinAnger.register();
		IllagerAlert.register();
		MobKeepers.register();
		AncientCityGrounding.register();
		ShriekerKeepers.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> KeepersCommand.register(dispatcher));
	}

	/**
	 * Builds a structure's piece along with the keepers it places: it tags a Bastion's keeper Brutes as they're placed,
	 * and places an Ancient City's keeper shrieker once its sculk patch has grown. Call around every piece's build.
	 *
	 * @param pieces the structure's pieces, its book last if it has one
	 */
	public static void buildPiece(StructurePiece piece, List<StructurePiece> pieces, WorldGenLevel level, Runnable buildPiece) {
		MobKeepers.build(piece, pieces, level.getLevel().dimension(), buildPiece);
		ShriekerKeepers.placeIfKeeper(piece, pieces, level);
	}

	/**
	 * Takes the structure book at {@code pos} for the player, as one action: the player gets its level I book, the
	 * block is removed and {@link #TAKEN} fires. Refused, with nothing changed and no event, when there's no book with an
	 * enchantment there, the book is locked or the player has no free inventory slot. A locked book shows the player
	 * trails leading to the spawners still to be Cleared, the keeper Brutes still alive, which glow, or the keeper
	 * shriekers still standing.
	 *
	 * @return whether the book was taken
	 */
	public static boolean take(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();
		Optional<Holder<Enchantment>> enchantment = level.getBlockEntity(pos) instanceof StructureBookBlockEntity book
			? book.enchantment()
			: Optional.empty();

		if (enchantment.isEmpty()) return false;

		Optional<BookLocks.Lock> lock = BookLocks.lockAt(level, pos).filter(BookLocks.Lock::isLocked);

		if (lock.isPresent()) {
			refuseLocked(player, level, pos, lock.get());

			return false;
		}

		if (player.getInventory().getFreeSlot() == -1) {
			player.sendOverlayMessage(Component.translatable("block.maple_flavored.structure_book.no_room"));

			return false;
		}

		player.getInventory().add(levelOneBook(enchantment.get()));
		level.removeBlock(pos, false);
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
		TAKEN.invoker().onTaken(player, level, pos, enchantment.get());

		return true;
	}

	private static void refuseLocked(ServerPlayer player, ServerLevel level, BlockPos book, BookLocks.Lock lock) {
		List<BlockPos> remaining = lock.remainingKeys();
		int done = lock.keys().size() - remaining.size();
		String message = switch (lock.kind()) {
			case SPAWNERS -> "block.maple_flavored.structure_book.locked";
			case BRUTES -> "block.maple_flavored.structure_book.locked_by_keepers";
			case EVOKERS -> "block.maple_flavored.structure_book.locked_by_evokers";
			case SHRIEKERS -> "block.maple_flavored.structure_book.locked_by_shriekers";
		};

		player.sendOverlayMessage(Component.translatable(message, done, lock.keys().size()));

		switch (lock.kind()) {
			case SPAWNERS, SHRIEKERS -> remaining.forEach(key -> showTrails(level, book, key));
			case BRUTES, EVOKERS -> pointOutKeepers(level, book, remaining);
		}
	}

	// Each remaining keeper here glows and gets trails; one that isn't loaded gets trails to where it spawned.
	private static void pointOutKeepers(ServerLevel level, BlockPos book, List<BlockPos> remaining) {
		Map<BlockPos, Mob> liveByKey = new HashMap<>();

		for (Mob keeper : MobKeepers.liveKeepers(level, book)) {
			MobKeepers.keyOf(keeper).filter(remaining::contains).ifPresent(key -> liveByKey.put(key, keeper));
		}

		for (BlockPos key : remaining) {
			Mob keeper = liveByKey.get(key);

			if (keeper == null) {
				showTrails(level, book, key);

				continue;
			}

			keeper.addEffect(new MobEffectInstance(MobEffects.GLOWING, KEEPER_GLOW_TICKS));
			showTrails(level, book, keeper.blockPosition());
		}
	}

	// Each trail runs from somewhere in the book's block to somewhere in the target's.
	private static void showTrails(ServerLevel level, BlockPos book, BlockPos target) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < TRAILS_PER_TARGET; i++) {
			Vec3 from = withinBlock(book, random);
			Vec3 to = withinBlock(target, random);
			TrailParticleOption trail = new TrailParticleOption(to, TRAIL_COLOR, TRAIL_MIN_TICKS + random.nextInt(TRAIL_EXTRA_TICKS));

			level.sendParticles(trail, true, true, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static Vec3 withinBlock(BlockPos pos, RandomSource random) {
		return Vec3.atLowerCornerOf(pos).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
	}

	/** The enchanted book a structure book gives: {@code enchantment} at level I. */
	public static ItemStack levelOneBook(Holder<Enchantment> enchantment) {
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1));
	}

	/** Listens for a taken structure book. */
	@FunctionalInterface
	public interface Taken {
		/** Called after {@code player} took the book at {@code pos}, which gave {@code enchantment} at level I. */
		void onTaken(ServerPlayer player, ServerLevel level, BlockPos pos, Holder<Enchantment> enchantment);
	}
}

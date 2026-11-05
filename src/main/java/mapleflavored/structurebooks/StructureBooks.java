package mapleflavored.structurebooks;

import java.util.Optional;
import java.util.Set;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.PushReaction;

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

	/** Registers the block, block entity and structure piece. Call once from mod init. */
	public static void register() {
		// Registration happens in the static fields above; this call makes sure they run.
	}

	/**
	 * Takes the structure book at {@code pos} for the player, as one action: the player gets its level I book, the
	 * block is removed and {@link #TAKEN} fires. Refused, with nothing changed and no event, when the player has no free
	 * inventory slot or there's no book with an enchantment there.
	 *
	 * @return whether the book was taken
	 */
	public static boolean take(ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();
		Optional<Holder<Enchantment>> enchantment = level.getBlockEntity(pos) instanceof StructureBookBlockEntity book
			? book.enchantment()
			: Optional.empty();

		if (enchantment.isEmpty()) return false;

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

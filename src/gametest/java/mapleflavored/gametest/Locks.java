package mapleflavored.gametest;

import java.util.ArrayList;
import java.util.List;
import mapleflavored.structurebooks.BookLocks;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Reads a level's book locks the way a client and a save see them, loads them back as a restart would, and runs
 * {@code /keepers}. The only place the tests reach the locks' saved form.
 */
final class Locks {
	/** The line {@code /keepers} writes first: the book, the keepers done and the keepers in all. */
	static final String KEEPERS_BOOK = "commands.maple_flavored.keepers.book";
	/** The line {@code /keepers} writes for each keeper: its number, where it is, how far that is and how it is. */
	static final String KEEPERS_KEEPER = "commands.maple_flavored.keepers.keeper";

	private Locks() {
	}

	/** Whether the book block at {@code book} is sent to clients locked. */
	static boolean sentLocked(ServerLevel level, BlockPos book) {
		return sentRemaining(level, book) > 0.0F;
	}

	/** The share of the book's lock still on, as clients are sent it to draw the book's cage. */
	static float sentRemaining(ServerLevel level, BlockPos book) {
		return level.getBlockEntity(book).getUpdateTag(level.registryAccess()).getFloatOr("lock_remaining", 0.0F);
	}

	/** The books the level's saved locks are for, read from the saved form without recording any planned lock. */
	static List<BlockPos> savedBooks(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		return saved(level).stream().map(lock -> BlockPos.CODEC.parse(ops, ((CompoundTag) lock).get("book")).getOrThrow()).toList();
	}

	/** Saves the level's locks and loads them back in their place, as a restart would. */
	static void reload(ServerLevel level) {
		load(level, saved(level));
	}

	/**
	 * Loads the level's locks back without the book's, as a restart after a crash that saved the book's structure and not
	 * its lock would. Nothing the locks counted for the book is kept.
	 */
	static void forget(ServerLevel level, BlockPos book) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		ListTag saved = saved(level);

		saved.removeIf(lock -> BlockPos.CODEC.parse(ops, ((CompoundTag) lock).get("book")).getOrThrow().equals(book));
		load(level, saved);
	}

	/** The lines {@code /keepers} writes when an operator runs it at the position. */
	static List<Component> keepersAt(ServerLevel level, Vec3 position) {
		List<Component> output = new ArrayList<>();
		CommandSource capture = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) {
				output.add(message);
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return false;
			}
		};
		MinecraftServer server = level.getServer();

		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(capture).withLevel(level).withPosition(position), "keepers");

		return output;
	}

	/** The translation key of a line a command wrote. */
	static String key(Component line) {
		return ((TranslatableContents) line.getContents()).getKey();
	}

	/** What a line a command wrote fills its translation with. */
	static List<Object> args(Component line) {
		return List.of(((TranslatableContents) line.getContents()).getArgs());
	}

	/** The translation key of how a keeper is, from the line {@code /keepers} wrote for it. */
	static String keeperStatus(Component keeperLine) {
		return key((Component) args(keeperLine).get(3));
	}

	// The level's locks as they'd be saved.
	private static ListTag saved(ServerLevel level) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		return (ListTag) BookLocks.TYPE.codec().encodeStart(ops, level.getDataStorage().computeIfAbsent(BookLocks.TYPE)).getOrThrow();
	}

	private static void load(ServerLevel level, ListTag saved) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		level.getDataStorage().set(BookLocks.TYPE, BookLocks.TYPE.codec().parse(ops, saved).getOrThrow());
	}
}

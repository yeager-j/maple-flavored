package mapleflavored.structurebooks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /keepers}, an operator debugging command that finds the keepers of the Ancient City book nearest the source in
 * its dimension. It lists each keeper with its coordinates, which suggest a teleport when clicked, its distance and
 * whether it's standing, broken, in a chunk that isn't loaded, or missing from a loaded chunk.
 */
final class KeepersCommand {
	private static final SimpleCommandExceptionType NO_KEEPERS = new SimpleCommandExceptionType(
		Component.translatable("commands.maple_flavored.keepers.none")
	);

	private KeepersCommand() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("keepers")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> list(c.getSource()))
		);
	}

	private static int list(CommandSourceStack source) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		Vec3 origin = source.getPosition();
		BookLocks.Lock lock = nearestLock(level, origin).orElseThrow(NO_KEEPERS::create);
		List<BlockPos> keys = lock.keys();
		int broken = keys.size() - lock.remainingKeys().size();
		int standing = 0;

		source.sendSuccess(
			() -> Component.translatable("commands.maple_flavored.keepers.book", coordinates(lock.book()), broken, keys.size()),
			false
		);

		for (int i = 0; i < keys.size(); i++) {
			BlockPos key = keys.get(i);
			Optional<BlockPos> spot = lock.done().contains(key) ? Optional.empty() : ShriekerKeepers.standingSpot(level, key);
			BlockPos shown = spot.orElse(key);
			String status = status(level, lock, key, spot);
			int number = i + 1;
			int distance = Mth.floor(Math.sqrt(shown.distToCenterSqr(origin)));

			if (spot.isPresent()) standing++;

			source.sendSuccess(
				() -> Component.translatable("commands.maple_flavored.keepers.keeper", number, coordinates(shown), distance, Component.translatable(status)),
				false
			);
		}

		return standing;
	}

	private static Optional<BookLocks.Lock> nearestLock(ServerLevel level, Vec3 origin) {
		return BookLocks.locksOf(level, BookLocks.Kind.SHRIEKERS).stream()
			.min(Comparator.comparingDouble(lock -> lock.book().distToCenterSqr(origin)));
	}

	private static String status(ServerLevel level, BookLocks.Lock lock, BlockPos key, Optional<BlockPos> spot) {
		if (lock.done().contains(key)) return "commands.maple_flavored.keepers.broken";
		if (spot.isPresent()) return "commands.maple_flavored.keepers.standing";
		// As the keeper's spot is read, without loading the chunk.
		if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(key.getX()), SectionPos.blockToSectionCoord(key.getZ())) == null) {
			return "commands.maple_flavored.keepers.unloaded";
		}

		return "commands.maple_flavored.keepers.missing";
	}

	// As /locate shows what it found: green, suggesting a teleport there when clicked.
	private static Component coordinates(BlockPos pos) {
		return ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ()))
			.withStyle(style -> style
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
			);
	}
}

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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /keepers}, an operator debugging command that finds the keepers of the keeper-locked book nearest the source in
 * its dimension: a Bastion's Brutes, an Ancient City's shriekers or a Stronghold's Evokers. It lists each keeper with
 * its coordinates, which suggest a teleport when clicked, its distance and how it is. A book whose structure found no
 * keepers to plan is reported as that.
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

	// Returns how many keepers are there to be found: standing or alive.
	private static int list(CommandSourceStack source) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		Vec3 origin = source.getPosition();
		BookLocks.Lock lock = nearestLock(level, origin).orElseThrow(NO_KEEPERS::create);
		List<BlockPos> keys = lock.keys();

		if (keys.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.maple_flavored.keepers.no_keepers", coordinates(lock.book())), false);

			return 0;
		}

		List<Line> lines = keys.stream().map(key -> line(level, lock, key)).toList();
		int done = keys.size() - lock.remainingKeys().size();

		source.sendSuccess(
			() -> Component.translatable("commands.maple_flavored.keepers.book", coordinates(lock.book()), done, keys.size()),
			false
		);

		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			int number = i + 1;
			int distance = Mth.floor(Math.sqrt(line.at().distToCenterSqr(origin)));

			source.sendSuccess(
				() -> Component.translatable("commands.maple_flavored.keepers.keeper", number, coordinates(line.at()), distance, Component.translatable(line.status())),
				false
			);
		}

		return (int) lines.stream().filter(Line::isThere).count();
	}

	private static Optional<BookLocks.Lock> nearestLock(ServerLevel level, Vec3 origin) {
		return BookLocks.keeperLocks(level).stream().min(Comparator.comparingDouble(lock -> lock.book().distToCenterSqr(origin)));
	}

	private static Line line(ServerLevel level, BookLocks.Lock lock, BlockPos key) {
		return switch (lock.kind()) {
			case SHRIEKERS -> shriekerLine(ShriekerKeepers.keeper(level, lock, key));
			case BRUTES, EVOKERS -> mobLine(MobKeepers.sighting(level, lock, key));
			case SPAWNERS -> throw new IllegalStateException("a spawner lock has no keepers");
		};
	}

	private static Line shriekerLine(ShriekerKeepers.Keeper keeper) {
		String status = switch (keeper.status()) {
			case STANDING -> "commands.maple_flavored.keepers.standing";
			case BROKEN -> "commands.maple_flavored.keepers.broken";
			case UNLOADED -> "commands.maple_flavored.keepers.unloaded";
			case MISSING -> "commands.maple_flavored.keepers.missing";
		};

		return new Line(keeper.spot(), status, keeper.status() == ShriekerKeepers.Status.STANDING);
	}

	private static Line mobLine(MobKeepers.Sighting sighting) {
		String status = switch (sighting.status()) {
			case ALIVE -> "commands.maple_flavored.keepers.alive";
			case GONE -> "commands.maple_flavored.keepers.gone";
			case UNLOADED -> "commands.maple_flavored.keepers.mob_unloaded";
		};

		return new Line(sighting.at(), status, sighting.status() == MobKeepers.Status.ALIVE);
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

	/**
	 * What's written for one keeper.
	 *
	 * @param at where it is, or was last known to be
	 * @param status the translation key of how it is
	 * @param isThere whether it's there to be found, standing or alive
	 */
	private record Line(BlockPos at, String status, boolean isThere) {
	}
}

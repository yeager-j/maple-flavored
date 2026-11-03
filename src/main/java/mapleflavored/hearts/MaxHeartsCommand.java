package mapleflavored.hearts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Collection;
import java.util.function.IntUnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /maxhearts}, an operator command for checking and changing Max Hearts, like vanilla's {@code /experience}.
 *
 * <p>{@code query <target>}, {@code set <targets> <amount>} and {@code add <targets> <amount>}, where a negative
 * amount removes hearts. Results stay between 1 and 30 Max Hearts.
 */
final class MaxHeartsCommand {
	private static final int MIN_MAX_HEARTS = 1;

	private MaxHeartsCommand() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("maxhearts")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(
					Commands.literal("query")
						.then(
							Commands.argument("target", EntityArgument.player())
								.executes(c -> query(c.getSource(), EntityArgument.getPlayer(c, "target")))
						)
				)
				.then(
					Commands.literal("set")
						.then(
							Commands.argument("targets", EntityArgument.players())
								.then(
									Commands.argument("amount", IntegerArgumentType.integer(MIN_MAX_HEARTS, Hearts.MAX_HEARTS_CAP))
										.executes(c -> {
											int amount = IntegerArgumentType.getInteger(c, "amount");

											return change(c.getSource(), EntityArgument.getPlayers(c, "targets"), maxHearts -> amount);
										})
								)
						)
				)
				.then(
					Commands.literal("add")
						.then(
							Commands.argument("targets", EntityArgument.players())
								.then(
									Commands.argument("amount", IntegerArgumentType.integer())
										.executes(c -> {
											int amount = IntegerArgumentType.getInteger(c, "amount");

											return change(c.getSource(), EntityArgument.getPlayers(c, "targets"), maxHearts -> maxHearts + amount);
										})
								)
						)
				)
		);
	}

	private static int query(CommandSourceStack source, ServerPlayer target) {
		int maxHearts = Hearts.maxHearts(target);

		source.sendSuccess(() -> Component.translatable("commands.maple_flavored.maxhearts.query", target.getDisplayName(), maxHearts), false);

		return maxHearts;
	}

	private static int change(CommandSourceStack source, Collection<ServerPlayer> targets, IntUnaryOperator newMaxHearts) {
		for (ServerPlayer target : targets) {
			int requested = newMaxHearts.applyAsInt(Hearts.maxHearts(target));
			int maxHearts = Math.clamp(requested, MIN_MAX_HEARTS, Hearts.MAX_HEARTS_CAP);

			Hearts.setMaxHearts(target, maxHearts);
			source.sendSuccess(() -> Component.translatable("commands.maple_flavored.maxhearts.set", target.getDisplayName(), maxHearts), true);
		}

		return targets.size();
	}
}

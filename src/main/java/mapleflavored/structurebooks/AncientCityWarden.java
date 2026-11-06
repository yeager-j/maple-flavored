package mapleflavored.structurebooks;

import java.util.Optional;
import mapleflavored.wardensummoning.WardenSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ancient City's wake event: taking its book summons a Warden, angry at the player. The Warden is found a spot
 * before the book is given, so the book is never taken without it.
 */
final class AncientCityWarden {
	// The most anger a Warden holds. It fades by 1 a second, so the Warden stays angry for over a minute: long after it has
	// emerged and roared.
	private static final int FURY = 150;
	// How far under the book a Warden can stand: down to the slab floor of the sunken pit one city centre has as its altar.
	private static final int FLOOR_REACH = 4;

	private AncientCityWarden() {
	}

	/**
	 * Whether taking the book at {@code book} summons a Warden: it's an Ancient City's book, and Wardens can be summoned
	 * here. As for a shrieker, they can't on Peaceful or with the {@code spawn_wardens} game rule off.
	 */
	static boolean guards(ServerLevel level, BlockPos book) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_WARDENS)) return false;

		return level.structureManager().getStructureWithPieceAt(book, structure -> structure.is(BuiltinStructures.ANCIENT_CITY)).isValid();
	}

	/**
	 * Summons the book's Warden, angry at the player: through the summoning search near the player, else standing under
	 * the book.
	 *
	 * @return the Warden, or empty, with nothing spawned, when neither has room
	 */
	static Optional<Warden> summon(ServerLevel level, ServerPlayer player, BlockPos book) {
		Optional<Warden> warden = WardenSummoning.spawnNear(level, player).or(() -> spawnUnder(level, book));

		warden.ifPresent(summoned -> summoned.increaseAngerAt(player, FURY, false));

		return warden;
	}

	/**
	 * Spawns a Warden standing on the first floor under the book, as it is now, if a Warden has room there. The book has
	 * no collision, so the Warden can rise through it.
	 */
	private static Optional<Warden> spawnUnder(ServerLevel level, BlockPos book) {
		Optional<Vec3> feet = floorUnder(level, book).filter(floor -> level.getWorldBorder().isWithinBounds(BlockPos.containing(floor)));

		if (feet.isEmpty()) return Optional.empty();

		Warden warden = EntityTypes.WARDEN.create(level, null, BlockPos.containing(feet.get()), EntitySpawnReason.TRIGGERED, false, false);

		if (warden == null) return Optional.empty();

		warden.setPos(feet.get());

		if (!warden.checkSpawnObstruction(level)) {
			warden.discard();

			return Optional.empty();
		}

		level.addFreshEntityWithPassengers(warden);

		return Optional.of(warden);
	}

	// The middle of the top of the first block under the book with any collision, within reach.
	private static Optional<Vec3> floorUnder(ServerLevel level, BlockPos book) {
		for (int depth = 1; depth <= FLOOR_REACH; depth++) {
			BlockPos below = book.below(depth);
			VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);

			if (!shape.isEmpty()) return Optional.of(Vec3.atBottomCenterOf(below).add(0.0, shape.max(Direction.Axis.Y), 0.0));
		}

		return Optional.empty();
	}
}

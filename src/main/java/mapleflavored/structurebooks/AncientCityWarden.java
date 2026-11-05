package mapleflavored.structurebooks;

import java.util.Optional;
import mapleflavored.wardensummoning.WardenSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/**
 * The Ancient City's wake event: taking its book summons a Warden, angry at the player. The Warden is found a spot
 * before the book is given, so the book is never taken without it.
 */
final class AncientCityWarden {
	// The most anger a Warden holds. It fades by 1 a second, so the Warden stays angry for over a minute: long after it has
	// emerged and roared.
	private static final int FURY = 150;

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

	// One attempt with no spread, so vanilla's floor, border and room checks run at exactly the block under the book, as
	// it is now. The book has no collision, so the Warden can rise through it.
	private static Optional<Warden> spawnUnder(ServerLevel level, BlockPos book) {
		return SpawnUtil.trySpawnMob(
			EntityTypes.WARDEN,
			EntitySpawnReason.TRIGGERED,
			level,
			book.below(),
			1,
			0,
			0,
			SpawnUtil.Strategy.ON_TOP_OF_COLLIDER,
			false
		);
	}
}

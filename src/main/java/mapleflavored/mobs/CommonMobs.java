package mapleflavored.mobs;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Common mobs are faster and more fragile: each listed type gets attribute base values by difficulty when it spawns,
 * converts or generates with a structure. Unlisted mobs, adult zombie villagers and Peaceful stay vanilla.
 */
public final class CommonMobs {
	private static final Map<Holder<Attribute>, Double> VANILLA = Map.of();

	private static final List<Row> TABLE = List.of(
		new Row(
			Set.of(EntityTypes.ZOMBIE),
			Age.ADULT,
			VANILLA,
			Map.of(Attributes.MAX_HEALTH, 10.0, Attributes.MOVEMENT_SPEED, 0.34, Attributes.STEP_HEIGHT, 1.0),
			Map.of(
				Attributes.MAX_HEALTH, 15.0,
				Attributes.MOVEMENT_SPEED, 0.38,
				Attributes.STEP_HEIGHT, 1.0,
				Attributes.ATTACK_DAMAGE, 5.0,
				Attributes.JUMP_STRENGTH, 0.6
			)
		),
		new Row(
			Set.of(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER),
			Age.BABY,
			Map.of(Attributes.MAX_HEALTH, 4.0),
			Map.of(Attributes.MAX_HEALTH, 4.0),
			Map.of(Attributes.MAX_HEALTH, 7.0)
		),
		new Row(
			Set.of(EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.PARCHED),
			Age.ANY,
			Map.of(Attributes.MAX_HEALTH, 10.0),
			Map.of(Attributes.MAX_HEALTH, 10.0),
			Map.of(Attributes.MAX_HEALTH, 12.0)
		),
		new Row(
			Set.of(EntityTypes.CREEPER),
			Age.ANY,
			Map.of(Attributes.MAX_HEALTH, 14.0),
			Map.of(Attributes.MAX_HEALTH, 16.0),
			Map.of(Attributes.MAX_HEALTH, 18.0)
		),
		new Row(
			Set.of(EntityTypes.CAVE_SPIDER),
			Age.ANY,
			Map.of(Attributes.MAX_HEALTH, 2.0, Attributes.MOVEMENT_SPEED, 0.35),
			Map.of(Attributes.MAX_HEALTH, 4.0, Attributes.MOVEMENT_SPEED, 0.40),
			Map.of(Attributes.MAX_HEALTH, 6.0, Attributes.MOVEMENT_SPEED, 0.42)
		),
		new Row(
			Set.of(EntityTypes.SILVERFISH),
			Age.ANY,
			Map.of(Attributes.MAX_HEALTH, 2.0),
			Map.of(Attributes.MAX_HEALTH, 2.0),
			Map.of(Attributes.MAX_HEALTH, 4.0)
		),
		new Row(
			Set.of(EntityTypes.SPIDER),
			Age.ANY,
			VANILLA,
			Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0),
			Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
		),
		new Row(
			Set.of(EntityTypes.HUSK),
			Age.ADULT,
			VANILLA,
			Map.of(
				Attributes.MAX_HEALTH, 30.0,
				Attributes.MOVEMENT_SPEED, 0.25,
				Attributes.ATTACK_DAMAGE, 6.0,
				Attributes.ARMOR, 12.0,
				Attributes.FOLLOW_RANGE, 50.0,
				Attributes.KNOCKBACK_RESISTANCE, 0.9
			),
			Map.of(
				Attributes.MAX_HEALTH, 40.0,
				Attributes.MOVEMENT_SPEED, 0.265,
				Attributes.ATTACK_DAMAGE, 8.0,
				Attributes.ARMOR, 14.0,
				Attributes.FOLLOW_RANGE, 60.0,
				Attributes.KNOCKBACK_RESISTANCE, 1.0
			)
		)
	);

	private CommonMobs() {
	}

	/**
	 * Gives {@code entity} and every mob riding it their spawn stats. Call as a new entity enters the world, once its
	 * type and age are final. Repeating it changes nothing.
	 */
	public static void applySpawnStats(Entity entity) {
		entity.getSelfAndPassengers()
			.filter(Mob.class::isInstance)
			.map(Mob.class::cast)
			.forEach(CommonMobs::applyRow);
	}

	// Sets the mob's base values to its row for the level's current difficulty, with full health if the row sets max health.
	private static void applyRow(Mob mob) {
		Map<Holder<Attribute>, Double> stats = statsFor(mob.getType(), mob.isBaby(), mob.level().getDifficulty());

		stats.forEach((attribute, value) -> mob.getAttribute(attribute).setBaseValue(value));

		if (stats.containsKey(Attributes.MAX_HEALTH)) {
			mob.setHealth(mob.getMaxHealth());
		}
	}

	private static Map<Holder<Attribute>, Double> statsFor(EntityType<?> type, boolean baby, Difficulty difficulty) {
		return TABLE.stream()
			.filter(row -> row.matches(type, baby))
			.findFirst()
			.map(row -> row.statsOn(difficulty))
			.orElse(VANILLA);
	}

	private enum Age {
		ADULT,
		BABY,
		ANY;

		boolean includes(boolean baby) {
			return switch (this) {
				case ADULT -> !baby;
				case BABY -> baby;
				case ANY -> true;
			};
		}
	}

	private record Row(
		Set<EntityType<?>> types,
		Age age,
		Map<Holder<Attribute>, Double> easy,
		Map<Holder<Attribute>, Double> normal,
		Map<Holder<Attribute>, Double> hard
	) {
		boolean matches(EntityType<?> type, boolean baby) {
			return types.contains(type) && age.includes(baby);
		}

		Map<Holder<Attribute>, Double> statsOn(Difficulty difficulty) {
			return switch (difficulty) {
				case PEACEFUL -> VANILLA;
				case EASY -> easy;
				case NORMAL -> normal;
				case HARD -> hard;
			};
		}
	}
}

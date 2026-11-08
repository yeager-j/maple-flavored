package mapleflavored.mobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mapleflavored.deepvariants.DeepVariants;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Common mobs are faster and more fragile: each listed type gets attribute base values by difficulty when it spawns,
 * converts or generates with a structure. Unlisted mobs, adult zombie villagers and Peaceful stay vanilla. In the
 * Overworld, a mob that spawns below Y 63 keeps vanilla health unless its row's health applies at any height, so caving
 * stays harder than the surface. A Deep variant takes its base mob's row, with its own health and damage instead.
 */
public final class CommonMobs {
	private static final Map<Holder<Attribute>, Double> VANILLA = Map.of();
	private static final int LOWEST_SURFACE_Y = 63;

	private static final List<Row> TABLE = List.of(
		new Row(
			Set.of(EntityTypes.ZOMBIE),
			Age.ADULT,
			HealthReach.SURFACE,
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
			HealthReach.SURFACE,
			Map.of(Attributes.MAX_HEALTH, 4.0),
			Map.of(Attributes.MAX_HEALTH, 4.0),
			Map.of(Attributes.MAX_HEALTH, 7.0)
		),
		new Row(
			Set.of(EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.PARCHED),
			Age.ANY,
			HealthReach.SURFACE,
			Map.of(Attributes.MAX_HEALTH, 10.0),
			Map.of(Attributes.MAX_HEALTH, 10.0),
			Map.of(Attributes.MAX_HEALTH, 12.0)
		),
		new Row(
			Set.of(EntityTypes.CREEPER),
			Age.ANY,
			HealthReach.SURFACE,
			Map.of(Attributes.MAX_HEALTH, 14.0),
			Map.of(Attributes.MAX_HEALTH, 16.0),
			Map.of(Attributes.MAX_HEALTH, 18.0)
		),
		new Row(
			Set.of(EntityTypes.CAVE_SPIDER),
			Age.ANY,
			HealthReach.ANY_HEIGHT,
			Map.of(Attributes.MAX_HEALTH, 2.0, Attributes.MOVEMENT_SPEED, 0.35),
			Map.of(Attributes.MAX_HEALTH, 4.0, Attributes.MOVEMENT_SPEED, 0.40),
			Map.of(Attributes.MAX_HEALTH, 6.0, Attributes.MOVEMENT_SPEED, 0.42)
		),
		new Row(
			Set.of(EntityTypes.SILVERFISH),
			Age.ANY,
			HealthReach.ANY_HEIGHT,
			Map.of(Attributes.MAX_HEALTH, 2.0),
			Map.of(Attributes.MAX_HEALTH, 2.0),
			Map.of(Attributes.MAX_HEALTH, 4.0)
		),
		new Row(
			Set.of(EntityTypes.SPIDER),
			Age.ANY,
			HealthReach.SURFACE,
			VANILLA,
			Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0),
			Map.of(Attributes.JUMP_STRENGTH, 0.85, Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
		),
		new Row(
			Set.of(EntityTypes.HUSK),
			Age.ADULT,
			HealthReach.SURFACE,
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
	 * type, age and spawn position are final. Repeating it at the same height changes nothing; calling it after the mob
	 * has moved may pick different health.
	 */
	public static void applySpawnStats(Entity entity) {
		entity.getSelfAndPassengers()
			.filter(Mob.class::isInstance)
			.map(Mob.class::cast)
			.forEach(CommonMobs::applyRow);
	}

	// Sets the mob's base values to its row for the level's current difficulty and its height, with full health if the
	// row sets max health there.
	private static void applyRow(Mob mob) {
		boolean underground = mob.level().dimension() == Level.OVERWORLD && mob.getBlockY() < LOWEST_SURFACE_Y;
		Optional<EntityType<? extends Mob>> deepBase = DeepVariants.baseOf(mob.getType());
		Map<Holder<Attribute>, Double> stats = deepBase.isPresent()
			? deepVariantStats(mob, deepBase.get(), underground)
			: statsFor(mob.getType(), mob.isBaby(), mob.level().getDifficulty(), underground);

		stats.forEach((attribute, value) -> mob.getAttribute(attribute).setBaseValue(value));

		if (stats.containsKey(Attributes.MAX_HEALTH)) {
			mob.setHealth(mob.getMaxHealth());
		}
	}

	// A Deep variant takes its base mob's row, except that its health and damage come from the Deep variant rules.
	private static Map<Holder<Attribute>, Double> deepVariantStats(Mob mob, EntityType<? extends Mob> base, boolean underground) {
		Map<Holder<Attribute>, Double> stats = new HashMap<>(statsFor(base, mob.isBaby(), mob.level().getDifficulty(), underground));
		GameRules rules = ((ServerLevel) mob.level()).getGameRules();

		stats.remove(Attributes.MAX_HEALTH);
		stats.remove(Attributes.ATTACK_DAMAGE);
		stats.putAll(DeepVariants.spawnStats(base, rules));

		return stats;
	}

	private static Map<Holder<Attribute>, Double> statsFor(EntityType<?> type, boolean baby, Difficulty difficulty, boolean underground) {
		return TABLE.stream()
			.filter(row -> row.matches(type, baby))
			.findFirst()
			.map(row -> row.statsFor(difficulty, underground))
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

	// Where a row's max health applies. Its other stats apply at any height.
	private enum HealthReach {
		SURFACE,
		ANY_HEIGHT
	}

	private record Row(
		Set<EntityType<?>> types,
		Age age,
		HealthReach healthReach,
		Map<Holder<Attribute>, Double> easy,
		Map<Holder<Attribute>, Double> normal,
		Map<Holder<Attribute>, Double> hard
	) {
		boolean matches(EntityType<?> type, boolean baby) {
			return types.contains(type) && age.includes(baby);
		}

		Map<Holder<Attribute>, Double> statsFor(Difficulty difficulty, boolean underground) {
			Map<Holder<Attribute>, Double> stats = statsOn(difficulty);

			if (!underground || healthReach == HealthReach.ANY_HEIGHT) return stats;

			Map<Holder<Attribute>, Double> withoutHealth = new HashMap<>(stats);

			withoutHealth.remove(Attributes.MAX_HEALTH);

			return withoutHealth;
		}

		private Map<Holder<Attribute>, Double> statsOn(Difficulty difficulty) {
			return switch (difficulty) {
				case PEACEFUL -> VANILLA;
				case EASY -> easy;
				case NORMAL -> normal;
				case HARD -> hard;
			};
		}
	}
}

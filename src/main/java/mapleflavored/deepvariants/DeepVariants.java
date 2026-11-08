package mapleflavored.deepvariants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The Deep variants module: stronger forms of common mobs that take their place in natural Overworld spawns below Y 0.
 * Each is built on its base mob's class, is in every entity tag its base mob is in, and drops its base mob's loot.
 * Spawners, spawn eggs, commands, structures, conversions and Blood Moon Restores still make the base mob.
 */
public final class DeepVariants {
	/** How many times its base mob's vanilla XP a Deep variant drops. */
	public static final int XP_MULTIPLIER = 2;

	private static final int DEEP_BELOW_Y = 0;
	private static final ResourceKey<EntityType<?>> DEEP_ZOMBIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MapleFlavored.id("deep_zombie"));

	/** The Deep Zombie: a zombie whose hits disable shields. */
	public static final EntityType<DeepZombie> DEEP_ZOMBIE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		DEEP_ZOMBIE_KEY,
		FabricEntityType.Builder.<DeepZombie>createMob(
				DeepZombie::new,
				MobCategory.MONSTER,
				mob -> mob.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules)
					.defaultAttributes(Zombie::createAttributes)
			)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.passengerAttachments(2.0125F)
			.ridingOffset(-0.7F)
			.clientTrackingRange(8)
			.notInPeaceful()
			.build(DEEP_ZOMBIE_KEY)
	);

	/** The Deep Zombie's spawn egg. */
	public static final Item DEEP_ZOMBIE_SPAWN_EGG = registerSpawnEgg("deep_zombie_spawn_egg", DEEP_ZOMBIE);

	// Each base mob and the Deep variant that takes its place.
	private static final Map<EntityType<? extends Mob>, EntityType<? extends Mob>> DEEP_BY_BASE = Map.of(EntityTypes.ZOMBIE, DEEP_ZOMBIE);
	private static final Map<EntityType<? extends Mob>, EntityType<? extends Mob>> BASE_BY_DEEP = inverse(DEEP_BY_BASE);

	private DeepVariants() {
	}

	/** Adds the spawn eggs to the creative menu. Call once from mod init. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.insertAfter(Items.ZOMBIE_SPAWN_EGG, DEEP_ZOMBIE_SPAWN_EGG));
	}

	/** The base mob a Deep variant stands in for, or empty if {@code type} isn't a Deep variant. */
	public static Optional<EntityType<? extends Mob>> baseOf(EntityType<?> type) {
		return Optional.ofNullable(BASE_BY_DEEP.get(type));
	}

	/**
	 * What natural spawning makes when it has picked {@code picked} for {@code pos}, and the group data to finalize it
	 * with. Call before the mob is created, with the group's data so far.
	 *
	 * <p>A zombie's age is normally rolled as it finalizes. For a zombie that could become a Deep Zombie it's rolled
	 * here instead, with vanilla's odds, so the returned group data must replace the group's: a baby roll stays a zombie.
	 */
	public static NaturalSpawn naturalSpawn(EntityType<?> picked, ServerLevel level, BlockPos pos, @Nullable SpawnGroupData groupData) {
		EntityType<? extends Mob> deep = DEEP_BY_BASE.get(picked);

		if (deep == null || !isDeep(level, pos)) return new NaturalSpawn(picked, groupData);

		SpawnGroupData aged = needsZombieAge(picked, groupData) ? rollZombieAge(level.getRandom()) : groupData;

		return new NaturalSpawn(isBaby(aged) ? picked : deep, aged);
	}

	/**
	 * A Deep variant's health and damage base values from the game rules, as percentages of {@code base}'s vanilla
	 * values. Damage is left out for a base mob without an attack attribute.
	 */
	public static Map<Holder<Attribute>, Double> spawnStats(EntityType<? extends Mob> base, GameRules rules) {
		AttributeSupplier vanilla = DefaultAttributes.getSupplier(base);
		Map<Holder<Attribute>, Double> stats = new HashMap<>();

		stats.put(Attributes.MAX_HEALTH, percentOf(vanilla.getBaseValue(Attributes.MAX_HEALTH), rules.get(MapleGameRules.DEEP_VARIANT_HEALTH_PERCENT)));

		if (vanilla.hasAttribute(Attributes.ATTACK_DAMAGE)) {
			stats.put(Attributes.ATTACK_DAMAGE, percentOf(vanilla.getBaseValue(Attributes.ATTACK_DAMAGE), rules.get(MapleGameRules.DEEP_VARIANT_DAMAGE_PERCENT)));
		}

		return stats;
	}

	private static boolean isDeep(ServerLevel level, BlockPos pos) {
		return level.dimension() == Level.OVERWORLD && pos.getY() < DEEP_BELOW_Y;
	}

	// A zombie group's age is rolled once, by its first member.
	private static boolean needsZombieAge(EntityType<?> picked, @Nullable SpawnGroupData groupData) {
		return picked == EntityTypes.ZOMBIE && groupData == null;
	}

	// Rolls the age as Zombie.finalizeSpawn would for a new group.
	private static SpawnGroupData rollZombieAge(RandomSource random) {
		return new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(random), true);
	}

	private static boolean isBaby(@Nullable SpawnGroupData groupData) {
		return groupData instanceof Zombie.ZombieGroupData zombieData && zombieData.isBaby;
	}

	private static double percentOf(double value, int percent) {
		return value * percent / 100.0;
	}

	private static <K, V> Map<V, K> inverse(Map<K, V> map) {
		Map<V, K> inverse = new HashMap<>();

		map.forEach((key, value) -> inverse.put(value, key));

		return Map.copyOf(inverse);
	}

	private static Item registerSpawnEgg(String name, EntityType<? extends Mob> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MapleFlavored.id(name));

		return Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
	}

	/** The type natural spawning should create, and the group data to finalize it with. */
	public record NaturalSpawn(EntityType<?> type, @Nullable SpawnGroupData groupData) {
	}
}

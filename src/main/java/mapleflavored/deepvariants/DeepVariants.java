package mapleflavored.deepvariants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import mapleflavored.MapleFlavored;
import mapleflavored.gamerules.MapleGameRules;
import mapleflavored.items.MapleItems;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The Deep variants module: stronger forms of common mobs that take their place in natural Overworld spawns below Y 0.
 * Each is built on its base mob's class, is in every entity tag its base mob is in, and drops its base mob's loot,
 * with a small chance of Pith when a player kills one that no spawner made.
 * Spawners, spawn eggs, commands, structures, conversions and Blood Moon Restores still make the base mob.
 */
public final class DeepVariants {
	/** How many times its base mob's vanilla XP a Deep variant drops. */
	public static final int XP_MULTIPLIER = 2;


	/** The Deep Zombie: a zombie whose hits disable shields. */
	public static final EntityType<DeepZombie> DEEP_ZOMBIE = registerEntityType(
		"deep_zombie",
		DeepZombie::new,
		Zombie::createAttributes,
		builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F)
	);

	/** The Deep Skeleton: a skeleton with a longer follow range whose arrows carry Slowness. */
	public static final EntityType<DeepSkeleton> DEEP_SKELETON = registerEntityType(
		"deep_skeleton",
		DeepSkeleton::new,
		DeepSkeleton::createAttributes,
		builder -> builder.sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F)
	);

	/** The Deep Creeper: a creeper that explodes as hard as TNT. */
	public static final EntityType<DeepCreeper> DEEP_CREEPER = registerEntityType(
		"deep_creeper",
		DeepCreeper::new,
		Creeper::createAttributes,
		builder -> builder.sized(0.6F, 1.7F)
	);

	/** The Deep Spider: a spider whose bites blind. */
	public static final EntityType<DeepSpider> DEEP_SPIDER = registerEntityType(
		"deep_spider",
		DeepSpider::new,
		Spider::createAttributes,
		builder -> builder.sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F)
	);

	/** The Deep Zombie's spawn egg. */
	public static final Item DEEP_ZOMBIE_SPAWN_EGG = registerSpawnEgg("deep_zombie_spawn_egg", DEEP_ZOMBIE);
	/** The Deep Skeleton's spawn egg. */
	public static final Item DEEP_SKELETON_SPAWN_EGG = registerSpawnEgg("deep_skeleton_spawn_egg", DEEP_SKELETON);
	/** The Deep Creeper's spawn egg. */
	public static final Item DEEP_CREEPER_SPAWN_EGG = registerSpawnEgg("deep_creeper_spawn_egg", DEEP_CREEPER);
	/** The Deep Spider's spawn egg. */
	public static final Item DEEP_SPIDER_SPAWN_EGG = registerSpawnEgg("deep_spider_spawn_egg", DEEP_SPIDER);

	// Each base mob and the Deep variant that takes its place.
	private static final Map<EntityType<? extends Mob>, EntityType<? extends Mob>> DEEP_BY_BASE = Map.of(
		EntityTypes.ZOMBIE, DEEP_ZOMBIE,
		EntityTypes.SKELETON, DEEP_SKELETON,
		EntityTypes.CREEPER, DEEP_CREEPER,
		EntityTypes.SPIDER, DEEP_SPIDER
	);
	private static final Map<EntityType<? extends Mob>, EntityType<? extends Mob>> BASE_BY_DEEP = inverse(DEEP_BY_BASE);
	// A Deep Creeper's threat is its fixed explosion power, so it spawns with exactly its base mob's stats.
	private static final Set<EntityType<? extends Mob>> BASES_WITH_UNCHANGED_STATS = Set.of(EntityTypes.CREEPER);

	private DeepVariants() {
	}

	/** Adds the spawn eggs to the creative menu and registers the Pith drop's loot condition. Call once from mod init. */
	public static void register() {
		Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, MapleFlavored.id("deep_variant_pith_roll"), PithRoll.MAP_CODEC);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> {
				output.insertAfter(Items.ZOMBIE_SPAWN_EGG, DEEP_ZOMBIE_SPAWN_EGG);
				output.insertAfter(Items.SKELETON_SPAWN_EGG, DEEP_SKELETON_SPAWN_EGG);
				output.insertAfter(Items.CREEPER_SPAWN_EGG, DEEP_CREEPER_SPAWN_EGG);
				output.insertAfter(Items.SPIDER_SPAWN_EGG, DEEP_SPIDER_SPAWN_EGG);
			});
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

		if (deep == null || !OverworldDepths.isDeep(level, pos.getY())) return new NaturalSpawn(picked, groupData);

		SpawnGroupData aged = needsZombieAge(picked, groupData) ? rollZombieAge(level.getRandom()) : groupData;

		return new NaturalSpawn(isBaby(aged) ? picked : deep, aged);
	}

	/**
	 * The attribute base values the Deep variant of {@code base} spawns with, given {@code baseMobStats}: the values
	 * its base mob would spawn with at the same spot. An attribute in neither keeps the entity type's default.
	 *
	 * <p>The Deep Creeper takes its base mob's values unchanged. Every other Deep variant takes them with its health
	 * and attack damage replaced by the game rules' percentages of {@code base}'s vanilla values; damage is left out
	 * for a base mob without an attack attribute. {@link #damageMultiplier} reads the attack damage written here.
	 */
	public static Map<Holder<Attribute>, Double> spawnStats(EntityType<? extends Mob> base, Map<Holder<Attribute>, Double> baseMobStats, GameRules rules) {
		if (BASES_WITH_UNCHANGED_STATS.contains(base)) return baseMobStats;

		AttributeSupplier vanilla = DefaultAttributes.getSupplier(base);
		Map<Holder<Attribute>, Double> stats = new HashMap<>(baseMobStats);

		// The base mob's own damage goes even when the rule has none to put in its place.
		stats.remove(Attributes.ATTACK_DAMAGE);
		stats.put(Attributes.MAX_HEALTH, percentOf(vanilla.getBaseValue(Attributes.MAX_HEALTH), rules.get(MapleGameRules.DEEP_VARIANT_HEALTH_PERCENT)));

		if (vanilla.hasAttribute(Attributes.ATTACK_DAMAGE)) {
			stats.put(Attributes.ATTACK_DAMAGE, percentOf(vanilla.getBaseValue(Attributes.ATTACK_DAMAGE), rules.get(MapleGameRules.DEEP_VARIANT_DAMAGE_PERCENT)));
		}

		return stats;
	}

	/**
	 * How many times its base mob's vanilla damage {@code deep} deals, as set by the damage rule when it spawned: the
	 * attack damage {@link #spawnStats} gave it, over vanilla's. Scale damage the attack attribute doesn't cover, such
	 * as arrows, by this. 1 for a mob that isn't a Deep variant.
	 */
	public static double damageMultiplier(Mob deep) {
		Optional<EntityType<? extends Mob>> base = baseOf(deep.getType());

		if (base.isEmpty()) return 1.0;

		double vanillaDamage = DefaultAttributes.getSupplier(base.get()).getBaseValue(Attributes.ATTACK_DAMAGE);

		return deep.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) / vanillaDamage;
	}

	/** The skeleton type that rides {@code spider} when it spawns as a jockey: a Deep Skeleton for a Deep Spider. */
	public static EntityType<? extends Skeleton> jockeyRider(Spider spider) {
		return spider.getType() == DEEP_SPIDER ? DEEP_SKELETON : EntityTypes.SKELETON;
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

	// Registers a monster with a Deep variant's shared traits: a monster's spawn rules, tracking range and no Peaceful.
	private static <T extends Mob> EntityType<T> registerEntityType(
		String name,
		EntityType.EntityFactory<T> factory,
		Supplier<AttributeSupplier.Builder> attributes,
		UnaryOperator<EntityType.Builder<T>> sizing
	) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MapleFlavored.id(name));
		EntityType.Builder<T> builder = FabricEntityType.Builder.createMob(
			factory,
			MobCategory.MONSTER,
			mob -> mob.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules)
				.defaultAttributes(attributes)
		);

		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, sizing.apply(builder).clientTrackingRange(8).notInPeaceful().build(key));
	}

	private static Item registerSpawnEgg(String name, EntityType<? extends Mob> type) {
		return MapleItems.register(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type));
	}

	/** The type natural spawning should create, and the group data to finalize it with. */
	public record NaturalSpawn(EntityType<?> type, @Nullable SpawnGroupData groupData) {
	}
}

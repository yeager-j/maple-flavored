package mapleflavored.mobs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import mapleflavored.MapleFlavored;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Raiders a raid spawned as one of its waves, as opposed to raiders that came from anywhere else and maybe joined a raid
 * later. Loot tables tell them apart with the {@code maple_flavored:spawned_by_raid_wave} condition, which passes when the
 * entity being looted was spawned by a raid wave, so raid farms don't drop Illager books.
 */
public final class RaidWaves {
	// Saved with the raider, so it outlives the raid and reloads.
	private static final AttachmentType<Boolean> SPAWNED_BY_WAVE = AttachmentRegistry.create(
		MapleFlavored.id("spawned_by_raid_wave"),
		builder -> builder.persistent(Codec.BOOL)
	);

	private RaidWaves() {
	}

	/** Registers the {@code maple_flavored:spawned_by_raid_wave} loot condition. Call once at mod initialization. */
	static void register() {
		Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, MapleFlavored.id("spawned_by_raid_wave"), SpawnedByRaidWave.MAP_CODEC);
	}

	/** Marks a raider a raid is spawning as part of a wave. Call before the raider joins the level. */
	public static void markSpawnedByWave(Raider raider) {
		raider.setAttached(SPAWNED_BY_WAVE, true);
	}

	/** Whether the entity is a raider a raid spawned as part of a wave. */
	public static boolean spawnedByWave(Entity entity) {
		return entity.hasAttached(SPAWNED_BY_WAVE);
	}

	private static final class SpawnedByRaidWave implements LootItemCondition {
		private static final SpawnedByRaidWave INSTANCE = new SpawnedByRaidWave();
		static final MapCodec<SpawnedByRaidWave> MAP_CODEC = MapCodec.unit(INSTANCE);

		@Override
		public MapCodec<SpawnedByRaidWave> codec() {
			return MAP_CODEC;
		}

		@Override
		public Set<ContextKey<?>> getReferencedContextParams() {
			return Set.of(LootContextParams.THIS_ENTITY);
		}

		@Override
		public boolean test(LootContext context) {
			Entity looted = context.getOptional(LootContextParams.THIS_ENTITY);

			return looted != null && spawnedByWave(looted);
		}
	}
}

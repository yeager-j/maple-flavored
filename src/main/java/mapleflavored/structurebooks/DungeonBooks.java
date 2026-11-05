package mapleflavored.structurebooks;

import java.util.List;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * The Dungeon's structure book, which fights the dungeon's own mob: Smite for a zombie or skeleton dungeon, Bane of
 * Arthropods for a spider dungeon. Dungeons are placed features, not structures, so the book isn't a block: the
 * dungeon's spawner holds it and drops it with its Pith once Cleared and broken.
 */
public final class DungeonBooks {
	/** The enchantments a Dungeon's book can have. */
	public static final List<ResourceKey<Enchantment>> ENCHANTMENTS = List.of(Enchantments.BANE_OF_ARTHROPODS, Enchantments.SMITE);

	private DungeonBooks() {
	}

	/**
	 * The book for a dungeon that spawns {@code mob}, read from the tags those enchantments hit harder. A mob in neither
	 * tag gets one of the two with equal weight, chosen with {@code random}.
	 */
	public static ResourceKey<Enchantment> forMob(EntityType<?> mob, RandomSource random) {
		if (mob.builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) return Enchantments.SMITE;
		if (mob.builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) return Enchantments.BANE_OF_ARTHROPODS;

		return Util.getRandom(ENCHANTMENTS, random);
	}

	/** Gives a dungeon's freshly placed spawner the book for {@code mob}, the mob the dungeon chose for it. */
	public static void give(WorldGenLevel level, SpawnerBlockEntity spawner, EntityType<?> mob, RandomSource random) {
		var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(forMob(mob, random));

		Spawners.holdBook(spawner, enchantment);
	}
}

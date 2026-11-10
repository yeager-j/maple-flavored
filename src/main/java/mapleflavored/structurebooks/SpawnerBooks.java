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
 * The books held by spawners placed at generation: each Dungeon's spawner and each Mineshaft cave spider spawner. The
 * book fights the spawner's own mob: Smite for zombies and skeletons, Bane of Arthropods for spiders and cave spiders.
 * The spawner drops it with its Pith once Cleared and broken.
 */
public final class SpawnerBooks {
	/** The enchantments a spawner's book can have. */
	public static final List<ResourceKey<Enchantment>> ENCHANTMENTS = List.of(Enchantments.BANE_OF_ARTHROPODS, Enchantments.SMITE);

	private SpawnerBooks() {
	}

	/**
	 * The book for a spawner that spawns {@code mob}, read from the tags those enchantments hit harder. A mob in neither
	 * tag gets one of the two with equal weight, chosen with {@code random}.
	 */
	public static ResourceKey<Enchantment> forMob(EntityType<?> mob, RandomSource random) {
		if (mob.builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) return Enchantments.SMITE;
		if (mob.builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) return Enchantments.BANE_OF_ARTHROPODS;

		return Util.getRandom(ENCHANTMENTS, random);
	}

	/** Gives a spawner placed at generation the book for {@code mob}, the mob it was set to spawn. */
	public static void give(WorldGenLevel level, SpawnerBlockEntity spawner, EntityType<?> mob, RandomSource random) {
		var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(forMob(mob, random));

		Spawners.holdDrop(spawner, StructureBooks.levelOneBook(enchantment));
	}
}

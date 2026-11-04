package mapleflavored.structurebooks;

import java.util.List;
import mapleflavored.spawners.Spawners;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * The Dungeon's structure book. Dungeons are placed features, not structures, so the book isn't a block: the
 * dungeon's spawner holds it and drops it with its Pith once Cleared and broken.
 */
public final class DungeonBooks {
	/** The enchantments a Dungeon's book can have, with equal weight. */
	public static final List<ResourceKey<Enchantment>> ENCHANTMENTS = List.of(Enchantments.BANE_OF_ARTHROPODS, Enchantments.SMITE);

	private DungeonBooks() {
	}

	/** One of {@link #ENCHANTMENTS}, chosen with {@code random}. */
	public static ResourceKey<Enchantment> choose(RandomSource random) {
		return Util.getRandom(ENCHANTMENTS, random);
	}

	/** Gives a dungeon's freshly placed spawner its book, chosen with the dungeon's own random. */
	public static void give(WorldGenLevel level, SpawnerBlockEntity spawner, RandomSource random) {
		var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(choose(random));

		Spawners.holdBook(spawner, enchantment);
	}
}

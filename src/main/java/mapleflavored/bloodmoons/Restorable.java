package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import mapleflavored.MapleFlavored;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * A structure whose mobs Blood Moons Restore: which mobs, and the block that must still be there for one to return to
 * its spot. Each block family is a block tag, {@code maple_flavored:<structure>_restore_blocks}.
 */
public enum Restorable {
	/** Piglins, Brutes and Hoglins, standing on the blackstone or basalt family. */
	BASTION(
		BuiltinStructures.BASTION_REMNANT,
		Set.of(EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.HOGLIN),
		"bastion",
		Footing.STANDING
	),
	/** Elder Guardians, in water over the prismarine family. */
	OCEAN_MONUMENT(BuiltinStructures.OCEAN_MONUMENT, Set.of(EntityTypes.ELDER_GUARDIAN), "ocean_monument", Footing.SWIMMING),
	/** Shulkers, facing the purpur family or end stone bricks on some side. */
	END_CITY(BuiltinStructures.END_CITY, Set.of(EntityTypes.SHULKER), "end_city", Footing.ATTACHED),
	/** Vindicators and Evokers, standing on birch planks, cobblestone or a smooth stone slab. Allays never return. */
	WOODLAND_MANSION(
		BuiltinStructures.WOODLAND_MANSION,
		Set.of(EntityTypes.VINDICATOR, EntityTypes.EVOKER),
		"woodland_mansion",
		Footing.STANDING
	),
	/** Vindicators, Pillagers and Evokers, standing on the stone bricks family, infested or not, or a library's oak planks. */
	STRONGHOLD(
		BuiltinStructures.STRONGHOLD,
		Set.of(EntityTypes.VINDICATOR, EntityTypes.PILLAGER, EntityTypes.EVOKER),
		"stronghold",
		Footing.STANDING
	);

	static final Codec<Restorable> CODEC = Codec.STRING.xmap(Restorable::valueOf, Restorable::name);

	// How far each footing looks for its block. A structure doesn't always place its mobs against it: a Bastion places
	// them a block over the floor, an Ocean Monument a few blocks up in the water, and an End City's Sentries float in
	// its hollow towers until each Shulker finds a wall.
	private static final int MAX_DROP = 4;
	private static final int MAX_WATER_DEPTH = 16;
	private static final int MAX_REACH = 24;
	private static final double FEET_DEPTH = 0.01;

	private final ResourceKey<Structure> structure;
	private final Set<EntityType<?>> mobs;
	private final TagKey<Block> blocks;
	private final Footing footing;

	Restorable(ResourceKey<Structure> structure, Set<EntityType<?>> mobs, String name, Footing footing) {
		this.structure = structure;
		this.mobs = mobs;
		this.blocks = TagKey.create(Registries.BLOCK, MapleFlavored.id(name + "_restore_blocks"));
		this.footing = footing;
	}

	/** The Restorable the structure is, if it's one. */
	public static Optional<Restorable> of(RegistryAccess registries, Structure structure) {
		return registries.lookupOrThrow(Registries.STRUCTURE)
			.getResourceKey(structure)
			.flatMap(key -> Arrays.stream(values()).filter(restorable -> restorable.structure.equals(key)).findFirst());
	}

	/** Whether Blood Moons Restore this kind of mob in the structure. */
	public boolean restores(EntityType<?> type) {
		return mobs.contains(type);
	}

	/**
	 * Whether the structure's block still holds a mob's spot, where the mob's feet were at {@code position}. Reads only
	 * loaded chunks: a block in an unloaded one doesn't hold the spot.
	 */
	public boolean holds(LevelReader level, Vec3 position) {
		BlockPos spot = BlockPos.containing(position);

		return switch (footing) {
			case STANDING -> isBlock(level, firstBlock(level, underFeet(position), Direction.DOWN, MAX_DROP, BlockState::isAir));
			case ATTACHED -> Arrays.stream(Direction.values())
				.anyMatch(side -> isBlock(level, firstBlock(level, spot.relative(side), side, MAX_REACH, BlockState::isAir)));
			case SWIMMING -> level.hasChunkAt(spot)
				&& level.getFluidState(spot).is(FluidTags.WATER)
				&& isBlock(level, firstBlock(level, spot.below(), Direction.DOWN, MAX_WATER_DEPTH, state -> state.is(Blocks.WATER)));
		};
	}

	// The block a mob's feet rest on: the slab or stair itself when they stand on one, otherwise the block below.
	private static BlockPos underFeet(Vec3 feet) {
		return BlockPos.containing(feet.x, feet.y - FEET_DEPTH, feet.z);
	}

	// The first block from {@code from} onwards, going {@code direction}, that {@code passable} doesn't pass through, if
	// it's within {@code reach} blocks and loaded chunks. Reading a block in an unloaded chunk would load it. A
	// waterlogged slab or stair isn't water.
	private static Optional<BlockPos> firstBlock(
		LevelReader level,
		BlockPos from,
		Direction direction,
		int reach,
		Predicate<BlockState> passable
	) {
		BlockPos.MutableBlockPos pos = from.mutable();

		for (int step = 0; step < reach && level.hasChunkAt(pos); step++, pos.move(direction)) {
			if (!passable.test(level.getBlockState(pos))) return Optional.of(pos.immutable());
		}

		return Optional.empty();
	}

	private boolean isBlock(LevelReader level, Optional<BlockPos> pos) {
		return pos.map(found -> level.getBlockState(found).is(blocks)).orElse(false);
	}

	// Where a mob's spot finds its block.
	private enum Footing {
		// Over the first block below its feet.
		STANDING,
		// Facing the block on any side, as a Shulker attaches to whichever wall it finds.
		ATTACHED,
		// In water, over the first block below that isn't water.
		SWIMMING
	}
}

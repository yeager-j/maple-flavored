package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * One mob a structure placed as it generated: where, which mob and whether it was persistent, and whether that mob is
 * still there. A spot holds at most one mob: only a pending spot fills, and filling it makes it occupied.
 *
 * @param id this spot, which its mob carries
 * @param position where the mob's feet were
 * @param structure the structure it belongs to, which decides the block it needs
 * @param type the mob the spot first rolled, and the one it Restores
 * @param persistent whether the original never despawned, which the Restored mob keeps
 * @param occupant the mob holding the spot, while {@link State#OCCUPIED}
 * @param occupantChunk the chunk of the spot's level the occupant is saved in, packed, where that's known: not while
 *     it's in another dimension or saved with a player, nor for a spot saved before chunks were recorded
 */
public record RestoreSpot(
	UUID id,
	Vec3 position,
	Restorable structure,
	EntityType<?> type,
	boolean persistent,
	State state,
	Optional<UUID> occupant,
	Optional<Long> occupantChunk
) {
	static final Codec<RestoreSpot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(RestoreSpot::id),
		Vec3.CODEC.fieldOf("position").forGetter(RestoreSpot::position),
		Restorable.CODEC.fieldOf("structure").forGetter(RestoreSpot::structure),
		BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(RestoreSpot::type),
		Codec.BOOL.fieldOf("persistent").forGetter(RestoreSpot::persistent),
		State.CODEC.fieldOf("state").forGetter(RestoreSpot::state),
		UUIDUtil.CODEC.optionalFieldOf("occupant").forGetter(RestoreSpot::occupant),
		Codec.LONG.optionalFieldOf("occupant_chunk").forGetter(RestoreSpot::occupantChunk)
	).apply(instance, RestoreSpot::new));

	/** A new spot for the mob {@code occupant}, which a structure is placing. */
	static RestoreSpot occupiedBy(UUID occupant, Vec3 position, Restorable structure, EntityType<?> type, boolean persistent) {
		return new RestoreSpot(UUID.randomUUID(), position, structure, type, persistent, State.OCCUPIED, Optional.of(occupant), Optional.empty());
	}

	/** The chunk the spot is in, packed. */
	public long chunk() {
		return ChunkPos.pack(BlockPos.containing(position));
	}

	/** The spot held by {@code mob}, which is saved in {@code chunk}. */
	RestoreSpot filledBy(UUID mob, Optional<Long> chunk) {
		return new RestoreSpot(id, position, structure, type, persistent, State.OCCUPIED, Optional.of(mob), chunk);
	}

	RestoreSpot emptied() {
		return new RestoreSpot(id, position, structure, type, persistent, State.EMPTY, Optional.empty(), Optional.empty());
	}

	RestoreSpot madePending() {
		return new RestoreSpot(id, position, structure, type, persistent, State.PENDING, Optional.empty(), Optional.empty());
	}

	/** Whether a spot holds a mob, waits for a Blood Moon, or waits to fill. */
	public enum State {
		/** Its mob is alive somewhere, loaded or not. */
		OCCUPIED,
		/** Its mob is gone for good; the next Blood Moon makes it pending. */
		EMPTY,
		/** A Blood Moon rose while it was empty; it fills as soon as it can, however long that takes. */
		PENDING;

		static final Codec<State> CODEC = Codec.STRING.xmap(State::valueOf, State::name);
	}
}

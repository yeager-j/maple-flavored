package mapleflavored.bloodmoons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
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
 * @param occupancy whether a mob holds the spot, and which
 */
public record RestoreSpot(UUID id, Vec3 position, Restorable structure, EntityType<?> type, boolean persistent, Occupancy occupancy) {
	static final Codec<RestoreSpot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(RestoreSpot::id),
		Vec3.CODEC.fieldOf("position").forGetter(RestoreSpot::position),
		Restorable.CODEC.fieldOf("structure").forGetter(RestoreSpot::structure),
		BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(RestoreSpot::type),
		Codec.BOOL.fieldOf("persistent").forGetter(RestoreSpot::persistent),
		Occupancy.CODEC.forGetter(RestoreSpot::occupancy)
	).apply(instance, RestoreSpot::new));

	/** A new spot for the mob {@code occupant}, which a structure is placing. */
	static RestoreSpot occupiedBy(UUID occupant, Vec3 position, Restorable structure, EntityType<?> type, boolean persistent) {
		return new RestoreSpot(UUID.randomUUID(), position, structure, type, persistent, new Occupied(occupant, Optional.empty()));
	}

	/** The chunk the spot is in, packed. */
	public long chunk() {
		return ChunkPos.pack(BlockPos.containing(position));
	}

	/** Whether the spot holds a mob, waits for a Blood Moon, or waits to fill. */
	public State state() {
		return occupancy.state();
	}

	/** The mob holding the spot, while it's occupied. */
	public Optional<UUID> occupant() {
		return occupancy instanceof Occupied occupied ? Optional.of(occupied.occupant()) : Optional.empty();
	}

	/** The chunk of the spot's level its occupant is saved in, packed, while it's occupied and that's known. */
	public Optional<Long> occupantChunk() {
		return occupancy instanceof Occupied occupied ? occupied.savedChunk() : Optional.empty();
	}

	/** The spot held by {@code mob}, which is saved in {@code chunk}. */
	RestoreSpot filledBy(UUID mob, Optional<Long> chunk) {
		return with(new Occupied(mob, chunk));
	}

	RestoreSpot emptied() {
		return with(Vacant.EMPTY);
	}

	RestoreSpot madePending() {
		return with(Vacant.PENDING);
	}

	private RestoreSpot with(Occupancy occupancy) {
		return new RestoreSpot(id, position, structure, type, persistent, occupancy);
	}

	/** Whether a spot holds a mob, waits for a Blood Moon, or waits to fill. */
	public enum State implements StringRepresentable {
		/** Its mob is alive somewhere, loaded or not. */
		OCCUPIED("occupied"),
		/** Its mob is gone for good; the next Blood Moon makes it pending. */
		EMPTY("empty"),
		/** A Blood Moon rose while it was empty; it fills as soon as it can, however long that takes. */
		PENDING("pending");

		static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

		private final String name;

		State(String name) {
			this.name = name;
		}

		/** The name the state is saved under. It must never change once worlds hold spots. */
		@Override
		public String getSerializedName() {
			return name;
		}
	}

	/**
	 * A spot's state with what that state needs: an occupied spot always names its occupant, and no other spot has one.
	 * Saved in the spot's own fields, under {@code state}.
	 */
	public sealed interface Occupancy {
		/** Read with the rest of a spot's fields. An occupied spot saved without its occupant is an error. */
		MapCodec<Occupancy> CODEC = State.CODEC.dispatchMap("state", Occupancy::state, state -> switch (state) {
			case OCCUPIED -> Occupied.CODEC;
			case EMPTY -> MapCodec.unit(Vacant.EMPTY);
			case PENDING -> MapCodec.unit(Vacant.PENDING);
		});

		State state();
	}

	/**
	 * A mob holds the spot.
	 *
	 * @param savedChunk the chunk of the spot's level the occupant is saved in, packed, where that's known: not while
	 *     it's in another dimension or saved with a player
	 */
	public record Occupied(UUID occupant, Optional<Long> savedChunk) implements Occupancy {
		static final MapCodec<Occupied> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("occupant").forGetter(Occupied::occupant),
			Codec.LONG.optionalFieldOf("occupant_chunk").forGetter(Occupied::savedChunk)
		).apply(instance, Occupied::new));

		@Override
		public State state() {
			return State.OCCUPIED;
		}
	}

	/** No mob holds the spot. */
	public enum Vacant implements Occupancy {
		EMPTY(State.EMPTY),
		PENDING(State.PENDING);

		private final State state;

		Vacant(State state) {
			this.state = state;
		}

		@Override
		public State state() {
			return state;
		}
	}
}

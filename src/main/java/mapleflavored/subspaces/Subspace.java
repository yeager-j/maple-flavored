package mapleflavored.subspaces;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * A boss's Subspace: a fixed sphere that stays where the boss opened it. The level that holds it is its dimension.
 *
 * @param id unique to this opening, even when the same boss opens another Subspace where it stood
 * @param boss the UUID of the boss that opened it
 * @param centre where the boss stood when it opened it
 * @param radius in blocks
 */
public record Subspace(UUID id, UUID boss, Kind kind, Vec3 centre, double radius) {
	/** How a Subspace is saved with its level. */
	public static final Codec<Subspace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(Subspace::id),
		UUIDUtil.CODEC.fieldOf("boss").forGetter(Subspace::boss),
		Kind.CODEC.fieldOf("kind").forGetter(Subspace::kind),
		Vec3.CODEC.fieldOf("centre").forGetter(Subspace::centre),
		Codec.DOUBLE.fieldOf("radius").forGetter(Subspace::radius)
	).apply(instance, Subspace::new));

	static final StreamCodec<ByteBuf, Subspace> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, Subspace::id,
		UUIDUtil.STREAM_CODEC, Subspace::boss,
		Kind.STREAM_CODEC, Subspace::kind,
		Vec3.STREAM_CODEC, Subspace::centre,
		ByteBufCodecs.DOUBLE, Subspace::radius,
		Subspace::new
	);

	/** Whether the position is inside the sphere, its surface included. */
	public boolean contains(Vec3 position) {
		return centre.distanceToSqr(position) <= radius * radius;
	}

	/** Which boss opened a Subspace, which decides its rules. */
	public enum Kind {
		/** Darkness and Grounded for players inside, and its Warden is aware of everything inside. */
		WARDEN,

		/** Withered Hearts can't return inside. It gives no effect of its own. */
		WITHER;

		static final Codec<Kind> CODEC = Codec.STRING.xmap(Kind::valueOf, Kind::name);
		static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Kind::valueOf, Kind::name);
	}
}

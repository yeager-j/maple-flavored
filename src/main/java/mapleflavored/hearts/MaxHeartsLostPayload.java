package mapleflavored.hearts;

import mapleflavored.MapleFlavored;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client how many Max Hearts its player lost on the death that just happened, for the death screen. */
public record MaxHeartsLostPayload(int lost) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MaxHeartsLostPayload> TYPE = new CustomPacketPayload.Type<>(
		MapleFlavored.id("max_hearts_lost")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, MaxHeartsLostPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, MaxHeartsLostPayload::lost,
		MaxHeartsLostPayload::new
	);

	@Override
	public Type<MaxHeartsLostPayload> type() {
		return TYPE;
	}
}

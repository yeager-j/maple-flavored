package mapleflavored.items;

import mapleflavored.MapleFlavored;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client its player just consumed a Crystal Heart, so it plays the Totem's activation with the heart. */
public record CrystalHeartUsedPayload() implements CustomPacketPayload {
	public static final CrystalHeartUsedPayload INSTANCE = new CrystalHeartUsedPayload();

	public static final CustomPacketPayload.Type<CrystalHeartUsedPayload> TYPE = new CustomPacketPayload.Type<>(
		MapleFlavored.id("crystal_heart_used")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, CrystalHeartUsedPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<CrystalHeartUsedPayload> type() {
		return TYPE;
	}
}

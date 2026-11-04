package mapleflavored.zephyr;

import mapleflavored.MapleFlavored;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the server its player just made a Zephyr jump, so the server can check it and apply its fall rules. */
public record ZephyrJumpPayload() implements CustomPacketPayload {
	public static final ZephyrJumpPayload INSTANCE = new ZephyrJumpPayload();

	public static final CustomPacketPayload.Type<ZephyrJumpPayload> TYPE = new CustomPacketPayload.Type<>(
		MapleFlavored.id("zephyr_jump")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, ZephyrJumpPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<ZephyrJumpPayload> type() {
		return TYPE;
	}
}

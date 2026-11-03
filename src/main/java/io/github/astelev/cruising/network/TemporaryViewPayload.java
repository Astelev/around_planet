package io.github.astelev.cruising.network;

import io.github.astelev.cruising.CruisingSpeed;
import io.github.astelev.cruising.client.ClientFlightView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record TemporaryViewPayload(ResourceLocation dimension, boolean active, int x, int z, int radius)
        implements CustomPacketPayload {
    public static final Type<TemporaryViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CruisingSpeed.MODID, "temporary_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TemporaryViewPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeResourceLocation(value.dimension);
                buf.writeBoolean(value.active);
                buf.writeInt(value.x);
                buf.writeInt(value.z);
                buf.writeVarInt(value.radius);
            },
            buf -> new TemporaryViewPayload(buf.readResourceLocation(), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readVarInt()));

    public TemporaryViewPayload {
        if (radius < 0 || radius > 32) {
            throw new IllegalArgumentException("Invalid temporary view radius");
        }
    }

    @Override
    public Type<TemporaryViewPayload> type() {
        return TYPE;
    }

    public static final class Registration {
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToClient(TYPE, CODEC,
                    (payload, context) -> context.enqueueWork(() -> ClientFlightView.accept(payload)));
        }
    }
}

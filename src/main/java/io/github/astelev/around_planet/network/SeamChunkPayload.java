package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamImages;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded read-only visual snapshot, not a normal client chunk packet. */
public record SeamChunkPayload(ResourceLocation dimension, int x, int z, byte[] data) implements CustomPacketPayload {
    public static final int MAX_BYTES = 900000;
    public static final Type<SeamChunkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AroundPlanet.MOD_ID, "seam_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SeamChunkPayload> CODEC = StreamCodec.of(
        (b,p) -> { b.writeResourceLocation(p.dimension); b.writeInt(p.x); b.writeInt(p.z); b.writeByteArray(p.data); },
        b -> new SeamChunkPayload(b.readResourceLocation(), b.readInt(), b.readInt(), b.readByteArray(MAX_BYTES)));
    public SeamChunkPayload { if(data.length > MAX_BYTES) throw new IllegalArgumentException("Oversize seam snapshot"); }
    @Override public Type<SeamChunkPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, (p,c) -> c.enqueueWork(() -> SeamImages.accept(p)));
    }
}

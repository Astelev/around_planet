package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamEntityImages;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** A complete, bounded visual frame; never dispatched to the native entity packet listener. */
public record SeamEntitiesPayload(ResourceLocation dimension,int shift,byte[] data) implements CustomPacketPayload {
    public static final int MAX_BYTES=524288, MAX_ENTITIES=128, MAX_ENTRY_BYTES=65536, INTERVAL=4;
    public static final Type<SeamEntitiesPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(AroundPlanet.MOD_ID,"seam_entities"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SeamEntitiesPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeResourceLocation(p.dimension);b.writeInt(p.shift);b.writeByteArray(p.data);},
            b->new SeamEntitiesPayload(b.readResourceLocation(),b.readInt(),b.readByteArray(MAX_BYTES)));
    public SeamEntitiesPayload {if(data.length>MAX_BYTES)throw new IllegalArgumentException("Oversize entity frame");}
    @Override public Type<SeamEntitiesPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->SeamEntityImages.accept(p)));
    }
}

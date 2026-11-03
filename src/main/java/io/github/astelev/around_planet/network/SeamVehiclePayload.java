package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.SeamVehicleCorrection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** One authoritative pose, also applied if the vehicle's native spawn arrives after the player teleport. */
public record SeamVehiclePayload(ResourceLocation dimension,int id,UUID uuid,double x,double y,double z,float yaw,float pitch) implements CustomPacketPayload {
    public static final Type<SeamVehiclePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(AroundPlanet.MOD_ID,"seam_vehicle"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SeamVehiclePayload> CODEC=StreamCodec.of(
        (b,p)->{b.writeResourceLocation(p.dimension);b.writeVarInt(p.id);b.writeUUID(p.uuid);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeFloat(p.yaw);b.writeFloat(p.pitch);},
        b->new SeamVehiclePayload(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat()));
    @Override public Type<SeamVehiclePayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->SeamVehicleCorrection.accept(p)));
    }
}

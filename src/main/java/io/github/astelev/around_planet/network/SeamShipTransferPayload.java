package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.ShipTransferClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
/** Reliable history rebasing before passenger position packets; plot identity and buffers remain native. */
public record SeamShipTransferPayload(ResourceLocation dimension,long serial,int width,double dx,List<Ship> ships) implements CustomPacketPayload {
    public record Ship(UUID id,double oldX){}
    public static final int MAX_SHIPS=256;
    public static final Type<SeamShipTransferPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(AroundPlanet.MOD_ID,"ship_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SeamShipTransferPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeResourceLocation(p.dimension);b.writeLong(p.serial);b.writeInt(p.width);b.writeDouble(p.dx);b.writeVarInt(p.ships.size());
        for(var s:p.ships){b.writeUUID(s.id);b.writeDouble(s.oldX);}
    },b->{var dimension=b.readResourceLocation();long serial=b.readLong();int width=b.readInt();double dx=b.readDouble();int count=b.readVarInt();
        if(count<0 || count>MAX_SHIPS)throw new IllegalArgumentException("Ship group size");var ships=new ArrayList<Ship>();
        for(int i=0;i<count;i++)ships.add(new Ship(b.readUUID(),b.readDouble()));return new SeamShipTransferPayload(dimension,serial,width,dx,List.copyOf(ships));});
    @Override public Type<SeamShipTransferPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("3").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->ShipTransferClient.accept(p)));}
}

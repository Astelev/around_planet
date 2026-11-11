package io.github.astelev.around_planet.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;
import io.github.astelev.around_planet.space.*;

/** Small server-authoritative HUD sample, four times per second, only for occupied space cells. */
@EventBusSubscriber(modid="around_planet")
public record SpaceStatusPayload(boolean active,double x,double y,double z,double vx,double vy,double vz,double altitude,boolean returning,double radius,double mu,double returnAltitude,SpaceFrame frame,SpaceVector anchor) implements CustomPacketPayload {
    public static final Type<SpaceStatusPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","space_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpaceStatusPayload> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.active);for(double n:new double[]{p.x,p.y,p.z,p.vx,p.vy,p.vz,p.altitude})b.writeDouble(n);b.writeBoolean(p.returning);b.writeDouble(p.radius);b.writeDouble(p.mu);b.writeDouble(p.returnAltitude);for(var v:new SpaceVector[]{p.frame.east(),p.frame.up(),p.frame.south(),p.anchor}){b.writeDouble(v.x());b.writeDouble(v.y());b.writeDouble(v.z());}},b->new SpaceStatusPayload(b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),new SpaceFrame(vector(b),vector(b),vector(b)),vector(b)));
    private static SpaceVector vector(RegistryFriendlyByteBuf b){return new SpaceVector(b.readDouble(),b.readDouble(),b.readDouble());}
    private static final Set<ServerPlayer> SENT=Collections.newSetFromMap(new WeakHashMap<>());
    @Override public Type<SpaceStatusPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("3").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.SpaceHud.accept(p)));}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) || level.getGameTime()%5!=0 || !net.neoforged.fml.ModList.get().isLoaded("sable"))return;
        for(var player:level.players())if(player.connection.hasChannel(TYPE)) {
            var packet=io.github.astelev.around_planet.compat.SpaceTelemetry.sample(player);
            if(packet.active()){SENT.add(player);PacketDistributor.sendToPlayer(player,packet);}else if(SENT.remove(player))PacketDistributor.sendToPlayer(player,packet);
        }
    }
}

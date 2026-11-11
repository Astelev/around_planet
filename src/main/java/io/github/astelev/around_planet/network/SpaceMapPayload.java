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
/** Bounded saved-orbit catalog, only while a player subscribes by opening the map. Never loads ships/chunks. */
@EventBusSubscriber(modid="around_planet")
public record SpaceMapPayload(UUID focus,int total,List<Body> bodies) implements CustomPacketPayload {
    public record Body(UUID id,String name,double x,double y,double z,double vx,double vy,double vz,double mu,double returnAltitude,boolean waiting,boolean loaded){}
    public static final Type<SpaceMapPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","space_map_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpaceMapPayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.focus);b.writeVarInt(p.total);b.writeVarInt(p.bodies.size());for(var o:p.bodies){b.writeUUID(o.id);b.writeUtf(o.name,96);for(double n:new double[]{o.x,o.y,o.z,o.vx,o.vy,o.vz,o.mu,o.returnAltitude})b.writeDouble(n);b.writeBoolean(o.waiting);b.writeBoolean(o.loaded);}},b->{var focus=b.readUUID();int total=b.readVarInt(),size=b.readVarInt();if(total<0 || size<0 || size>128)throw new IllegalArgumentException("Orbit catalog size");var list=new ArrayList<Body>();for(int i=0;i<size;i++)list.add(new Body(b.readUUID(),b.readUtf(96),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean(),b.readBoolean()));return new SpaceMapPayload(focus,total,List.copyOf(list));});
    public record Request(boolean open) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","space_map_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->b.writeBoolean(p.open),b->new Request(b.readBoolean()));
        @Override public Type<Request> type(){return TYPE;}
    }
    private static final Map<ServerPlayer,Long> SUBSCRIBERS=new WeakHashMap<>();
    @Override public Type<SpaceMapPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1").optional();r.playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.SpaceMapCatalog.accept(p)));
        r.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->{var player=(ServerPlayer)c.player();if(p.open && net.neoforged.fml.ModList.get().isLoaded("sable"))SUBSCRIBERS.put(player,player.serverLevel().getGameTime()+200);else SUBSCRIBERS.remove(player);}));
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) || level.getGameTime()%20!=0 || !net.neoforged.fml.ModList.get().isLoaded("sable"))return;
        for(var p:level.players())if(SUBSCRIBERS.containsKey(p)){
            if(SUBSCRIBERS.get(p)<level.getGameTime() || !io.github.astelev.around_planet.space.SpaceCells.contains(p) || !p.connection.hasChannel(TYPE)){SUBSCRIBERS.remove(p);continue;}
            var packet=io.github.astelev.around_planet.compat.SpaceMapSnapshot.sample(p);if(packet!=null)PacketDistributor.sendToPlayer(p,packet);
        }
    }
}

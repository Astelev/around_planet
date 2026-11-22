package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.space.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
/** Four-Hz target telemetry remains available with the map closed; never wakes target ships. */
@EventBusSubscriber(modid="around_planet")
public record RendezvousPayload(UUID focus,UUID target,String name,long now,SpaceState own,SpaceState other,double mu) implements CustomPacketPayload {
    public static final Type<RendezvousPayload> TYPE=new Type<>(ResourceLocation.parse("around_planet:rendezvous_state"));
    public boolean active(){return !target.equals(ManeuverPayload.NONE);}
    public static final StreamCodec<RegistryFriendlyByteBuf,RendezvousPayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.focus);b.writeUUID(p.target);b.writeUtf(p.name,96);b.writeLong(p.now);for(var s:new SpaceState[]{p.own,p.other})for(var v:new SpaceVector[]{s.position(),s.velocity()}){b.writeDouble(v.x());b.writeDouble(v.y());b.writeDouble(v.z());}b.writeDouble(p.mu);},b->new RendezvousPayload(b.readUUID(),b.readUUID(),b.readUtf(96),b.readLong(),new SpaceState(vector(b),vector(b)),new SpaceState(vector(b),vector(b)),b.readDouble()));
    private static SpaceVector vector(RegistryFriendlyByteBuf b){return new SpaceVector(b.readDouble(),b.readDouble(),b.readDouble());}
    @Override public Type<RendezvousPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.RendezvousClient.accept(p)));}
    public static void send(ServerPlayer p){if(net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.RendezvousTelemetry.send(p);}

    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post e){if(!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||level.getGameTime()%5!=0||!net.neoforged.fml.ModList.get().isLoaded("sable"))return;for(var p:level.players())if(SpaceCells.contains(p))send(p);}
}

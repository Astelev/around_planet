package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.space.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
/** Ephemeris parameters, independent of ship membership. Changes on warp/settings, not every frame. */
@EventBusSubscriber(modid="around_planet")
public record CelestialSkyPayload(CelestialSystem system) implements CustomPacketPayload {
 public static final Type<CelestialSkyPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","celestial_sky"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CelestialSkyPayload> CODEC=StreamCodec.of((b,p)->{b.writeDouble(p.system.earthRadius());b.writeDouble(p.system.earthMu());b.writeLong(p.system.clockTicks());b.writeDouble(p.system.earthEntry());},b->new CelestialSkyPayload(new CelestialSystem(b.readDouble(),b.readDouble(),b.readLong(),b.readDouble())));
 private record Sent(ServerLevel level,CelestialSystem system,long tick){}
 private static final Map<ServerPlayer,Sent> SENT=new WeakHashMap<>();
 @Override public Type<CelestialSkyPayload> type(){return TYPE;}
 public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").optional().playToClient(TYPE,CODEC,(packet,context)->context.enqueueWork(()->io.github.astelev.around_planet.client.CelestialSky.accept(packet)));}
 public static CelestialSkyPayload sample(ServerLevel level){var sys=SpaceOrbitData.get(level.getServer()).system();return sys==null?null:new CelestialSkyPayload(sys);}
 @SubscribeEvent public static void tick(LevelTickEvent.Post event){if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%5!=0)return;var packet=sample(level);if(packet==null)return;long now=level.getGameTime();for(var player:level.players())if(player.connection.hasChannel(TYPE)){var old=SENT.get(player);if(old==null||old.level()!=level||!old.system().equals(packet.system)||now-old.tick()>=100||now<old.tick()){PacketDistributor.sendToPlayer(player,packet);SENT.put(player,new Sent(level,packet.system,now));}}}
 @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){SENT.clear();}
}

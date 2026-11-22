package io.github.astelev.around_planet.network;
import io.github.astelev.around_planet.moon.*;
import io.github.astelev.around_planet.space.MoonSurface;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import java.util.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet")
public record MoonTerrainPayload(double circumference,long seed) implements CustomPacketPayload {
 public static final Type<MoonTerrainPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("around_planet","moon_terrain"));
 public static final StreamCodec<RegistryFriendlyByteBuf,MoonTerrainPayload> CODEC=StreamCodec.of((b,p)->{b.writeDouble(p.circumference);b.writeLong(p.seed);},b->new MoonTerrainPayload(b.readDouble(),b.readLong()));
 public MoonTerrainPayload {new MoonTerrain(circumference,seed);}
 @Override public Type<MoonTerrainPayload> type(){return TYPE;}
 public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").optional().playToClient(TYPE,CODEC,(p,c)->c.enqueueWork(()->io.github.astelev.around_planet.client.MoonAtlasTexture.accept(p)));}
 private static final Map<ServerPlayer,MoonTerrainPayload> SENT=new WeakHashMap<>();
 @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post e){if(!(e.getLevel() instanceof ServerLevel l)||!l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||l.getGameTime()%20!=0)return;var moon=l.getServer().getLevel(MoonSurface.dimension());if(moon==null||!(moon.getChunkSource().getGenerator() instanceof MoonChunkGenerator generator))return;var t=generator.terrain();var packet=new MoonTerrainPayload(t.circumference(),t.seed());for(var player:l.getServer().getPlayerList().getPlayers())if(player.connection.hasChannel(TYPE)&&!packet.equals(SENT.get(player))){net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,packet);SENT.put(player,packet);}}
 @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SENT.clear();}
}

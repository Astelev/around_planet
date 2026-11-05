package io.github.astelev.around_planet.seam;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.network.SeamChunkPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Reads only chunks already loaded by the existing transition preload. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class SeamStreamer {
    private record Sent(int hash,long time) {}
    private static final Map<UUID,Map<Long,Sent>> SENT = new HashMap<>();
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        var topology = PlanetBorders.get(level.getWorldBorder());
        if(topology == null) return;
        int half = topology.width()/32, radius = PlanetConfig.RADIUS.getAsInt();
        for(var player : level.players()) {
            if(!player.connection.hasChannel(SeamChunkPayload.TYPE)) continue;
            if(topology.distanceToSeam(player.getX()) > Math.max(PlanetConfig.ACTIVATION.getAsInt(), (radius+3)*16)) {
                SENT.remove(player.getUUID()); continue;
            }
            var sent = SENT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            Set<Long> desired = new HashSet<>(); int budget = 2;
            boolean east = player.getX() >= 0;
            int visualRange=Math.max(Math.min(player.requestedViewDistance(),level.getServer().getPlayerList().getViewDistance())+2,radius+3);
            for(int d=0; d<=radius+1; d++) for(int dz=-radius-1; dz<=radius+1; dz++) {
                int sourceX = east ? -half+d : half-1-d;
                int imageX = sourceX+(east ? half*2 : -half*2), z = player.chunkPosition().z+dz;
                if(Math.abs(imageX-player.chunkPosition().x)>visualRange) continue;
                long key = ChunkPos.asLong(imageX,z); desired.add(key);
                var previous = sent.get(key);
                if(budget == 0 || previous != null && level.getGameTime()-previous.time < 40) continue;
                var chunk = level.getChunkSource().getChunkNow(sourceX,z);
                if(chunk == null || !chunk.isLightCorrect()) continue;
                budget--;
                var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
                try {
                    var packet = new ClientboundLevelChunkWithLightPacket(chunk,level.getChunkSource().getLightEngine(),null,null);
                    ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(buf,packet);
                    if(buf.readableBytes() > SeamChunkPayload.MAX_BYTES) {
                        if(previous==null) AroundPlanet.LOGGER.warn("Visual chunk {} exceeds the bounded seam packet size",chunk.getPos());
                        sent.put(key,new Sent(0,level.getGameTime())); continue;
                    }
                    byte[] data = new byte[buf.readableBytes()]; buf.readBytes(data);
                    int hash = Arrays.hashCode(data);
                    if(previous == null || previous.hash != hash) PacketDistributor.sendToPlayer(player,
                        new SeamChunkPayload(level.dimension().location(),imageX,z,data));
                    sent.put(key,new Sent(hash,level.getGameTime()));
                } finally { buf.release(); }
            }
            sent.keySet().retainAll(desired);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SENT.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { SENT.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { SENT.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { SENT.clear(); }
    private SeamStreamer() {}
}

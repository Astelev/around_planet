package io.github.astelev.around_planet.seam;

import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
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
    private record Source(int imageX,int z,net.minecraft.world.level.chunk.LevelChunk chunk) {}
    private static final Map<UUID,Map<Long,Sent>> SENT = new HashMap<>();
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        var topology = PlanetBorders.get(level.getWorldBorder());
        if(topology == null) return;
        int half = topology.width()/32, radius = PlanetConfig.RADIUS.getAsInt();
        for(var player : level.players()) {
            if(!player.connection.hasChannel(SeamChunkPayload.TYPE)) continue;
            if(!topology.outside(player.getX()) && topology.distanceToSeam(player.getX()) > io.github.astelev.around_planet.transfer.TransferService.approachDistance(player,topology)) {
                SENT.remove(player.getUUID()); continue;
            }
            var sent = SENT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            Set<Long> desired = new HashSet<>();
            Map<Long,Source> sources=new HashMap<>();
            List<SnapshotSchedule.Candidate> candidates=new ArrayList<>();
            boolean east = player.getX() >= 0;
            int visualRange=Math.max(Math.min(player.requestedViewDistance(),level.getServer().getPlayerList().getViewDistance())+2,radius+3);
            int depth=east?player.chunkPosition().x-half:-half-1-player.chunkPosition().x;
            int first=Math.max(0,depth-radius-1),last=Math.max(radius+1,depth+radius+1);
            for(int d=first; d<=last; d++) for(int dz=-radius-1; dz<=radius+1; dz++) {
                int imageX=east?half+d:-half-1-d,sourceX=SeamGeometry.sourceX(topology,imageX),z=player.chunkPosition().z+dz;
                if(Math.abs(imageX-player.chunkPosition().x)>visualRange) continue;
                long key = ChunkPos.asLong(imageX,z); desired.add(key);
                var chunk=level.getChunkSource().getChunkNow(sourceX,z);
                if(chunk==null){SeamDiagnostics.count("server","stream.missing");continue;}
                if(!chunk.isLightCorrect()){SeamDiagnostics.count("server","stream.unlit");continue;}
                SeamDiagnostics.count("server","stream.ready");
                var previous=sent.get(key);
                sources.put(key,new Source(imageX,z,chunk));
                double dx=imageX-player.chunkPosition().x;
                candidates.add(new SnapshotSchedule.Candidate(key,previous==null?Long.MIN_VALUE:previous.time,dx*dx+dz*dz));
            }
            for(var next:SnapshotSchedule.choose(candidates,level.getGameTime(),
                    PlanetConfig.SNAPSHOT_INTERVAL.getAsInt(),PlanetConfig.SNAPSHOT_BUDGET.getAsInt())) {
                SeamDiagnostics.count("server","stream.serialized");long diagnosticStart=SeamDiagnostics.start();long key=next.key();var source=sources.get(key);var previous=sent.get(key);
                var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
                try {
                    var packet = new ClientboundLevelChunkWithLightPacket(source.chunk,level.getChunkSource().getLightEngine(),null,null);
                    ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(buf,packet);
                    if(buf.readableBytes() > SeamChunkPayload.MAX_BYTES) {
                        if(previous==null) AroundPlanet.LOGGER.warn("Visual chunk {} exceeds the bounded seam packet size",source.chunk.getPos());
                        sent.put(key,new Sent(0,level.getGameTime())); continue;
                    }
                    byte[] data = new byte[buf.readableBytes()]; buf.readBytes(data);
                    int hash = Arrays.hashCode(data);
                    if(previous == null || previous.hash != hash) {
                        SeamDiagnostics.count("server",previous==null?"stream.sent_new":"stream.sent_changed");SeamDiagnostics.add("server","stream.sent_bytes",data.length);
                        PacketDistributor.sendToPlayer(player,new SeamChunkPayload(level.dimension().location(),source.imageX,source.z,data));
                    } else SeamDiagnostics.count("server","stream.unchanged");
                    sent.put(key,new Sent(hash,level.getGameTime()));
                } finally { buf.release();SeamDiagnostics.time("server","stream.serialize",diagnosticStart); }
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

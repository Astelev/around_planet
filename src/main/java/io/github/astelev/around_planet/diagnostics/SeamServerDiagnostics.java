package io.github.astelev.around_planet.diagnostics;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class SeamServerDiagnostics {
    private static long tickStart;
    @SubscribeEvent public static void pre(ServerTickEvent.Pre e){tickStart=SeamDiagnostics.start();SeamServerStallWatch.begin(tickStart);}
    @SubscribeEvent public static void post(ServerTickEvent.Post e) {
        SeamServerStallWatch.end();SeamDiagnostics.time("server","tick",tickStart);if(!SeamDiagnostics.due("server"))return;
        var text=new StringBuilder(SeamDiagnostics.config());int n=0;
        for(var p:e.getServer().getPlayerList().getPlayers()) {
            if(n++>=4)break;var t=PlanetBorders.get(p.level().getWorldBorder());if(t==null)continue;
            int ready=0,unlit=0,missing=0,cx=p.chunkPosition().x,cz=p.chunkPosition().z;
            for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++) {
                var c=p.serverLevel().getChunkSource().getChunkNow(x,z);if(c==null)missing++;else if(!c.isLightCorrect())unlit++;else ready++;
            }
            text.append(" player{id=").append(p.getId()).append(" dim=").append(p.level().dimension().location()).append(" xyz=").append(p.position())
                    .append(" seamDistance=").append(t.distanceToSeam(p.getX())).append(" knownMotion=").append(p.getKnownMovement())
                    .append(" local5x5[ready/unlit/missing]=").append(ready).append('/').append(unlit).append('/').append(missing)
                    .append(" space=").append(io.github.astelev.around_planet.space.SpaceCells.contains(p))
                    .append(" loaded=").append(p.serverLevel().getChunkSource().getLoadedChunksCount()).append(' ').append(TransferService.diagnostics(p)).append('}');
        }
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))text.append(" spaceCells=").append(io.github.astelev.around_planet.space.SpaceCells.get(e.getServer().overworld()).all().stream().limit(8).map(c->c.id()+"@("+c.x()+","+c.y()+","+c.z()+")").toList());
        text.append(" dhQueues=").append(SeamDiagnostics.queues());SeamDiagnostics.report("server",text.toString());
    }
    @SubscribeEvent public static void load(ChunkEvent.Load e){chunk(e,true);}
    @SubscribeEvent public static void unload(ChunkEvent.Unload e){chunk(e,false);}
    private static void chunk(ChunkEvent e,boolean load) {
        if(!(e.getLevel() instanceof ServerLevel level) || !SeamDiagnostics.enabled())return;
        SeamDiagnostics.count("server",load?"chunk.load":"chunk.unload");var topology=PlanetBorders.get(level.getWorldBorder());var pos=e.getChunk().getPos();
        if(topology!=null && topology.distanceToSeam(pos.getMiddleBlockX())<512)
            SeamDiagnostics.event("server",load?"chunk.near_load":"chunk.near_unload",()->"dim="+level.dimension().location()+" pos="+pos+" status="+e.getChunk().getPersistedStatus());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SeamServerStallWatch.close();if(SeamDiagnostics.enabled())SeamDiagnostics.report("server","server_stopped");}
}

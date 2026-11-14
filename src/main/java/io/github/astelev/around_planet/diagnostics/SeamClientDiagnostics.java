package io.github.astelev.around_planet.diagnostics;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.client.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.List;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamClientDiagnostics {
    public static int cacheX,cacheZ,cacheRadius;
    private static double maxCruisingSpeed;private static boolean lastSpace;
    private static Object world;private static double lastX=Double.NaN;private static long tickStart,lastTick;
    @SubscribeEvent public static void pre(ClientTickEvent.Pre e){tickStart=SeamDiagnostics.start();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        SeamDiagnostics.time("client","tick",tickStart);var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null)return;
        CpuLoadDiagnostics.poll();
        var topology=PlanetBorders.get(mc.level.getWorldBorder());if(topology==null)return;
        if(world!=mc.level) {
            world=mc.level;lastX=Double.NaN;lastSpace=false;
            SeamDiagnostics.event("client","session.start",()->"java="+System.getProperty("java.version")+" "+mods()+" "+SeamDiagnostics.config());
        }
        boolean space=SpaceRenderView.active();if(space!=lastSpace){lastSpace=space;SeamDiagnostics.event("client",space?"space.enter":"space.exit",()->"xyz="+mc.player.position()+" cache="+cacheX+","+cacheZ+" loaded="+mc.level.getChunkSource().getLoadedChunksCount()+" view="+mc.options.getEffectiveRenderDistance());}
        double speed=CruisingDiagnostics.cameraSpeed();if(Double.isFinite(speed))maxCruisingSpeed=Math.max(maxCruisingSpeed,speed);
        double x=mc.player.getX();
        if(Double.isFinite(lastX) && Math.abs(x-lastX)>topology.width()/2.0) {
            double old=lastX;SeamDiagnostics.event("client","crossing",()->"fromX="+old+" to="+mc.player.position()+" previousTickGapMs="+(System.nanoTime()-lastTick)/1e6+" images="+SeamImages.size()+" "+SeamMeshCache.diagnostic());
        }
        lastX=x;lastTick=System.nanoTime();if(!SeamDiagnostics.due("client"))return;
        int surfaceReady=0,surfaceMissing=0,surfaceRetained=0;var holes=new StringBuilder();
        int data=0,missing=0,cy=mc.player.blockPosition().getY()>>4,cx=mc.player.chunkPosition().x,cz=mc.player.chunkPosition().z,ready=0,retained=0;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
            var chunk=SeamSectionCoverage.data(mc.level,cx+dx,cz+dz);
            if(chunk==null)missing++;else {
                data++;int sampled=0;
                for(int section=chunk.getSectionsCount()-1;section>=0 && sampled<3;section--)if(!chunk.getSection(section).hasOnlyAir()) {
                    sampled++;int y=mc.level.getMinSection()+section;
                    boolean nativeReady=SeamSectionCoverage.ready(mc.level,cx+dx,y,cz+dz,null);
                    int canonical=(int)Math.floor(topology.canonicalX((cx+dx)*16.0)/16);
                    boolean retainedReady=SeamMeshCache.hasMesh(canonical,y,cz+dz);
                    if(nativeReady)surfaceReady++;else if(retainedReady)surfaceRetained++;else {surfaceMissing++;if(holes.length()<180)holes.append('[').append(cx+dx).append(',').append(y).append(',').append(cz+dz).append(']');}
                }
            }
            for(int y=cy-2;y<=cy;y++) {if(SeamSectionCoverage.ready(mc.level,cx+dx,y,cz+dz,null))ready++;
                int canonical=(int)Math.floor(topology.canonicalX((cx+dx)*16.0)/16);if(SeamMeshCache.hasMesh(canonical,y,cz+dz))retained++;}
        }
        String dh="absent";
        if(ModList.get().isLoaded("distanthorizons"))try{dh=io.github.astelev.around_planet.compat.DhSeamViews.diagnostic();}
        catch(RuntimeException | LinkageError failure){dh="probe_error:"+failure.getClass().getSimpleName();}
        var runtime=Runtime.getRuntime();
        SeamDiagnostics.report("client","dim="+mc.level.dimension().location()+" level="+SeamDiagnostics.id(mc.level)+" xyz="+mc.player.position()+" seamDistance="+topology.distanceToSeam(x)
                +" space="+SpaceRenderView.active()+" spaceAnchor="+(SpaceHud.current()==null?"none":SpaceHud.current().anchor())+" fps="+mc.getFps()+" paused="+mc.isPaused()+" view="+mc.options.getEffectiveRenderDistance()+" viewRequested="+mc.options.renderDistance().get()+" nativeCache="+cacheX+","+cacheZ+" radius="+cacheRadius
                +" heapMiB="+(runtime.totalMemory()-runtime.freeMemory())/1048576+"/"+runtime.maxMemory()/1048576
                +" height=["+mc.level.getMinBuildHeight()+","+mc.level.getMaxBuildHeight()+"] sections="+mc.level.getSectionsCount()+" loaded="+mc.level.getChunkSource().getLoadedChunksCount()+" local5x5[data/missing]="+data+"/"+missing+" nearFeetSections[ready/retained]="+ready+"/"+retained
                +" surfaceSections[native/retained/missing]="+surfaceReady+"/"+surfaceRetained+"/"+surfaceMissing+" notReadySample="+holes+" images="+SeamImages.diagnostic()+" "+SeamMeshCache.diagnostic()+" clientGenQueues="+(mc.getSingleplayerServer()==null?SeamDiagnostics.queues():"see_server_summary")+" DH{"+dh+"} Cruising{maxCameraSpeed="+maxCruisingSpeed+" "+CruisingDiagnostics.snapshot()+"}");
        maxCruisingSpeed=0;
    }
    private static String mods(){var s=new StringBuilder("mods{");for(var id:List.of("around_planet","sodium","embeddium","distanthorizons","cruising","sable","simpleclouds","connector","fabric_api","fabric_biome_api_v1"))ModList.get().getModContainerById(id).ifPresent(m->s.append(id).append('=').append(m.getModInfo().getVersion()).append(' '));return s.append('}').toString();}
    @SubscribeEvent public static void unload(LevelEvent.Unload e){if(e.getLevel()==world){SeamDiagnostics.event("client","level.unload",()->"level="+SeamDiagnostics.id(world));world=null;lastX=Double.NaN;}}
}

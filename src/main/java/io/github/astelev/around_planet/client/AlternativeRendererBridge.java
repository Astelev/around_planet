package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Renderer-local readiness only: no fake chunks or load events enter the gameplay cache. */
public final class AlternativeRendererBridge {
    private static final Set<Long> MARKED=new HashSet<>();
    private static ClientLevel world;
    private static boolean initialized,unavailable;
    private static Method getTracker,added,removed;
    private static String prefix;
    private static boolean initialize() {
        if(initialized)return getTracker!=null && !unavailable;
        initialized=true;
        prefix=ModList.get().isLoaded("sodium")?"net.caffeinemc.mods.sodium.client":
                ModList.get().isLoaded("embeddium")?"org.embeddedt.embeddium.impl":null;
        if(prefix==null)return false;
        try {
            getTracker=Class.forName(prefix+".render.chunk.map.ChunkTrackerHolder").getMethod("get",ClientLevel.class);
            var tracker=Class.forName(prefix+".render.chunk.map.ChunkTracker");
            added=tracker.getMethod("onChunkStatusAdded",int.class,int.class,int.class);
            removed=tracker.getMethod("onChunkStatusRemoved",int.class,int.class,int.class);
            AroundPlanet.LOGGER.info("Seam renderer adapter active: {}",prefix);
            return true;
        }catch(ReflectiveOperationException e){unavailable=true;AroundPlanet.LOGGER.error("Unsupported alternative renderer seam API",e);return false;}
    }
    public static void changed(ClientLevel level) {
        if(!initialize())return;
        if(world!=level){MARKED.clear();world=level;}
        try {
            Object tracker=getTracker.invoke(null,level);
            Set<Long> needed=new HashSet<>();
            for(var pos:SeamImages.positions(level))for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(SeamGeometry.clipped(level,pos.x+dx,pos.z+dz))needed.add(ChunkPos.asLong(pos.x+dx,pos.z+dz));
            // Empty renderer-only padding supplies the neighbor ring required by Sodium's visibility graph.
            for(long key:MARKED)if(!needed.contains(key))removed.invoke(tracker,ChunkPos.getX(key),ChunkPos.getZ(key),3);
            for(long key:needed)if(!MARKED.contains(key))added.invoke(tracker,ChunkPos.getX(key),ChunkPos.getZ(key),3);
            MARKED.clear();MARKED.addAll(needed);
        }catch(ReflectiveOperationException e){unavailable=true;AroundPlanet.LOGGER.error("Alternative renderer seam readiness failed",e);}
    }
    public static void clear(ClientLevel level){if(world==level){MARKED.clear();world=null;}}
    public static Boolean isReady(int x,int y,int z) {
        if(!initialize())return null;
        try {
            String name=prefix.startsWith("net.")?"SodiumWorldRenderer":"EmbeddiumWorldRenderer";
            var type=Class.forName(prefix+".render."+name);var renderer=type.getMethod("instanceNullable").invoke(null);
            return renderer!=null && (Boolean)type.getMethod("isSectionReady",int.class,int.class,int.class).invoke(renderer,x,y,z);
        }catch(ReflectiveOperationException e){return false;}
    }
    private AlternativeRendererBridge() {}
}

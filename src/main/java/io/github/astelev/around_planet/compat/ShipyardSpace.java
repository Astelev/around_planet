package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Sable uses the same renderer for private plot coordinates, which are not geographical terrain. */
public final class ShipyardSpace {
    private record Bounds(long minX,long minZ,long maxX,long maxZ) {
        boolean contains(int x,int z) {return x>=minX && x<maxX && z>=minZ && z<maxZ;}
    }
    private static final Map<Level,Bounds> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean unavailable;
    public static boolean reserved(Level level,int x,int z) {
        if(!ModList.get().isLoaded("sable"))return false;
        if(unavailable)return true; // Never hide a ship because a newer private API cannot be identified.
        var bounds=CACHE.get(level);
        if(bounds==null) {
            try {
                var type=Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                var container=type.getMethod("getContainer",Level.class).invoke(null,level);
                if(container==null)return false;
                var origin=(org.joml.Vector2i)type.getMethod("getOrigin").invoke(container);
                int plot=(Integer)type.getMethod("getLogPlotSize").invoke(container);
                int side=(Integer)type.getMethod("getLogSideLength").invoke(container);
                bounds=new Bounds((long)origin.x<<plot,(long)origin.y<<plot,
                        ((long)origin.x+(1L<<side))<<plot,((long)origin.y+(1L<<side))<<plot);
                CACHE.put(level,bounds);
            } catch(ReflectiveOperationException | LinkageError e) {
                unavailable=true;
                AroundPlanet.LOGGER.warn("Sable private coordinate bounds unavailable; source watch clipping is suspended to preserve ship data",e);
                return true;
            }
        }
        return bounds.contains(x,z);
    }
    private ShipyardSpace() {}
}

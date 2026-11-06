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
                long[] nativeBounds=SableShipyardBounds.bounds(level);
                if(nativeBounds==null)return false;
                bounds=new Bounds(nativeBounds[0],nativeBounds[1],nativeBounds[2],nativeBounds[3]);
                CACHE.put(level,bounds);
            } catch(RuntimeException | LinkageError e) {
                unavailable=true;
                AroundPlanet.LOGGER.warn("Sable private coordinate bounds unavailable; seam clipping is suspended to preserve ship rendering",e);
                return true;
            }
        }
        return bounds.contains(x,z);
    }
    private ShipyardSpace() {}
}

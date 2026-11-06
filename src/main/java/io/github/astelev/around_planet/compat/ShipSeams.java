package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.level.Level;

/** Optional adapter gates; the world's topology and all saved coordinates stay unchanged. */
public final class ShipSeams {
    public static EastWestTopology topology(Level level) {
        return PlanetConfig.SPEC.isLoaded() && PlanetConfig.SHIP_SEAM.getAsBoolean()?PlanetBorders.get(level.getWorldBorder()):null;
    }
    public static boolean image(Level level,int x,int z) {
        var topology=topology(level);if(topology==null || ShipyardSpace.reserved(level,x,z))return false;
        int half=topology.width()/32,band=PlanetConfig.SHIP_BAND.getAsInt();
        return x>=half && x<half+band || x< -half && x>=-half-band;
    }
    public static int source(Level level,int x) {return SeamGeometry.sourceX(topology(level),x);}
    public static double nearestShift(double x,double cameraX,int width) {return Math.floor((cameraX-x)/width+0.5)*width;}
    private ShipSeams(){}
}

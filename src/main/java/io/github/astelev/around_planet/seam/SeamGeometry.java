package io.github.astelev.around_planet.seam;

import io.github.astelev.around_planet.topology.EastWestTopology;

/** Coordinates of visual images; never changes saved chunk positions. */
public final class SeamGeometry {
    public static boolean clipped(net.minecraft.world.level.Level level,int x,int z) {
        var topology=io.github.astelev.around_planet.border.PlanetBorders.get(level.getWorldBorder());
        return topology!=null && !canonical(topology,x)
                && !io.github.astelev.around_planet.compat.ShipyardSpace.reserved(level,x,z);
    }
    public static boolean canonical(EastWestTopology topology, int x) {
        int half = topology.width() / 32;
        return x >= -half && x < half;
    }
    public static int sourceX(EastWestTopology topology, int imageX) {
        int width = topology.width() / 16;
        return Math.floorMod(imageX + width / 2, width) - width / 2;
    }
    private SeamGeometry() {}
}

package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.level.Level;

/** Loaded only with Sable. Direct descriptor avoids reflecting client-only overloads on a dedicated server. */
final class SableShipyardBounds {
    static long[] bounds(Level level) {
        var container=SubLevelContainer.getContainer(level);if(container==null)return null;
        var origin=container.getOrigin();int plot=container.getLogPlotSize(),side=container.getLogSideLength();
        return new long[]{(long)origin.x<<plot,(long)origin.y<<plot,
                ((long)origin.x+(1L<<side))<<plot,((long)origin.y+(1L<<side))<<plot};
    }
    private SableShipyardBounds(){}
}

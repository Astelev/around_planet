package io.github.astelev.around_planet.portal;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.compat.ShipyardSpace;
import net.minecraft.world.level.Level;

/** Only player watch subscriptions are clipped; one boundary column supplies native neighbor readiness. */
public final class PortalChunkPolicy {
    public static boolean allow(Level level,int x,int z) {
        var topology=PlanetBorders.get(level.getWorldBorder());if(topology==null)return true;
        int half=topology.width()/32;
        return x>=-half-1 && x<=half || ShipyardSpace.reserved(level,x,z);
    }
    private PortalChunkPolicy() {}
}

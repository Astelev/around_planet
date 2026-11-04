package io.github.astelev.around_planet.portal;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** A loaded mob across a same-dimension seam must not despawn solely due to canonical X distance. */
public final class PortalMobDistance {
    public static double forDespawn(Entity viewer,Entity mob,double ordinary) {
        if(!(viewer instanceof ServerPlayer player) || viewer.level()!=mob.level() || viewer.isSpectator())return ordinary;
        var topology=PlanetBorders.get(mob.level().getWorldBorder());
        if(topology==null || topology.outside(viewer.getX()) || topology.outside(mob.getX()))return ordinary;
        double dx=mob.getX()-viewer.getX();
        if(Math.abs(dx)<=topology.width()/2.0 || viewer.getY()<PlanetConfig.MIN_Y.getAsInt() || viewer.getY()>PlanetConfig.MAX_Y.getAsInt()
                || mob.getY()<PlanetConfig.MIN_Y.getAsInt() || mob.getY()>PlanetConfig.MAX_Y.getAsInt()
                || Math.abs(viewer.getZ())>PlanetConfig.HALF_SPAN_Z.getAsInt() || Math.abs(mob.getZ())>PlanetConfig.HALF_SPAN_Z.getAsInt())return ordinary;
        dx-=Math.copySign(topology.width(),dx);
        double dy=mob.getY()-viewer.getY(),dz=mob.getZ()-viewer.getZ();
        return Math.min(ordinary,dx*dx+dy*dy+dz*dz);
    }
    private PortalMobDistance() {}
}

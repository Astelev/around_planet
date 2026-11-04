package io.github.astelev.around_planet.seam;

import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Only the despawn check uses this visual-neighbor distance; positions and navigation stay canonical. */
public final class SeamEntityDistance {
    public static double forDespawn(Player player,Entity mob,double ordinary) {
        if(player.level()!=mob.level() || player.isSpectator())return ordinary;
        var topology=PlanetBorders.get(mob.level().getWorldBorder());if(topology==null)return ordinary;
        int radius=PlanetConfig.RADIUS.getAsInt();
        if(topology.outside(player.getX()) || topology.outside(mob.getX())
                || topology.distanceToSeam(player.getX())>Math.max(PlanetConfig.ACTIVATION.getAsInt(),(radius+3)*16)
                || topology.distanceToSeam(mob.getX())>(radius+2)*16
                || Math.abs((long)player.chunkPosition().z-mob.chunkPosition().z)>radius+1)return ordinary;
        double dx=mob.getX()-player.getX();
        if(Math.abs(dx)<=topology.width()/2.0)return ordinary;
        dx-=Math.copySign(topology.width(),dx);
        double dy=mob.getY()-player.getY(),dz=mob.getZ()-player.getZ();
        return Math.min(ordinary,dx*dx+dy*dy+dz*dz);
    }
    private SeamEntityDistance() {}
}

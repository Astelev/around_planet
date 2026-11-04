package io.github.astelev.around_planet.portal;

import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.world.phys.Vec3;

/** Geometry only; source and destination differ by exactly one circumference. */
public record PortalLayout(EastWestTopology topology,int minY,int maxY,int halfSpanZ) {
    public PortalLayout {
        if(minY>=maxY)throw new IllegalArgumentException("portalMinY must be below portalMaxY");
        if(halfSpanZ<16 || halfSpanZ>29999984)throw new IllegalArgumentException("Invalid portalHalfSpanZ");
    }
    public Vec3 origin(boolean east){return new Vec3(east?topology.east():topology.west(),(minY+(double)maxY)/2,0);}
    public Vec3 destination(boolean east){return origin(!east);}
    public Vec3 axisW(boolean east){return new Vec3(0,0,east?1:-1);}
    public double width(){return halfSpanZ*2.0;}
    public double height(){return maxY-(double)minY;}
}

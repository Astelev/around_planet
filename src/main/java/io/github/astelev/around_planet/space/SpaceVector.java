package io.github.astelev.around_planet.space;

/** Immutable coordinates in blocks, seconds and blocks/second; never Minecraft plot coordinates. */
public record SpaceVector(double x,double y,double z) {
    public static final SpaceVector ZERO=new SpaceVector(0,0,0);
    public SpaceVector {
        if(!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))throw new IllegalArgumentException("Non-finite space vector");
    }
    public SpaceVector add(SpaceVector b){return new SpaceVector(x+b.x,y+b.y,z+b.z);}
    public SpaceVector subtract(SpaceVector b){return new SpaceVector(x-b.x,y-b.y,z-b.z);}
    public SpaceVector scale(double factor){return new SpaceVector(x*factor,y*factor,z*factor);}
    public double dot(SpaceVector b){return x*b.x+y*b.y+z*b.z;}
    public SpaceVector cross(SpaceVector b){return new SpaceVector(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
    public double length(){return Math.hypot(Math.hypot(x,y),z);}
}

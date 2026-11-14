package io.github.astelev.around_planet.space;
import java.util.UUID;
/** Signed prograde, orbit-normal and radial-out delta-v, in m/s at an absolute game tick. */
public record ManeuverNode(UUID id,long tick,double prograde,double normal,double radial) {
    public static final int MAX_NODES=16;public static final double MAX_DV=10000;
    public ManeuverNode {if(id==null||tick<0)throw new IllegalArgumentException("Maneuver identity/time");for(double v:new double[]{prograde,normal,radial})if(!Double.isFinite(v)||Math.abs(v)>MAX_DV)throw new IllegalArgumentException("Maneuver delta-v");}
    public double magnitude(){return Math.hypot(Math.hypot(prograde,normal),radial);}
    public SpaceVector delta(SpaceState state){
        var r=state.position();var v=state.velocity();if(r.length()<1e-8)throw new IllegalArgumentException("No orbital frame at centre");
        var p=v.length()>1e-8?v.scale(1/v.length()):r.cross(Math.abs(r.z()/r.length())<.9?new SpaceVector(0,0,1):new SpaceVector(0,1,0));p=p.scale(1/p.length());
        var n=r.cross(p);if(n.length()<1e-8)n=p.cross(Math.abs(p.z())<.9?new SpaceVector(0,0,1):new SpaceVector(0,1,0));n=n.scale(1/n.length());
        var out=p.cross(n);return p.scale(prograde).add(n.scale(normal)).add(out.scale(radial));
    }
}

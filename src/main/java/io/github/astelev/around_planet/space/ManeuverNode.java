package io.github.astelev.around_planet.space;
import java.util.UUID;
/** Signed prograde, orbit-normal and radial-out delta-v, in m/s at an absolute game tick. */
public record ManeuverNode(UUID id,long tick,double prograde,double normal,double radial,double warningSeconds) {
    public ManeuverNode(UUID id,long tick,double prograde,double normal,double radial){this(id,tick,prograde,normal,radial,-1);}
    public double warningSeconds(double fallback){return warningSeconds<0?fallback:warningSeconds;}
    public long warningTicks(long fallback){return warningSeconds<0?fallback:WarningDuration.ticks(warningSeconds);}
    public static final int MAX_NODES=16;public static final double MAX_DV=10000;
    public ManeuverNode {if(warningSeconds!=-1)warningSeconds=WarningDuration.ticks(warningSeconds)/20.0;if(id==null||tick<0)throw new IllegalArgumentException("Maneuver identity/time");for(double v:new double[]{prograde,normal,radial})if(!Double.isFinite(v)||Math.abs(v)>MAX_DV)throw new IllegalArgumentException("Maneuver delta-v");}
    public double magnitude(){return Math.hypot(Math.hypot(prograde,normal),radial);}
    public ManeuverNode afterImpulse(SpaceState before,SpaceState after,SpaceVector impulse){
        var remaining=delta(before).subtract(impulse);
        if(remaining.length()<.005)remaining=SpaceVector.ZERO;
        var p=new ManeuverNode(id,tick,1,0,0).delta(after);
        var n=new ManeuverNode(id,tick,0,1,0).delta(after);
        var r=new ManeuverNode(id,tick,0,0,1).delta(after);
        return new ManeuverNode(id,tick,Math.clamp(remaining.dot(p),-MAX_DV,MAX_DV),Math.clamp(remaining.dot(n),-MAX_DV,MAX_DV),Math.clamp(remaining.dot(r),-MAX_DV,MAX_DV),warningSeconds);
    }
    public SpaceVector delta(SpaceState state){
        var r=state.position();var v=state.velocity();if(r.length()<1e-8)throw new IllegalArgumentException("No orbital frame at centre");
        var p=v.length()>1e-8?v.scale(1/v.length()):r.cross(Math.abs(r.z()/r.length())<.9?new SpaceVector(0,0,1):new SpaceVector(0,1,0));p=p.scale(1/p.length());
        var n=r.cross(p);if(n.length()<1e-8)n=p.cross(Math.abs(p.z())<.9?new SpaceVector(0,0,1):new SpaceVector(0,1,0));n=n.scale(1/n.length());
        var out=p.cross(n);return p.scale(prograde).add(n.scale(normal)).add(out.scale(radial));
    }
}

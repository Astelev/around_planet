package io.github.astelev.around_planet.space;
import java.util.Optional;
/** Inertial navball directions. Radial is in the orbit plane and perpendicular to velocity. */
public final class OrbitalAttitude {
    public enum Mode {PROGRADE,RETROGRADE,RADIAL_OUT,RADIAL_IN,NORMAL,ANTINORMAL,PLANET_OUT,PLANET_IN,SUN}
    public record Error(double yaw,double pitch){}
    public static Optional<SpaceVector> target(Mode mode,SpaceState state){var r=state.position();var v=state.velocity();var n=r.cross(v);var d=switch(mode){case PROGRADE->v;case RETROGRADE->v.scale(-1);case RADIAL_OUT->v.cross(n);case RADIAL_IN->v.cross(n).scale(-1);case NORMAL->n;case ANTINORMAL->n.scale(-1);case PLANET_OUT->r;case PLANET_IN->r.scale(-1);case SUN->SpaceEnvironment.SUN_DIRECTION;};double length=d.length();return length<1e-8?Optional.empty():Optional.of(d.scale(1/length));}
    /** Forward/right are horizontal block-local unit vectors. */
    public static Error error(SpaceVector target,SpaceVector forward,SpaceVector right){return new Error(Math.hypot(target.x(),target.z())<1e-8?0:Math.toDegrees(Math.atan2(target.dot(right),target.dot(forward))),Math.toDegrees(Math.atan2(target.y(),Math.hypot(target.x(),target.z()))));}
    public static int power(double degrees,int fullScale){return Math.abs(degrees)<1?0:(int)Math.ceil(Math.clamp(Math.abs(degrees)/fullScale,0,1)*15);}
    private OrbitalAttitude(){}
}

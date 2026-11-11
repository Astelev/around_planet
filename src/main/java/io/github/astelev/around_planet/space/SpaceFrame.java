package io.github.astelev.around_planet.space;

/** Nonrotating local frame: owner attitude/rotors stay in Sable, not in the frame orientation.
 * Local axes are fixed in the primary's inertial chart. A non-gravitational change in frame velocity
 * gives every unattached object the opposite delta-v, preserving its cosmic velocity.
 * Common free-fall gravity is part of the coast trajectory, not another thrust impulse.
 */
public record SpaceFrame(SpaceVector east,SpaceVector up,SpaceVector south) {
    public static final SpaceFrame IDENTITY=new SpaceFrame(new SpaceVector(1,0,0),new SpaceVector(0,1,0),new SpaceVector(0,0,1));
    public SpaceFrame {
        if(Math.abs(east.dot(east)-1)>1e-9 || Math.abs(up.dot(up)-1)>1e-9 || Math.abs(south.dot(south)-1)>1e-9
                || Math.abs(east.dot(up))>1e-9 || Math.abs(east.dot(south))>1e-9 || Math.abs(up.dot(south))>1e-9
                || east.cross(up).subtract(south).length()>1e-9)throw new IllegalArgumentException("Frame must be a proper orthonormal rotation");
    }
    public SpaceVector toCosmic(SpaceVector local){return east.scale(local.x()).add(up.scale(local.y())).add(south.scale(local.z()));}
    public SpaceVector toLocal(SpaceVector cosmic){return new SpaceVector(cosmic.dot(east),cosmic.dot(up),cosmic.dot(south));}
    public SpaceState detach(SpaceState origin,SpaceState local) {
        return new SpaceState(origin.position().add(toCosmic(local.position())),origin.velocity().add(toCosmic(local.velocity())));
    }
    public SpaceVector counterImpulse(SpaceVector nonGravitationalFrameDeltaV){return toLocal(nonGravitationalFrameDeltaV).scale(-1);}
}

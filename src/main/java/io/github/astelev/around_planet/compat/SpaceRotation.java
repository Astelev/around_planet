package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import org.joml.*;
/** Proper rigid rotations only: native plots, handedness and joint anchors never change. */
public final class SpaceRotation {
    public static Quaterniond between(SpaceFrame from,SpaceFrame to){
        var e=to.toLocal(from.east());var u=to.toLocal(from.up());var s=to.toLocal(from.south());
        return new Quaterniond().setFromNormalized(new Matrix3d(e.x(),e.y(),e.z(),u.x(),u.y(),u.z(),s.x(),s.y(),s.z())).normalize();
    }
    public static Vector3d point(Vector3dc p,Vector3dc old,Vector3dc target,Quaterniondc q){return q.transform(new Vector3d(p).sub(old)).add(target);}
    public static void pose(dev.ryanhcode.sable.companion.math.Pose3d p,Vector3dc old,Vector3dc target,Quaterniondc q){p.position().set(point(p.position(),old,target,q));p.orientation().premul(q);}
    private SpaceRotation(){}
}

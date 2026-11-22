package io.github.astelev.around_planet.mixin;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import io.github.astelev.around_planet.compat.ShipPoseHistory;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.SubLevel",remap=false)
public abstract class SeamShipHistoryMixin implements ShipPoseHistory {
    @Shadow @Final protected Pose3d lastPose;
    @Shadow @Final protected BoundingBox3d lastGlobalBounds;
    @Override public void around_planet$rotateHistory(org.joml.Vector3dc old,org.joml.Vector3dc target,org.joml.Quaterniondc q){
        io.github.astelev.around_planet.compat.SpaceRotation.pose(lastPose,old,target,q);
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        for(int i=0;i<8;i++){var p=io.github.astelev.around_planet.compat.SpaceRotation.point(new org.joml.Vector3d((i&1)==0?lastGlobalBounds.minX():lastGlobalBounds.maxX(),(i&2)==0?lastGlobalBounds.minY():lastGlobalBounds.maxY(),(i&4)==0?lastGlobalBounds.minZ():lastGlobalBounds.maxZ()),old,target,q);minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
        lastGlobalBounds.set(new BoundingBox3d(minX,minY,minZ,maxX,maxY,maxZ));
    }
    @Override public void around_planet$shiftHistory(double dx,double dy,double dz){lastPose.position().add(dx,dy,dz);lastGlobalBounds.move(dx,dy,dz);}
}

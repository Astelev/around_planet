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
    @Override public void around_planet$shiftHistory(double dx,double dy,double dz){lastPose.position().add(dx,dy,dz);lastGlobalBounds.move(dx,dy,dz);}
}

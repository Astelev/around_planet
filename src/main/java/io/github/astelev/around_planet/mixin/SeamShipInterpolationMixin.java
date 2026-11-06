package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import io.github.astelev.around_planet.compat.ShipInterpolation;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator",remap=false)
public abstract class SeamShipInterpolationMixin implements ShipInterpolation {
    @Shadow @Final private Pose3d runningSnapshot;
    @Shadow @Final public it.unimi.dsi.fastutil.objects.ObjectArrayList<SubLevelSnapshotInterpolator.Snapshot> buffer;
    @Unique private int around_planet$width;
    @Override public void around_planet$rebase(double oldX,double dx,int width) {
        synchronized(buffer) {
            around_planet$width=width;
            if(Math.abs(runningSnapshot.position().x-oldX)<width/2.0)runningSnapshot.position().add(dx,0,0);
            for(int i=0;i<buffer.size();i++) {
                var s=buffer.get(i);var copy=new Pose3d().set(s.pose());
                copy.position().x+=Math.floor((runningSnapshot.position().x-copy.position().x)/width+0.5)*width;
                buffer.set(i,new SubLevelSnapshotInterpolator.Snapshot(s.gameTick(),copy));
            }
        }
    }
    @WrapMethod(method="setFirstPoses")
    private void around_planet$lateInitialPose(Pose3dc a,Pose3dc b,Operation<Void> original) {
        synchronized(buffer) {
            if(around_planet$width>0) {
                double dx=Math.floor((runningSnapshot.position().x-b.position().x())/around_planet$width+0.5)*around_planet$width;
                if(dx!=0){var copy=new Pose3d().set(b);copy.position().add(dx,0,0);b=copy;}
            }
            original.call(a,b);
        }
    }
    @WrapMethod(method="receiveSnapshot")
    private void around_planet$lateUdp(int tick,Pose3dc pose,Operation<Void> original) {
        synchronized(buffer) {
            if(around_planet$width>0) {
                double dx=Math.floor((runningSnapshot.position().x-pose.position().x())/around_planet$width+0.5)*around_planet$width;
                if(dx!=0){var copy=new Pose3d().set(pose);copy.position().add(dx,0,0);pose=copy;}
            }
            original.call(tick,pose);
        }
    }
}

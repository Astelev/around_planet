package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.astelev.around_planet.client.ShipRenderScope;
import io.github.astelev.around_planet.compat.ShipSeams;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.ClientSubLevel",remap=false)
public abstract class SeamShipRenderPoseMixin implements io.github.astelev.around_planet.compat.ShipClientHistory {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private Pose3d renderPose;
    @org.spongepowered.asm.mixin.Shadow private float lastRenderPosePartialTick;
    @Override public void around_planet$shiftRenderCache(double dx){renderPose.position().add(dx,0,0);lastRenderPosePartialTick=-1;}
    private double around_planet$shift(double x) {
        var ship=(ClientSubLevel)(Object)this;var topology=ShipSeams.topology(ship.getLevel());
        return !ShipRenderScope.active() || topology==null?0:ShipSeams.nearestShift(x,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x,topology.width());
    }
    @ModifyReturnValue(method={"renderPose()Ldev/ryanhcode/sable/companion/math/Pose3dc;","renderPose(F)Ldev/ryanhcode/sable/companion/math/Pose3dc;"},at=@At("RETURN"))
    private Pose3dc around_planet$nearestPose(Pose3dc pose) {
        double dx=around_planet$shift(pose.position().x());if(dx==0)return pose;
        var translated=new Pose3d().set(pose);translated.position().add(dx,0,0);return translated;
    }
    @ModifyReturnValue(method="boundingBox",at=@At("RETURN"))
    private BoundingBox3dc around_planet$nearestBounds(BoundingBox3dc box) {
        double dx=around_planet$shift((box.minX()+box.maxX())/2);
        return dx==0?box:new BoundingBox3d(box).move(dx,0,0);
    }
}

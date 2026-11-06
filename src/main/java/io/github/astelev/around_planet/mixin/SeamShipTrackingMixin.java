package io.github.astelev.around_planet.mixin;

import dev.ryanhcode.sable.SableConfig;
import io.github.astelev.around_planet.compat.ShipSeams;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the native spawn, plot chunks, interpolation and removal protocol. */
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem",remap=false)
public abstract class SeamShipTrackingMixin {
    @Inject(method="shouldLoad",at=@At("RETURN"),cancellable=true)
    private void around_planet$periodicViewer(Player player,Vector3dc position,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue())return;
        var topology=ShipSeams.topology(player.level());if(topology==null)return;
        double band=io.github.astelev.around_planet.PlanetConfig.SHIP_BAND.getAsInt()*16.0;
        // A swivel child may already have its own center beyond the seam while the parent has not crossed.
        if(player.getX()<topology.west()-band || player.getX()>topology.east()+band
                || position.x()<topology.west()-band || position.x()>topology.east()+band)return;
        double dx=position.x()+ShipSeams.nearestShift(position.x(),player.getX(),topology.width())-player.getX();
        double dy=position.y()-player.getY(),dz=position.z()-player.getZ(),range=SableConfig.SUB_LEVEL_TRACKING_RANGE.getAsDouble();
        if(dx*dx+dy*dy+dz*dz<range*range)cir.setReturnValue(true);
    }
}

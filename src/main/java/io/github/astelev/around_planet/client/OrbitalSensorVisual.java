package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.sensor.*;
import io.github.astelev.around_planet.space.*;
public final class OrbitalSensorVisual {
    public record Reading(SpaceVector target,SpaceVector up,double yaw,double pitch){}
    public static Reading read(OrbitalSensorBlockEntity be,float partial){var frame=OrbitalSensorBlock.frame(be.getBlockState());SpaceVector target=frame.forward(),up=frame.up();
        if(be.active() && net.neoforged.fml.ModList.get().isLoaded("sable")){target=io.github.astelev.around_planet.compat.SableSensorVisual.local(be,be.visualTarget(),partial);up=io.github.astelev.around_planet.compat.SableSensorVisual.local(be,be.visualUp(),partial);}
        var t=frame.instrument(target);var u=frame.instrument(up);return new Reading(t,u,Math.toDegrees(Math.atan2(t.x(),t.z())),Math.toDegrees(Math.atan2(t.y(),Math.hypot(t.x(),t.z()))));
    }
    private OrbitalSensorVisual(){}
}

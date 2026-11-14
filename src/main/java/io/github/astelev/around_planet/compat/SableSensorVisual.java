package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity;
import io.github.astelev.around_planet.space.*;
import org.joml.Vector3d;
public final class SableSensorVisual {
    public static SpaceVector local(OrbitalSensorBlockEntity be,SpaceVector v,float partial){var ship=Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos());if(!(ship instanceof ClientSubLevel client))return v;var p=client.renderPose(partial).orientation().transformInverse(new Vector3d(v.x(),v.y(),v.z()));return new SpaceVector(p.x,p.y,p.z);}
    public static java.util.UUID structure(OrbitalSensorBlockEntity be){var ship=Sable.HELPER.getContaining(be.getLevel(),be.getBlockPos());return ship==null?null:ship.getUniqueId();}
    private SableSensorVisual(){}
}

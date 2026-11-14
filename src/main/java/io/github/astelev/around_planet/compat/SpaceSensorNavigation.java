package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import java.util.Optional;
/** Uses saved inertial motion plus native relative point velocity, not the stationary cell's velocity. */
public final class SpaceSensorNavigation {
    public record Sample(SpaceVector localTarget,SpaceVector worldTarget,SpaceVector worldUp){}
    public static Optional<SpaceVector> direction(ServerLevel level,BlockPos pos,OrbitalAttitude.Mode mode){return sample(level,pos,mode).map(Sample::localTarget);}
    public static Optional<Sample> sample(ServerLevel level,BlockPos pos,OrbitalAttitude.Mode mode){
        if(!(Sable.HELPER.getContaining(level,pos) instanceof ServerSubLevel ship))return Optional.empty();
        var container=SubLevelContainer.getContainer(level);if(container==null || ship.isRemoved() || !container.physicsSystem().getPhysicsHandle(ship).isValid())return Optional.empty();
        var cells=SpaceCells.get(level);var cell=cells.forShip(ship.getUniqueId());
        var point=ship.logicalPose().transformPosition(new Vector3d(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5));
        if(cell==null)cell=cells.all().stream().filter(c->c.contains(point.x,point.y,point.z)).findFirst().orElse(null);
        var data=SpaceOrbitData.get(level.getServer());var dormant=SpaceDormantData.get(level.getServer()).forShip(ship.getUniqueId());
        java.util.UUID id=cell!=null?cell.id():dormant!=null?dormant.orbit():null;if(id==null || data.get(id).isEmpty() || data.navigation(id).isEmpty())return Optional.empty();
        var nav=data.navigation(id).orElseThrow();var anchor=cell!=null?new SpaceVector(cell.x(),cell.y(),cell.z()):dormant.anchor();var state=data.sample(id,level.getGameTime());
        var pipeline=container.physicsSystem().getPipeline();var velocity=pipeline.getLinearVelocity(ship,new Vector3d());
        if(ship.getSelfMassTracker().getCenterOfMass()!=null){var center=ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass()));velocity.add(pipeline.getAngularVelocity(ship,new Vector3d()).cross(new Vector3d(point).sub(center)));}
        var atPoint=new SpaceState(state.position().add(nav.frame().toCosmic(new SpaceVector(point.x-anchor.x(),point.y-anchor.y(),point.z-anchor.z()))),state.velocity().add(nav.frame().toCosmic(new SpaceVector(velocity.x,velocity.y,velocity.z))));
        return OrbitalAttitude.target(mode,atPoint).map(target->{var local=nav.frame().toLocal(target);var v=ship.logicalPose().orientation().transformInverse(new Vector3d(local.x(),local.y(),local.z()));return new Sample(new SpaceVector(v.x,v.y,v.z),local,nav.frame().toLocal(atPoint.position().scale(1/atPoint.position().length())));});
    }
    public static Vector3d worldPoint(net.minecraft.world.level.Level level,BlockPos pos){var ship=Sable.HELPER.getContaining(level,pos);return ship==null?new Vector3d(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5):ship.logicalPose().transformPosition(new Vector3d(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5));}
    private SpaceSensorNavigation(){}
}

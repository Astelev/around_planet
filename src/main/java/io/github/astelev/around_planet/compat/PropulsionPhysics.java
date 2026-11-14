package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.propulsion.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.joml.Vector3d;
/** Applied in native physics substeps before the virtual space frame captures common velocity. */
public final class PropulsionPhysics {
    public static void step(SubLevelPhysicsSystem system,double dt){
        if(!(dt>0)||!Double.isFinite(dt))return;var level=system.getLevel();var pipe=system.getPipeline();
        for(var be:PropulsionRuntime.loaded(level)){
            if(be.isRemoved()||!level.hasChunkAt(be.getBlockPos())||level.getBlockEntity(be.getBlockPos())!=be)continue;
            var body=Sable.HELPER.getContaining(level,be.getBlockPos());if(!(body instanceof ServerSubLevel ship)||ship.isRemoved()||!system.getPhysicsHandle(ship).isValid())continue;
            var mass=ship.getMassTracker();if(mass.isInvalid()||!(mass.getMass()>0))continue;var q=ship.logicalPose().orientation();
            if(be.kind()==PropulsionRegistry.Kind.DAMPER){
                int signal=be.signal(0);if(signal==0||be.energy.getEnergyStored()==0)continue;
                var w=pipe.getAngularVelocity(ship,new Vector3d());var local=q.transformInverse(new Vector3d(w));double energy=.5*local.dot(mass.getInertiaTensor().transform(local,new Vector3d()));
                var brake=PropulsionMath.brake(energy,be.strength(),signal,dt,be.energy.getEnergyStored());if(brake.cost()==0)continue;
                pipe.addLinearAndAngularVelocity(ship,new Vector3d(),w.mul(brake.factor()-1));be.spend(brake.cost());
            }else{
                var impulse=new Vector3d();for(int i=0;i<(be.kind()==PropulsionRegistry.Kind.RSU?4:1);i++){double amount=PropulsionMath.impulse(be.thrust(),be.signal(i),dt);if(amount==0)continue;var d=be.direction(i);impulse.add(d.x()*amount,d.y()*amount,d.z()*amount);}
                if(impulse.lengthSquared()==0)continue;
                var p=be.getBlockPos();var point=new Vector3d(p.getX()+.5,p.getY()+.5,p.getZ()+.5);if(be.kind()==PropulsionRegistry.Kind.RSU){var normal=be.getBlockState().getValue(io.github.astelev.around_planet.sensor.OrbitalSensorBlock.FACING);point.add(-.375*normal.getStepX(),-.375*normal.getStepY(),-.375*normal.getStepZ());}var torque=point.sub(mass.getCenterOfMass()).cross(impulse);
                var dv=q.transform(new Vector3d(impulse).mul(mass.getInverseMass()));var dw=q.transform(mass.getInverseInertiaTensor().transform(torque,new Vector3d()));
                pipe.addLinearAndAngularVelocity(ship,dv,dw);
            }
        }
    }
    private PropulsionPhysics(){}
}

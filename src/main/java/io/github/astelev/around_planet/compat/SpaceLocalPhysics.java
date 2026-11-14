package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import java.util.*;

/** Fixed nonrotating local frame. Native rotation/internal joint velocities remain untouched. */
public final class SpaceLocalPhysics {
    private record Group(List<ServerSubLevel> ships,Vector3d center,Vector3d velocity){}
    private static Group owner(ServerSubLevelContainer c,SpaceCells.Cell cell) {
        var ships=new ArrayList<ServerSubLevel>();var center=new Vector3d();var velocity=new Vector3d();double mass=0;
        for(var id:cell.ships()) {
            var ship=(ServerSubLevel)c.getSubLevel(id);if(ship==null || ship.isRemoved() || !c.physicsSystem().getPhysicsHandle(ship).isValid())return null;
            var m=ship.getSelfMassTracker();if(m.getCenterOfMass()==null || !(m.getMass()>0))return null;
            ships.add(ship);mass+=m.getMass();center.fma(m.getMass(),ship.logicalPose().transformPosition(new Vector3d(m.getCenterOfMass())));
            velocity.fma(m.getMass(),c.physicsSystem().getPipeline().getLinearVelocity(ship,new Vector3d()));
        }
        return mass>0?new Group(ships,center.div(mass),velocity.div(mass)):null;
    }
    public static SpaceNavigation navigation(ServerLevel level,SpaceCells.Cell cell) {
        var data=SpaceOrbitData.get(level.getServer());var old=data.navigation(cell.id());if(old.isPresent())return old.get();
        var planet=new PlanetScale(io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get()).geometry();
        var nav=new SpaceNavigation(planet,PlanetDaylight.spin(level).frame(planet,cell.originX(),cell.originZ()));
        // Upgrade alpha.2's entry-relative linear chart without losing its displacement or velocity.
        var orbit=data.get(cell.id());if(orbit.isPresent()) {
            var state=orbit.get().at(level.getGameTime());
            data.put(cell.id(),new SpaceTrajectory("around_planet:overworld",level.getGameTime(),new SpaceState(
                PlanetDaylight.spin(level).position(planet,cell.originX(),cell.originY()+32,cell.originZ()).add(nav.frame().toCosmic(state.position())),nav.frame().toCosmic(state.velocity())),mu(planet)));
        }
        data.navigate(cell.id(),nav);return nav;
    }
    private static double mu(PlanetGeometry p){return io.github.astelev.around_planet.PlanetConfig.SPACE_GRAVITY.get()*p.radius()*p.radius();}
    public static void enter(ServerLevel level,ServerSubLevelContainer c,SpaceCells.Cell cell){enter(level,c,cell,cell.originY()+32);}
    public static void enter(ServerLevel level,ServerSubLevelContainer c,SpaceCells.Cell cell,double altitude) {
        var group=owner(c,cell);if(group==null)return;
        var data=SpaceOrbitData.get(level.getServer());var nav=navigation(level,cell);
        if(data.get(cell.id()).isEmpty())data.put(cell.id(),new SpaceTrajectory("around_planet:overworld",level.getGameTime(),PlanetDaylight.spin(level).launch(nav.planet(),new SpaceVector(cell.originX(),altitude,cell.originZ()),vector(group.velocity)),mu(nav.planet())));
        SpaceReentry.entered(level,cell.id(),data.sample(cell.id(),level.getGameTime()).position());
        for(var s:group.ships)c.physicsSystem().getPipeline().addLinearAndAngularVelocity(s,new Vector3d(group.velocity).negate(),new Vector3d());
    }
    /** An external force on the owner frame creates the opposite relative impulse for free bodies. */
    public static void externalImpulse(ServerLevel level,SpaceCells.Cell cell,SpaceVector localDelta) {
        var c=SubLevelContainer.getContainer(level);var pipeline=c.physicsSystem().getPipeline();
        for(var ship:c.getAllSubLevels())if(!ship.isRemoved() && inCell(ship,cell) && !cell.ships().contains(ship.getUniqueId()) && c.physicsSystem().getPhysicsHandle(ship).isValid())
            pipeline.addLinearAndAngularVelocity(ship,new Vector3d(-localDelta.x(),-localDelta.y(),-localDelta.z()),new Vector3d());
        for(var entity:level.getAllEntities())if(!entity.isPassenger() && cell.contains(entity.getX(),entity.getY(),entity.getZ()) && SablePassengers.ship(entity)==null)
            entity.setDeltaMovement(entity.getDeltaMovement().add(-localDelta.x()/20,-localDelta.y()/20,-localDelta.z()/20));
    }
    /** Called immediately before native integration, AFTER queued engine/other forces have been applied. */
    public static void before(SubLevelPhysicsSystem system,double dt) {
        SpaceDormant.before(system,dt);
        var level=system.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
        var c=SubLevelContainer.getContainer(level);var cells=SpaceCells.get(level).all();if(cells.isEmpty())return;
        var pipeline=system.getPipeline();var antiGravity=new Vector3d(DimensionPhysicsData.getGravity(level)).mul(-dt);
        for(var cell:cells) {
            var group=owner(c,cell);var common=new Vector3d();
            if(group!=null) {
                var orbits=SpaceOrbitData.get(level.getServer());var nav=navigation(level,cell);
                if(orbits.get(cell.id()).isEmpty())orbits.put(cell.id(),new SpaceTrajectory("around_planet:overworld",level.getGameTime(),PlanetDaylight.spin(level).launch(nav.planet(),new SpaceVector(cell.originX(),cell.originY()+32,cell.originZ()),vector(group.velocity)),mu(nav.planet())));
                else if(group.velocity.lengthSquared()>1e-10)orbits.impulse(cell.id(),level.getGameTime(),nav.frame().toCosmic(vector(group.velocity)));
                common.set(group.velocity);
            }
            // Partial loading must not reintroduce gravity before the complete owner is available.
            for(var ship:c.getAllSubLevels())if(!ship.isRemoved() && system.getPhysicsHandle(ship).isValid() && inCell(ship,cell)) {
                SpaceDetachment.observe(level,ship,cell);
                // Public pipeline velocity API is in world axes. Cancel native world gravity separately
                // so gravity is not mistaken for engine thrust and saved into virtual velocity.
                pipeline.addLinearAndAngularVelocity(ship,new Vector3d(antiGravity).sub(common),new Vector3d());
            }
            if(common.lengthSquared()>1e-12)for(var entity:level.getAllEntities())if(!entity.isPassenger()
                    && cell.contains(entity.getX(),entity.getY(),entity.getZ()) && SablePassengers.ship(entity)==null)
                entity.setDeltaMovement(entity.getDeltaMovement().add(-common.x/20,-common.y/20,-common.z/20));
        }
    }
    static boolean inCell(ServerSubLevel s,SpaceCells.Cell cell){
        if(cell.ships().contains(s.getUniqueId()))return true;
        if(SpaceDormant.bound(s) || SpaceCells.containsShip(s.getLevel(),s.getUniqueId()) || s.getSelfMassTracker().getCenterOfMass()==null)return false;
        var p=s.logicalPose().transformPosition(new Vector3d(s.getSelfMassTracker().getCenterOfMass()));return cell.contains(p.x,p.y,p.z);
    }
    /** Compensate tiny integrator/solver position drift without resetting angular or relative velocities. */
    public static void after(SubLevelPhysicsSystem system) {
        SpaceDormant.after(system);
        var level=system.getLevel();if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
        var c=SubLevelContainer.getContainer(level);
        for(var cell:SpaceCells.get(level).all()) {
            var group=owner(c,cell);if(group==null)continue;
            var delta=new Vector3d(cell.x(),cell.y(),cell.z()).sub(group.center);
            if(delta.lengthSquared()<1e-12)continue;
            if(delta.lengthSquared()>1) {
                SpaceShipTransfers.move(level,c,group.ships,SpaceShipTransfers.roots(level,group.ships),delta,SpaceCells.get(level).all());
            } else for(var ship:c.getAllSubLevels())if(!ship.isRemoved() && inCell(ship,cell)) {
                system.getPipeline().teleport(ship,new Vector3d(ship.logicalPose().position()).add(delta),ship.logicalPose().orientation());
                ((ShipPoseHistory)ship).around_planet$shiftHistory(delta.x,delta.y,delta.z);ship.updateBoundingBox();
            }
        }
    }
    private static SpaceVector vector(Vector3d v){return new SpaceVector(v.x,v.y,v.z);}
    private SpaceLocalPhysics(){}
}

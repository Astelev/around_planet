package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceStatusPayload;
import net.minecraft.server.level.ServerPlayer;
public final class SpaceTelemetry {
    public static SpaceStatusPayload sample(ServerPlayer p){
        if(p.level().dimension().equals(MoonSurface.dimension()))return surface(p);if(!p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return new SpaceStatusPayload(false,0,0,0,0,0,0,0,false,0,0,0,0,SpaceFrame.IDENTITY,SpaceVector.ZERO);var cells=SpaceCells.get(p.serverLevel());var ship=SablePassengers.ship(p);var cell=ship==null?null:cells.forShip(ship.getUniqueId());
        if(cell==null)cell=cells.all().stream().filter(c->c.contains(p.getX(),p.getY(),p.getZ())).findFirst().orElse(null);
        var data=SpaceOrbitData.get(p.getServer());if(cell==null || data.get(cell.id()).isEmpty())return surface(p);
        var nav=SpaceLocalPhysics.navigation(p.serverLevel(),cell);var state=data.sample(cell.id(),p.serverLevel().getGameTime());var r=state.position();var v=state.velocity();boolean moon=CelestialSystem.MOON.equals(data.get(cell.id()).orElseThrow().primary());double radius=moon&&data.system()!=null?data.system().moonRadius():nav.planet().radius();
        return new SpaceStatusPayload(true,r.x(),r.y(),r.z(),v.x(),v.y(),v.z(),r.length()-radius,(data.reentry(cell.id()).isPresent()||moon&&MoonLandingData.get(p.getServer()).containsKey(cell.id())),radius,data.get(cell.id()).orElseThrow().mu(),moon?0:SpaceAtmosphere.returnAltitude(),SpaceAtmosphere.model().top(),nav.frame(),new SpaceVector(cell.x(),cell.y(),cell.z()),data.get(cell.id()).orElseThrow().primary(),data.system());
    }
    public static SpaceStatusPayload surface(ServerPlayer p){
        var sys=SpaceOrbitData.get(p.getServer()).system();boolean moon=p.level().dimension().equals(MoonSurface.dimension());
        var planet=moon&&sys!=null?MoonSurface.geometry(sys):new PlanetScale(io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get()).geometry();
        var position=new org.joml.Vector3d(p.getX(),p.getY(),p.getZ());
        var velocity=new org.joml.Vector3d(p.getDeltaMovement().x*20,p.getDeltaMovement().y*20,p.getDeltaMovement().z*20);
        if(net.neoforged.fml.ModList.get().isLoaded("sable")){
            var ship=SablePassengers.ship(p);
            if(ship instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel server){
                var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(p.serverLevel());
                if(dev.ryanhcode.sable.Sable.HELPER.getContaining(p)!=null)position=ship.logicalPose().transformPosition(position);
                if(container!=null&&container.physicsSystem().getPhysicsHandle(server).isValid()){
                    var pipeline=container.physicsSystem().getPipeline();velocity=pipeline.getLinearVelocity(server,new org.joml.Vector3d());
                    var center=ship.logicalPose().transformPosition(new org.joml.Vector3d(server.getSelfMassTracker().getCenterOfMass()));
                    velocity.add(pipeline.getAngularVelocity(server,new org.joml.Vector3d()).cross(new org.joml.Vector3d(position).sub(center)));
                }
            }
        }
        var frame=moon?planet.frame(position.x,position.z):PlanetDaylight.spin(p.serverLevel()).frame(planet,position.x,position.z);
        var r=moon?planet.position(position.x,position.y,position.z):PlanetDaylight.spin(p.serverLevel()).position(planet,position.x,position.y,position.z);
        var v=frame.toCosmic(new SpaceVector(velocity.x,velocity.y,velocity.z));double mu=moon&&sys!=null?sys.moonMu():io.github.astelev.around_planet.PlanetConfig.SPACE_GRAVITY.get()*planet.radius()*planet.radius();
        return new SpaceStatusPayload(false,r.x(),r.y(),r.z(),v.x(),v.y(),v.z(),position.y,true,planet.radius(),mu,0,4000,frame,new SpaceVector(position.x,position.y,position.z),moon?CelestialSystem.MOON:CelestialSystem.EARTH,sys);
    }
    private SpaceTelemetry(){}
}

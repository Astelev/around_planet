package io.github.astelev.around_planet.compat;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import dev.ryanhcode.sable.api.sublevel.*;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import net.minecraft.server.level.ServerLevel;
/** Lunar surface uses native local physics; ascent restores a virtual lunar orbit. */
public final class MoonLaunch {
 public static void check(ServerLevel level,ServerSubLevelContainer source){if(!DimensionalTransfers.available()||!PlanetConfig.SPEC.isLoaded()||!PlanetConfig.SPACE.getAsBoolean())return;var space=level.getServer().overworld();var data=SpaceOrbitData.get(level.getServer());var sys=data.system();if(sys==null)return;var destination=SubLevelContainer.getContainer(space);var planet=MoonSurface.geometry(sys);
  for(var group:ShipAssemblies.groups(level,source.getAllSubLevels())){if(group.anchored()||group.ships().isEmpty()||group.ships().size()>256||group.ships().stream().anyMatch(s->!source.physicsSystem().getPhysicsHandle(s).isValid()))continue;var center=SpaceDetachment.center(group.ships());if(center==null||center.y<PlanetConfig.MOON_LANDING.get()+200)continue;var velocity=SpaceDetachment.velocity(source,group.ships());if(velocity.y<=0)continue;
   var cell=SpaceShipTransfers.allocateDetached(space,group.ships(),center);if(cell==null)continue;var target=new Vector3d(cell.x(),cell.y(),cell.z());var delta=new Vector3d(target).sub(center);if(!SpaceShipTransfers.fits(space,group.ships(),delta,cell)||!SpaceShipTransfers.ready(space,group.ships(),delta))continue;var roots=SpaceShipTransfers.roots(level,group.ships());var frame=planet.frame(center.x,center.z);var ids=group.ships().stream().map(s->s.getUniqueId()).toList();
   DimensionalTransfers.move(group.ships(),roots,space,center,target,new Quaterniond(),new Vector3d());
   SpaceCells.get(space).add(space,cell);data.navigate(cell.id(),new SpaceNavigation(new PlanetScale(PlanetConfig.PLANET_SIZE.get()).geometry(),frame));data.put(cell.id(),new SpaceTrajectory(CelestialSystem.MOON,space.getGameTime(),new SpaceState(planet.position(center.x,center.y,center.z),frame.toCosmic(new SpaceVector(velocity.x,velocity.y,velocity.z))),sys.moonMu()));for(var id:ids){var moved=(dev.ryanhcode.sable.sublevel.ServerSubLevel)destination.getSubLevel(id);destination.physicsSystem().getPipeline().addLinearAndAngularVelocity(moved,new Vector3d(velocity).negate(),new Vector3d());}SpaceReentry.entered(space,cell.id(),data.sample(cell.id(),space.getGameTime()).position());io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_ASCENT cell={} ships={} altitude={} speed={}",cell.id(),ids.size(),center.y,velocity.length());
  }
 }
 private MoonLaunch(){}
}

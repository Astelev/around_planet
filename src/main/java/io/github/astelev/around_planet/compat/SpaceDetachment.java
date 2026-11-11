package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.Sable;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceTransferPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import java.util.*;

/** Promote a distant free connected group to its own fixed frame without recreating native bodies. */
public final class SpaceDetachment {
    public static final double DISTANCE=192;
    private static final Map<ServerLevel,Map<UUID,UUID>> SOURCE=new WeakHashMap<>();
    public static void clear(ServerLevel level){SOURCE.remove(level);}
    public static void observe(ServerLevel level,ServerSubLevel ship,SpaceCells.Cell cell){if(!SpaceCells.containsShip(level,ship.getUniqueId()))SOURCE.computeIfAbsent(level,k->new HashMap<>()).put(ship.getUniqueId(),cell.id());}
    public static boolean associated(ServerLevel level,UUID ship){return SOURCE.getOrDefault(level,Map.of()).containsKey(ship);}
    public static void check(ServerLevel level,ServerSubLevelContainer container){
        var cells=SpaceCells.get(level);if(cells.all().isEmpty()){SOURCE.remove(level);return;}var data=SpaceOrbitData.get(level.getServer());var sources=SOURCE.computeIfAbsent(level,k->new HashMap<>());
        var groups=ShipAssemblies.groups(level,container.getAllSubLevels());normalizeOwners(level,container,groups);
        sources.entrySet().removeIf(e->SpaceCells.containsShip(level,e.getKey()) || cells.all().stream().noneMatch(c->c.id().equals(e.getValue())) || container.getSubLevel(e.getKey())==null);
        for(var group:groups){if(group.anchored() || group.ships().isEmpty() || group.ships().size()>256 || group.ships().stream().anyMatch(s->SpaceDormant.bound(s) || SpaceCells.containsShip(level,s.getUniqueId()) || !container.physicsSystem().getPhysicsHandle(s).isValid()))continue;
            var center=center(group.ships());if(center==null)continue;
            SpaceCells.Cell parent=null;
            for(var cell:cells.all())if(cell.contains(center.x,center.y,center.z) || group.ships().stream().anyMatch(s->cell.id().equals(sources.get(s.getUniqueId())))){parent=cell;break;}
            if(parent==null || data.get(parent.id()).isEmpty() || data.reentry(parent.id()).isPresent())continue;
            for(var s:group.ships())sources.put(s.getUniqueId(),parent.id());
            if(Math.max(Math.max(Math.abs(center.x-parent.x()),Math.abs(center.y-parent.y())),Math.abs(center.z-parent.z()))<DISTANCE)continue;
            var allocated=SpaceShipTransfers.allocateDetached(level,group.ships(),center);if(allocated==null)continue;
            var child=new SpaceCells.Cell(allocated.id(),allocated.slot(),allocated.x(),allocated.y(),allocated.z(),parent.originX(),parent.originY(),parent.originZ(),allocated.ships());
            var delta=new Vector3d(child.x(),child.y(),child.z()).sub(center);
            if(!SpaceShipTransfers.fits(level,group.ships(),delta,child))continue;
            var roots=SpaceShipTransfers.roots(level,group.ships());
            if(roots.stream().flatMap(Entity::getSelfAndPassengers).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(SpaceTransferPayload.TYPE)))continue;
            Set<Entity> moving=Collections.newSetFromMap(new IdentityHashMap<>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
            boolean leash=false;for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.Mob m && m.isLeashed() && moving.contains(m)!=moving.contains(m.getLeashHolder())){leash=true;break;}if(leash)continue;
            if(!SpaceShipTransfers.ready(level,group.ships(),delta))continue;
            var nav=SpaceLocalPhysics.navigation(level,parent);var velocity=velocity(container,group.ships());
            var offset=new SpaceVector(center.x-parent.x(),center.y-parent.y(),center.z-parent.z());
            data.detach(parent.id(),child.id(),level.getGameTime(),nav.frame(),new SpaceState(offset,new SpaceVector(velocity.x,velocity.y,velocity.z)));data.navigate(child.id(),nav);
            cells.add(level,child);SpaceShipTransfers.move(level,container,group.ships(),roots,delta,cells.all());
            for(var s:group.ships()){container.physicsSystem().getPipeline().addLinearAndAngularVelocity(s,new Vector3d(velocity).negate(),new Vector3d());sources.remove(s.getUniqueId());}
            SpaceReentry.entered(level,child.id(),data.sample(child.id(),level.getGameTime()).position());
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_DETACH parent={} child={} bodies={} relativeVelocity={}",parent.id(),child.id(),group.ships().size(),velocity);
        }
    }
    private static void normalizeOwners(ServerLevel level,ServerSubLevelContainer c,List<ShipAssemblies.Group> groups){
        var cells=SpaceCells.get(level);var data=SpaceOrbitData.get(level.getServer());
        for(var cell:cells.all()){
            if(data.get(cell.id()).isEmpty() || data.reentry(cell.id()).isPresent() || cell.ships().stream().anyMatch(id->c.getSubLevel(id)==null))continue;
            var group=groups.stream().filter(g->g.ships().stream().anyMatch(s->s.getUniqueId().equals(cell.ships().getFirst()))).findFirst().orElse(null);
            if(group==null || group.anchored() || group.ships().stream().anyMatch(s->SpaceDormant.bound(s) || !c.physicsSystem().getPhysicsHandle(s).isValid()) || group.ships().stream().anyMatch(s->{var other=cells.forShip(s.getUniqueId());return other!=null && !other.id().equals(cell.id());}))continue;
            var ids=group.ships().stream().map(ServerSubLevel::getUniqueId).toList();if(new HashSet<>(ids).equals(new HashSet<>(cell.ships())))continue;
            var center=center(group.ships());if(center==null)continue;var nav=SpaceLocalPhysics.navigation(level,cell);var state=data.sample(cell.id(),level.getGameTime());var orbit=data.get(cell.id()).orElseThrow();
            var offset=new SpaceVector(center.x-cell.x(),center.y-cell.y(),center.z-cell.z());
            data.put(cell.id(),new SpaceTrajectory(orbit.primary(),level.getGameTime(),new SpaceState(state.position().add(nav.frame().toCosmic(offset)),state.velocity()),orbit.mu()));
            var all=c.getAllSubLevels().stream().filter(s->!s.isRemoved() && c.physicsSystem().getPhysicsHandle(s).isValid() && (SpaceLocalPhysics.inCell(s,cell) || cell.id().equals(SOURCE.getOrDefault(level,Map.of()).get(s.getUniqueId())))).toList();var delta=new Vector3d(cell.x(),cell.y(),cell.z()).sub(center);
            for(var id:cell.ships())if(!ids.contains(id))SOURCE.computeIfAbsent(level,k->new HashMap<>()).put(id,cell.id());
            var next=new ArrayList<UUID>();next.add(cell.ships().getFirst());for(var id:ids)if(!next.contains(id))next.add(id);
            cells.add(level,new SpaceCells.Cell(cell.id(),cell.slot(),cell.x(),cell.y(),cell.z(),cell.originX(),cell.originY(),cell.originZ(),next));
            if(delta.lengthSquared()>1e-12){var roots=SpaceShipTransfers.roots(level,all);for(var entity:level.getAllEntities())if(!entity.isPassenger() && cell.contains(entity.getX(),entity.getY(),entity.getZ()) && Sable.HELPER.getContaining(entity)==null)roots.add(entity.getRootVehicle());SpaceShipTransfers.move(level,c,all,roots,delta,cells.all());}
        }
    }
    static Vector3d center(List<ServerSubLevel> ships){var p=new Vector3d();double mass=0;for(var s:ships){var m=s.getSelfMassTracker();if(m.getCenterOfMass()==null || !(m.getMass()>0))return null;p.fma(m.getMass(),s.logicalPose().transformPosition(new Vector3d(m.getCenterOfMass())));mass+=m.getMass();}return p.div(mass);}
    static Vector3d velocity(ServerSubLevelContainer c,List<ServerSubLevel> ships){var v=new Vector3d();double mass=0;for(var s:ships){double m=s.getSelfMassTracker().getMass();v.fma(m,c.physicsSystem().getPipeline().getLinearVelocity(s,new Vector3d()));mass+=m;}return v.div(mass);}
    private SpaceDetachment(){}
}

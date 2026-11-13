package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import java.util.*;
/** Rebase physical addresses only; inertial states and velocities are untouched. */
public final class SpaceCellHeight {
    public static void relocate(ServerLevel level,ServerSubLevelContainer container){
        var cells=SpaceCells.get(level);double height=PlanetConfig.SPACE_Y.getAsInt();
        for(var cell:cells.all()){
            if(cell.y()==height || cell.ships().stream().anyMatch(id->!(container.getSubLevel(id) instanceof ServerSubLevel ship) || !container.physicsSystem().getPhysicsHandle(ship).isValid()))continue;
            var ships=new ArrayList<ServerSubLevel>();boolean blocked=false;
            for(var group:ShipAssemblies.groups(level,container.getAllSubLevels()))if(group.ships().stream().anyMatch(s->SpaceLocalPhysics.inCell(s,cell))){if(group.anchored() || group.ships().stream().anyMatch(s->!SpaceLocalPhysics.inCell(s,cell) || !container.physicsSystem().getPhysicsHandle(s).isValid())){blocked=true;break;}ships.addAll(group.ships());}
            if(blocked || ships.size()>256 || !ships.stream().map(ServerSubLevel::getUniqueId).toList().containsAll(cell.ships()))continue;
            var delta=new Vector3d(0,height-cell.y(),0);var next=new SpaceCells.Cell(cell.id(),cell.slot(),cell.x(),height,cell.z(),cell.originX(),cell.originY(),cell.originZ(),cell.ships());
            if(!SpaceShipTransfers.fits(level,ships,delta,next))continue;
            var roots=SpaceShipTransfers.roots(level,ships);for(var e:level.getAllEntities())if(!e.isPassenger() && cell.contains(e.getX(),e.getY(),e.getZ()) && dev.ryanhcode.sable.Sable.HELPER.getContaining(e)==null)roots.add(e.getRootVehicle());
            if(roots.stream().flatMap(Entity::getSelfAndPassengers).filter(net.minecraft.server.level.ServerPlayer.class::isInstance).map(net.minecraft.server.level.ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(io.github.astelev.around_planet.network.SpaceTransferPayload.TYPE)))continue;
            Set<Entity> moving=Collections.newSetFromMap(new IdentityHashMap<>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
            if(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.Mob m && m.isLeashed() && moving.contains(m)!=moving.contains(m.getLeashHolder())))continue;
            cells.add(level,next);SpaceShipTransfers.move(level,container,ships,roots,delta,cells.all());
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_HEIGHT cell={} from={} to={} bodies={}",cell.id(),cell.y(),height,ships.size());
        }
    }
    private SpaceCellHeight(){}
}

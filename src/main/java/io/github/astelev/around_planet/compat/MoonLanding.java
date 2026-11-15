package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import java.util.*;
/** Prepare destination chunks asynchronously, then transfer a complete physical cell. */
public final class MoonLanding {
    private static final Map<ServerLevel,Map<UUID,SpaceVector>> LAST=new WeakHashMap<>();
    private static final TicketType<ChunkPos> TICKET=TicketType.create("around_planet_moon_landing",Comparator.comparingLong(ChunkPos::toLong),100);
    public static void clear(ServerLevel level){LAST.remove(level);}
    public static void check(ServerLevel source,ServerSubLevelContainer container){
        if(!DimensionalTransfers.available())return;var destination=source.getServer().getLevel(MoonSurface.dimension());if(destination==null)return;
        var data=SpaceOrbitData.get(source.getServer());var sys=data.system();if(sys==null)return;var planet=MoonSurface.geometry(sys);var cells=SpaceCells.get(source);var pending=MoonLandingData.get(source.getServer());var last=LAST.computeIfAbsent(source,k->new HashMap<>());var live=cells.all().stream().map(SpaceCells.Cell::id).collect(java.util.stream.Collectors.toSet());pending.retain(live);last.keySet().retainAll(live);
        for(var cell:cells.all()){
            var orbit=data.get(cell.id()).orElse(null);if(orbit==null||!CelestialSystem.MOON.equals(orbit.primary())){pending.remove(cell.id());last.remove(cell.id());continue;}
            var state=data.sample(cell.id(),source.getGameTime());var before=last.put(cell.id(),state.position());double altitude=PlanetConfig.MOON_LANDING.get();
            if(!pending.containsKey(cell.id())){var contact=MoonSurface.contact(planet,before==null?orbit.initial().position():before,state,altitude);if(contact==null)continue;pending.put(cell.id(),new SpaceState(contact,state.velocity()));data.put(cell.id(),new SpaceTrajectory(CelestialSystem.MOON,source.getGameTime(),new SpaceState(contact,SpaceVector.ZERO),0));continue;}
            var landing=pending.get(cell.id());var surface=planet.surface(landing.position());var nav=SpaceLocalPhysics.navigation(source,cell);var frame=planet.frame(surface.x(),surface.z());var q=SpaceRotation.between(nav.frame(),frame);var old=new Vector3d(cell.x(),cell.y(),cell.z());var target=new Vector3d(surface.x(),altitude,surface.z());
            var ships=new ArrayList<ServerSubLevel>();boolean blocked=false;
            for(var group:ShipAssemblies.groups(source,container.getAllSubLevels())){if(group.ships().stream().noneMatch(s->SpaceLocalPhysics.inCell(s,cell)))continue;if(group.anchored()||group.ships().stream().anyMatch(s->!SpaceLocalPhysics.inCell(s,cell)||!container.physicsSystem().getPhysicsHandle(s).isValid())){blocked=true;break;}ships.addAll(group.ships());}
            var ids=ships.stream().map(ServerSubLevel::getUniqueId).toList();if(blocked||ships.isEmpty()||ships.size()>256||!ids.containsAll(cell.ships())){if(source.getGameTime()%100==0)io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_WAIT assembly cell={} blocked={} bodies={} ids={}",cell.id(),blocked,ships.size(),ids);continue;}
            var roots=SpaceShipTransfers.roots(source,ships);for(var e:source.getAllEntities())if(!e.isPassenger()&&cell.contains(e.getX(),e.getY(),e.getZ())&&Sable.HELPER.getContaining(e)==null)roots.add(e.getRootVehicle());
            var moving=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());roots.forEach(r->r.getSelfAndPassengers().forEach(moving::add));if(java.util.stream.StreamSupport.stream(source.getAllEntities().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.Mob m&&m.isLeashed()&&moving.contains(m)!=moving.contains(m.getLeashHolder())))continue;
            if(!ready(destination,ships,roots,old,target,q)){if(source.getGameTime()%100==0)io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_WAIT chunks cell={} target={} bounds={}",cell.id(),target,ships.getFirst().boundingBox().toMojang());continue;}
            var velocity=frame.toLocal(landing.velocity());DimensionalTransfers.move(ships,roots,destination,old,target,q,new Vector3d(velocity.x(),velocity.y(),velocity.z()));
            cells.remove(source,cell);data.remove(cell.id());pending.remove(cell.id());last.remove(cell.id());SpaceAtmosphere.forget(source,cell.id());
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("MOON_LANDING cell={} ships={} target={} speed={} dimension={}",cell.id(),ships.size(),target,velocity.length(),destination.dimension().location());
        }
    }
    static boolean ready(ServerLevel destination,List<ServerSubLevel> ships,Set<Entity> roots,Vector3dc old,Vector3dc target,Quaterniondc q){
        var columns=new HashSet<ChunkPos>();
        for(var ship:ships){var b=ship.boundingBox();for(int i=0;i<8;i++){var p=SpaceRotation.point(new Vector3d((i&1)==0?b.minX():b.maxX(),(i&2)==0?b.minY():b.maxY(),(i&4)==0?b.minZ():b.maxZ()),old,target,q);if(!column(destination,p,columns))return false;}}
        for(var root:roots)for(var e:root.getSelfAndPassengers().toList())if(Sable.HELPER.getContaining(e)==null){var p=SpaceRotation.point(new Vector3d(e.getX(),e.getY(),e.getZ()),old,target,q);if(!column(destination,p,columns))return false;}
        if(columns.isEmpty())return false;int minX=columns.stream().mapToInt(p->p.x).min().orElseThrow()-1,maxX=columns.stream().mapToInt(p->p.x).max().orElseThrow()+1,minZ=columns.stream().mapToInt(p->p.z).min().orElseThrow()-1,maxZ=columns.stream().mapToInt(p->p.z).max().orElseThrow()+1;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;boolean ready=true;for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){var p=new ChunkPos(x,z);destination.getChunkSource().addRegionTicket(TICKET,p,2,p);var chunk=destination.getChunkSource().getChunkNow(x,z);if(chunk==null||!chunk.isLightCorrect())ready=false;}return ready;
    }
    private static boolean column(ServerLevel level,Vector3d p,Set<ChunkPos> columns){if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)||p.y<level.getMaxBuildHeight()+24||p.y>30000||!level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(p.x,p.y,p.z)))return false;columns.add(new ChunkPos(net.minecraft.core.BlockPos.containing(p.x,p.y,p.z)));return columns.size()<=128;}
    private MoonLanding(){}
}

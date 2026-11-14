package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceTransferPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Quaterniond;
import java.util.*;

/** Merge coordinate frames, not rigid bodies. The receiver alone remains fixed to its orbit. */
public final class SpaceRendezvous {
    private static final SubLevelLoadingTicketType<UUID> TICKET=SubLevelLoadingTicketType.create(net.minecraft.resources.ResourceLocation.parse("around_planet:rendezvous"),com.mojang.serialization.Codec.STRING.xmap(UUID::fromString,UUID::toString));
    private static final TicketType<ChunkPos> CHUNKS=TicketType.create("around_planet_rendezvous",Comparator.comparingLong(ChunkPos::toLong),100);
    private record Wake(UUID host,long until){}
    private static final Map<ServerLevel,Map<UUID,Wake>> WAKING=new WeakHashMap<>();
    private record ObjectFrame(UUID id,SpaceCells.Cell cell,SpaceDormantData.Binding dormant){}
    public static void init(){}
    public static void clear(ServerLevel l){WAKING.remove(l);SpaceCargo.clear(l);}
    private static List<UUID> owners(ObjectFrame o){return o.cell()!=null?o.cell().ships():o.dormant().members().stream().map(SpaceDormantData.Member::ship).toList();}
    private static boolean loaded(ServerSubLevelContainer c,List<UUID> ids){return !ids.isEmpty()&&ids.stream().allMatch(id->{var s=(ServerSubLevel)c.getSubLevel(id);return s!=null&&!s.isRemoved()&&c.physicsSystem().getPhysicsHandle(s).isValid();});}
    private static boolean arriving(ServerLevel level,ObjectFrame source,UUID host){var pending=WAKING.getOrDefault(level,Map.of());return owners(source).stream().anyMatch(id->{var wake=pending.get(id);return wake!=null&&wake.until()>=level.getGameTime()&&wake.host().equals(host);});}
    public static void check(ServerLevel level,ServerSubLevelContainer c){
        release(level,c);if(PlanetConfig.RENDEZVOUS_DISTANCE.get()<=0)return;
        var cargo=SpaceCargoData.get(level.getServer());for(var record:cargo.all()){var body=c.getSubLevel(record.ship());if(body!=null&&!body.isRemoved()){var actual=SpaceCells.get(level).forShip(record.ship());var dormant=SpaceDormantData.get(level.getServer()).forShip(record.ship());if(actual!=null&&!actual.id().equals(record.cell())||dormant!=null&&!dormant.orbit().equals(record.cell()))cargo.remove(record.ship());}}
        var cells=SpaceCells.get(level);var orbits=SpaceOrbitData.get(level.getServer());var objects=new ArrayList<ObjectFrame>();
        for(var cell:cells.all())objects.add(new ObjectFrame(cell.id(),cell,null));
        for(var b:SpaceDormantData.get(level.getServer()).all())objects.add(new ObjectFrame(b.orbit(),null,b));
        objects.sort(Comparator.comparing(ObjectFrame::id));var used=new HashSet<UUID>();var samples=new HashMap<UUID,SpaceState>();for(var object:objects)if(orbits.get(object.id()).isPresent())samples.put(object.id(),orbits.sample(object.id(),level.getGameTime()));
        var receivers=objects.stream().filter(o->o.cell()!=null&&loaded(c,owners(o))).toList();
        for(var host:receivers){if(receivers.stream().anyMatch(other->other.cell()!=null&&!other.id().equals(host.id())&&loaded(c,owners(other))&&arriving(level,host,other.id())))continue;if(host.cell()==null||used.contains(host.id())||!loaded(c,owners(host))||orbits.get(host.id()).isEmpty()||orbits.reentry(host.id()).isPresent())continue;
            for(var source:objects){if(source.id().equals(host.id())||used.contains(source.id())||orbits.get(source.id()).isEmpty()||orbits.reentry(source.id()).isPresent())continue;
                // Loaded receivers win; two loaded cells use a stable UUID order.
                if(source.cell()!=null&&loaded(c,owners(source))&&source.id().compareTo(host.id())<0&&!arriving(level,source,host.id()))continue;
                var a=orbits.get(source.id()).orElseThrow();var b=orbits.get(host.id()).orElseThrow();if(!a.primary().equals(b.primary())||a.mu()!=b.mu())continue;
                if(!SpaceEncounter.eligible(samples.get(source.id()),samples.get(host.id()),PlanetConfig.RENDEZVOUS_DISTANCE.get(),PlanetConfig.RENDEZVOUS_SPEED.get()))continue;
                if(merge(level,c,source,host)){used.add(source.id());used.add(host.id());break;}
            }
        }
    }
    private static void release(ServerLevel level,ServerSubLevelContainer c){var waking=WAKING.computeIfAbsent(level,k->new HashMap<>());waking.values().removeIf(w->w.until()<level.getGameTime());var cells=SpaceCells.get(level);
        for(var e:c.collectForceLoadTickets().entrySet())for(var ticket:List.copyOf(e.getValue()))if(ticket.type().equals(TICKET)){
            var host=cells.all().stream().filter(cell->cell.id().equals(ticket.key())).findFirst().orElse(null);var ship=e.getKey();var wake=waking.get(ship.getUniqueId());
            boolean keep=host!=null&&loaded(c,host.ships())&&!host.ships().contains(ship.getUniqueId())&&(SpaceLocalPhysics.inCell(ship,host)||(wake!=null&&wake.host().equals(host.id())));
            if(!keep)c.removeForceLoadTicket(ship,TICKET,(UUID)ticket.key());
        }
    }
    private static boolean merge(ServerLevel level,ServerSubLevelContainer c,ObjectFrame source,ObjectFrame host){
        var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);var registry=SpaceDormantData.get(level.getServer());var cargo=SpaceCargoData.get(level.getServer());
        var ids=new LinkedHashSet<>(owners(source));for(var record:cargo.all())if(record.cell().equals(source.id()))ids.add(record.ship());
        if(source.cell()!=null)for(var ship:c.getAllSubLevels())if(!ship.isRemoved()&&SpaceLocalPhysics.inCell(ship,source.cell()))ids.add(ship.getUniqueId());
        if(ids.size()>256)return false;
        // Resolve saved native addresses without generating terrain or allocating a third space.
        for(var id:ids)if(c.getSubLevel(id)==null){var held=c.getHoldingChunkMap().getHoldingSubLevel(id);if(held!=null)c.getHoldingChunkMap().loadHoldingSubLevel(held);else{var record=cargo.all().stream().filter(r->r.ship().equals(id)).findFirst().orElse(null);if(record==null)return false;c.getHoldingChunkMap().snatchAndLoad(new GlobalSavedSubLevelPointer(new ChunkPos(record.chunk()),record.storage(),record.index()),id);}}
        var awake=ids.stream().map(id->(ServerSubLevel)c.getSubLevel(id)).filter(Objects::nonNull).filter(s->!s.isRemoved()).toList();
        for(var ship:awake){c.addForceLoadTicket(ship,TICKET,host.id());WAKING.computeIfAbsent(level,k->new HashMap<>()).put(ship.getUniqueId(),new Wake(host.id(),level.getGameTime()+100));}
        if(!loaded(c,List.copyOf(ids)))return false;var ships=awake;
        for(var group:ShipAssemblies.groups(level,c.getAllSubLevels()))if(group.ships().stream().anyMatch(s->ids.contains(s.getUniqueId()))&&(group.anchored()||group.ships().stream().anyMatch(s->!ids.contains(s.getUniqueId()))))return false;
        var sourceNav=source.cell()!=null?SpaceLocalPhysics.navigation(level,source.cell()):data.navigation(source.id()).orElseThrow();var hostNav=SpaceLocalPhysics.navigation(level,host.cell());
        if(!sourceNav.planet().equals(hostNav.planet()))return false;
        var relative=SpaceEncounter.relative(data.sample(source.id(),level.getGameTime()),data.sample(host.id(),level.getGameTime()),hostNav.frame());
        var q=SpaceRotation.between(sourceNav.frame(),hostNav.frame());
        var old=source.cell()!=null?new Vector3d(source.cell().x(),source.cell().y(),source.cell().z()):vec(source.dormant().anchor());
        var target=new Vector3d(host.cell().x(),host.cell().y(),host.cell().z()).add(vec(relative.position()));var boost=vec(relative.velocity());
        var roots=SpaceShipTransfers.roots(level,ships);if(source.cell()!=null)for(var e:level.getAllEntities())if(!e.isPassenger()&&source.cell().contains(e.getX(),e.getY(),e.getZ())&&Sable.HELPER.getContaining(e)==null)roots.add(e.getRootVehicle());
        if(roots.stream().flatMap(Entity::getSelfAndPassengers).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)&&!p.connection.hasChannel(SpaceTransferPayload.TYPE)))return false;
        Set<Entity> moving=Collections.newSetFromMap(new IdentityHashMap<>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
        for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.Mob m&&m.isLeashed()&&moving.contains(m)!=moving.contains(m.getLeashHolder()))return false;
        if(!ready(level,ships,roots,old,target,q,host.cell()))return false;
        SpaceCargo.prepare(c);SpaceDormant.prepare(c);
        var next=cells.all().stream().filter(cell->!cell.id().equals(source.id())).toList();var moves=ships.stream().map(s->{var p=s.logicalPose().position();var n=SpaceRotation.point(p,old,target,q);return new SpaceTransferPayload.Move(s.getUniqueId(),p.x,p.y,p.z,n.x-p.x,n.y-p.y,n.z-p.z,q.x,q.y,q.z,q.w);}).toList();
        for(var p:level.players())if(p.connection.hasChannel(SpaceTransferPayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new SpaceTransferPayload(level.dimension().location(),next,moves));
        var riders=new ArrayList<Runnable>();for(var root:roots)riders.add(ShipSeatTransfer.prepareRigid(root,old,target,q,boost,SablePassengers.ship(root)!=null||Sable.HELPER.getContaining(root)!=null));
        var pipeline=c.physicsSystem().getPipeline();for(var ship:ships){var p=SpaceRotation.point(ship.logicalPose().position(),old,target,q);var v=pipeline.getLinearVelocity(ship,new Vector3d());var w=pipeline.getAngularVelocity(ship,new Vector3d());pipeline.addLinearAndAngularVelocity(ship,q.transform(new Vector3d(v)).add(boost).sub(v),q.transform(new Vector3d(w)).sub(w));pipeline.teleport(ship,p,new Quaterniond(q).mul(ship.logicalPose().orientation()));((ShipPoseHistory)ship).around_planet$rotateHistory(old,target,q);ship.updateBoundingBox();cargo.remove(ship.getUniqueId());}
        for(var rider:riders)rider.run();
        if(source.cell()!=null)cells.remove(level,source.cell());registry.remove(source.id());SpaceDormant.forget(level,source.id());data.remove(source.id());SpaceAtmosphere.forget(level,source.id());SpaceDetachment.clearSource(level,source.id());
        io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_RENDEZVOUS source={} host={} bodies={} relativeSpeed={} distance={} target={}",source.id(),host.id(),ships.size(),relative.velocity().length(),relative.position().length(),target);return true;
    }
    private static boolean ready(ServerLevel level,List<ServerSubLevel> ships,Set<Entity> roots,Vector3d old,Vector3d target,Quaterniond q,SpaceCells.Cell host){
        var columns=new HashSet<ChunkPos>();for(var ship:ships){var b=ship.boundingBox();double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
            for(int i=0;i<8;i++){var p=SpaceRotation.point(new Vector3d((i&1)==0?b.minX():b.maxX(),(i&2)==0?b.minY():b.maxY(),(i&4)==0?b.minZ():b.maxZ()),old,target,q);if(!host.contains(p.x,p.y,p.z)||p.y<level.getMaxBuildHeight()+24)return false;minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z);}
            for(int x=(int)Math.floor(minX/16);x<=(int)Math.floor(maxX/16);x++)for(int z=(int)Math.floor(minZ/16);z<=(int)Math.floor(maxZ/16);z++){columns.add(new ChunkPos(x,z));if(columns.size()>128)return false;}}
        for(var root:roots)if(SablePassengers.ship(root)==null){var p=SpaceRotation.point(new Vector3d(root.getX(),root.getY(),root.getZ()),old,target,q);if(!host.contains(p.x,p.y,p.z))return false;columns.add(new ChunkPos(net.minecraft.core.BlockPos.containing(p.x,p.y,p.z)));}
        boolean ready=true;for(var pos:columns){level.getChunkSource().addRegionTicket(CHUNKS,pos,2,pos);var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);if(chunk==null||!chunk.isLightCorrect())ready=false;}return ready;
    }
    private static Vector3d vec(SpaceVector v){return new Vector3d(v.x(),v.y(),v.z());}
    private SpaceRendezvous(){}
}

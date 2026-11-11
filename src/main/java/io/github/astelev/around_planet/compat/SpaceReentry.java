package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceTransferPayload;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import java.util.*;

/** A cell returns as one transaction: all loaded bodies, free entities and native seat trees. */
public final class SpaceReentry {
    private static final Map<ServerLevel,Map<UUID,SpaceVector>> LAST=new WeakHashMap<>();
    private static final TicketType<ChunkPos> TICKET=TicketType.create("around_planet_reentry",Comparator.comparingLong(ChunkPos::toLong),100);
    public static void clear(ServerLevel level){LAST.remove(level);SpaceAtmosphere.clear(level);}
    public static void entered(ServerLevel level,UUID id,SpaceVector position){LAST.computeIfAbsent(level,k->new HashMap<>()).put(id,position);}
    public static void check(ServerLevel level,ServerSubLevelContainer c,Set<UUID> requests,Map<UUID,Long> cooldown) {
        var data=SpaceOrbitData.get(level.getServer());var cells=SpaceCells.get(level);var last=LAST.computeIfAbsent(level,k->new HashMap<>());
        last.keySet().removeIf(id->cells.all().stream().noneMatch(cell->cell.id().equals(id)));
        for(var cell:cells.all()) {
            var nav=SpaceLocalPhysics.navigation(level,cell);var trajectory=data.get(cell.id());if(trajectory.isEmpty())continue;
            long now=level.getGameTime();SpaceAtmosphere.advance(level,cell.id(),cell);var state=data.sample(cell.id(),now);double shell=SpaceAtmosphere.returnAltitude();
            if(data.reentry(cell.id()).isEmpty()) {
                SpaceVector previous=last.put(cell.id(),state.position());SpaceVector contact=null;
                if(requests.contains(cell.id()) || state.position().length()<nav.planet().radius()+shell && state.position().dot(state.velocity())<0)
                    contact=state.position().scale((nav.planet().radius()+shell)/state.position().length());
                else {var before=previous==null?trajectory.get().initial().position():previous;contact=nav.planet().inwardCrossing(before,state.position(),shell).orElse(null);}
                if(contact==null)continue;
                SpaceAtmosphere.handoff(level,cell.id(),cell);data.reenter(cell.id(),contact,now);
            }
            var world=nav.planet().surface(data.reentry(cell.id()).orElseThrow());var destinationFrame=nav.planet().frame(world.x(),world.z());
            var q=SpaceRotation.between(nav.frame(),destinationFrame);var old=new Vector3d(cell.x(),cell.y(),cell.z());var target=new Vector3d(world.x(),shell,world.z());
            var all=new ArrayList<ServerSubLevel>();boolean blocked=false;
            for(var group:ShipAssemblies.groups(level,c.getAllSubLevels())) {
                if(group.ships().stream().noneMatch(s->SpaceLocalPhysics.inCell(s,cell)))continue;
                if(group.anchored() || group.ships().stream().anyMatch(s->!SpaceLocalPhysics.inCell(s,cell) || !c.physicsSystem().getPhysicsHandle(s).isValid())){blocked=true;break;}
                all.addAll(group.ships());
            }
            var ids=all.stream().map(ServerSubLevel::getUniqueId).toList();if(blocked || all.size()>256 || !ids.containsAll(cell.ships()))continue;
            var roots=SpaceShipTransfers.roots(level,all);
            for(var e:level.getAllEntities())if(!e.isPassenger() && cell.contains(e.getX(),e.getY(),e.getZ()) && Sable.HELPER.getContaining(e)==null)roots.add(e.getRootVehicle());
            if(roots.stream().flatMap(Entity::getSelfAndPassengers).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(SpaceTransferPayload.TYPE)))continue;
            Set<Entity> moving=Collections.newSetFromMap(new IdentityHashMap<>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
            if(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).anyMatch(e->e instanceof net.minecraft.world.entity.Mob m && m.isLeashed() && moving.contains(m)!=moving.contains(m.getLeashHolder())))continue;
            if(!ready(level,all,roots,old,target,q))continue;
            SpaceAtmosphere.handoff(level,cell.id(),cell);
            var velocity=destinationFrame.toLocal(data.sample(cell.id(),now).velocity());var boost=new Vector3d(velocity.x(),velocity.y(),velocity.z());
            var moves=all.stream().map(s->{var p=s.logicalPose().position();var n=SpaceRotation.point(p,old,target,q);return new SpaceTransferPayload.Move(s.getUniqueId(),p.x,p.y,p.z,n.x-p.x,n.y-p.y,n.z-p.z,q.x,q.y,q.z,q.w);}).toList();
            var packet=new SpaceTransferPayload(level.dimension().location(),cells.all().stream().filter(v->!v.id().equals(cell.id())).toList(),moves);
            for(var p:level.players())if(p.connection.hasChannel(SpaceTransferPayload.TYPE))PacketDistributor.sendToPlayer(p,packet);
            var riders=new ArrayList<Runnable>();for(var root:roots)riders.add(ShipSeatTransfer.prepareRigid(root,old,target,q,boost,SablePassengers.ship(root)!=null || Sable.HELPER.getContaining(root)!=null));
            var pipeline=c.physicsSystem().getPipeline();
            for(var ship:all){var p=new Vector3d(ship.logicalPose().position());var n=SpaceRotation.point(p,old,target,q);var v=pipeline.getLinearVelocity(ship,new Vector3d());var w=pipeline.getAngularVelocity(ship,new Vector3d());
                pipeline.addLinearAndAngularVelocity(ship,q.transform(new Vector3d(v)).add(boost).sub(v),q.transform(new Vector3d(w)).sub(w));
                pipeline.teleport(ship,n,new Quaterniond(q).mul(ship.logicalPose().orientation()));((ShipPoseHistory)ship).around_planet$rotateHistory(old,target,q);ship.updateBoundingBox();cooldown.put(ship.getUniqueId(),now+100);
            }
            for(var rider:riders)rider.run();
            data.remove(cell.id());cells.remove(level,cell);requests.remove(cell.id());last.remove(cell.id());SpaceAtmosphere.forget(level,cell.id());
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_REENTRY cell={} bodies={} target={} velocity={}",cell.id(),all.size(),target,boost);
        }
    }
    private static boolean ready(ServerLevel level,List<ServerSubLevel> ships,Set<Entity> roots,Vector3dc old,Vector3dc target,Quaterniondc q) {
        var columns=new HashSet<ChunkPos>();
        for(var ship:ships){var b=ship.boundingBox();double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
            for(int i=0;i<8;i++){var p=SpaceRotation.point(new Vector3d((i&1)==0?b.minX():b.maxX(),(i&2)==0?b.minY():b.maxY(),(i&4)==0?b.minZ():b.maxZ()),old,target,q);minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
            if(minY<level.getMaxBuildHeight()+24 || maxY>Math.max(2000,target.y()+256) || !level.getWorldBorder().isWithinBounds(new net.minecraft.world.phys.AABB(minX,minY,minZ,maxX,maxY,maxZ)))return false;
            for(int x=(int)Math.floor(minX/16);x<=(int)Math.floor(maxX/16);x++)for(int z=(int)Math.floor(minZ/16);z<=(int)Math.floor(maxZ/16);z++){columns.add(new ChunkPos(x,z));if(columns.size()>128)return false;}
        }
        for(var root:roots)for(var entity:root.getSelfAndPassengers().toList())if(Sable.HELPER.getContaining(entity)==null){
            var p=SpaceRotation.point(new Vector3d(entity.getX(),entity.getY(),entity.getZ()),old,target,q);double radius=entity.getBbWidth()/2.0;
            if(p.y<level.getMaxBuildHeight()+24 || p.y+entity.getBbHeight()>Math.max(2000,target.y()+256))return false;
            for(int x=(int)Math.floor((p.x-radius)/16);x<=(int)Math.floor((p.x+radius)/16);x++)for(int z=(int)Math.floor((p.z-radius)/16);z<=(int)Math.floor((p.z+radius)/16);z++){columns.add(new ChunkPos(x,z));if(columns.size()>128)return false;}
        }
        boolean ready=true;for(var p:columns){level.getChunkSource().addRegionTicket(TICKET,p,2,p);var chunk=level.getChunkSource().getChunkNow(p.x,p.z);if(chunk==null || !chunk.isLightCorrect())ready=false;}return ready;
    }
    private SpaceReentry(){}
}

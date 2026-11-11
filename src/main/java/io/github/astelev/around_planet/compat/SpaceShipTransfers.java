package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.SpaceCells;
import io.github.astelev.around_planet.network.SpaceTransferPayload;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import java.util.*;

/** Native same-level transfer; stable plots and joint identity are retained. */
public final class SpaceShipTransfers {
    private record Pending(SpaceCells.Cell cell,long created){}
    private static final Map<ServerLevel,Map<UUID,Pending>> PENDING=new WeakHashMap<>();
    private static final Set<UUID> RETURN=new HashSet<>();
    private static final Map<UUID,Long> RECENT_RETURN=new HashMap<>();
    private static final TicketType<ChunkPos> TICKET=TicketType.create("around_planet_space",Comparator.comparingLong(ChunkPos::toLong),100);
    public static void clear(ServerLevel level){PENDING.remove(level);RETURN.clear();RECENT_RETURN.clear();SpaceReentry.clear(level);SpacePlanetScale.clear(level);SpaceDetachment.clear(level);}
    public static boolean requestReturn(ServerPlayer player) {
        var ship=SablePassengers.ship(player);if(ship==null)return false;
        var cell=SpaceCells.get(player.serverLevel()).forShip(ship.getUniqueId());if(cell==null)return false;
        RETURN.add(cell.id());return true;
    }
    public static void check(ServerLevel level,ServerSubLevelContainer container) {
        if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) || !PlanetConfig.SPEC.isLoaded()
                )return;
        RECENT_RETURN.values().removeIf(until->until<level.getGameTime());
        SpacePlanetScale.check(level);SpaceReentry.check(level,container,RETURN,RECENT_RETURN);
        SpaceDetachment.check(level,container);
        var data=SpaceCells.get(level);var pending=PENDING.computeIfAbsent(level,k->new HashMap<>());
        pending.values().removeIf(p->level.getGameTime()-p.created>200);
        for(var group:ShipAssemblies.groups(level,container.getAllSubLevels())) {
            if(group.anchored() || group.ships().isEmpty() || group.ships().size()>256)continue;
            var center=new Vector3d();double mass=0;boolean valid=true;
            for(var ship:group.ships()) {
                var m=ship.getSelfMassTracker();if(m.getCenterOfMass()==null || !Double.isFinite(m.getMass()) || m.getMass()<=0){valid=false;break;}
                center.fma(m.getMass(),ship.logicalPose().transformPosition(new Vector3d(m.getCenterOfMass())));mass+=m.getMass();
            }
            if(!valid || mass<=0)continue;center.div(mass);
            var key=group.ships().stream().map(ServerSubLevel::getUniqueId).min(UUID::compareTo).orElseThrow();
            var cell=data.forShip(key);
            if(cell!=null || group.ships().stream().anyMatch(s->SpaceDetachment.associated(level,s.getUniqueId())))continue;
            {
                if(group.ships().stream().anyMatch(s->RECENT_RETURN.getOrDefault(s.getUniqueId(),0L)>level.getGameTime()))continue;
                if(!PlanetConfig.ENABLED.getAsBoolean() || !PlanetConfig.SPACE.getAsBoolean())continue;
                if(SpaceCells.contains(level,center.x,center.y,center.z))continue;
                if(center.y<PlanetConfig.SPACE_ENTRY.getAsInt()) {pending.remove(key);continue;}
                boolean clear=group.ships().stream().allMatch(s->s.boundingBox().minY()>level.getMaxBuildHeight()+24);
                if(!clear)continue;
                var existing=pending.get(key);
                if(existing==null) {
                    cell=allocate(level,data,pending,group.ships(),center);if(cell==null)continue;
                    pending.put(key,new Pending(cell,level.getGameTime()));
                }else cell=existing.cell;
            }
            var target=new Vector3d(cell.x(),cell.y(),cell.z());
            var delta=target.sub(center);
            if(!fits(level,group.ships(),delta,cell))continue;
            var roots=roots(level,group.ships());
            if(roots.stream().flatMap(Entity::getSelfAndPassengers).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast)
                    .anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(SpaceTransferPayload.TYPE)))continue;
            var moving=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
            boolean externalLeash=false;
            for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.Mob m && m.isLeashed() && moving.contains(m)!=moving.contains(m.getLeashHolder())){externalLeash=true;break;}
            if(externalLeash)continue;
            if(!ready(level,group.ships(),delta))continue;
            data.add(level,cell);
            move(level,container,group.ships(),roots,delta,data.all());
            SpaceLocalPhysics.enter(level,container,cell,center.y);
            pending.remove(key);
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_TRANSFER enter cell={} ships={} delta={}",cell.id(),group.ships().size(),delta);
        }
    }
    static SpaceCells.Cell allocateDetached(ServerLevel level,List<ServerSubLevel> ships,Vector3d center){return allocate(level,SpaceCells.get(level),PENDING.computeIfAbsent(level,k->new HashMap<>()),ships,center);}
    static SpaceCells.Cell allocate(ServerLevel level,SpaceCells data,Map<UUID,Pending> pending,List<ServerSubLevel> ships,Vector3d center) {
        var occupied=new HashSet<Integer>();data.all().forEach(c->occupied.add(c.slot()));pending.values().forEach(p->occupied.add(p.cell.slot()));
        var b=level.getWorldBorder();var topology=io.github.astelev.around_planet.border.PlanetBorders.get(b);
        double west=topology==null?b.getMinX():Math.max(b.getMinX(),topology.west());
        double east=topology==null?b.getMaxX():Math.min(b.getMaxX(),topology.east());
        var address=io.github.astelev.around_planet.space.SpaceCellLayout.find(occupied,west,east,b.getMinZ(),b.getMaxZ());
        if(address.isEmpty())return null;var slot=address.get();
        return new SpaceCells.Cell(UUID.randomUUID(),slot.slot(),slot.x(),PlanetConfig.SPACE_Y.getAsInt(),slot.z(),center.x,PlanetConfig.SPACE_ENTRY.getAsInt()-32,center.z,ships.stream().map(ServerSubLevel::getUniqueId).toList());
    }
    static boolean fits(ServerLevel level,List<ServerSubLevel> ships,Vector3d delta,SpaceCells.Cell cell) {
        for(var s:ships) {
            var b=s.boundingBox();double lo=b.minY()+delta.y,hi=b.maxY()+delta.y;
            if(lo<level.getMaxBuildHeight()+24 || hi>2000)return false;
            if(cell!=null && (b.minX()+delta.x<cell.x()-256 || b.maxX()+delta.x>cell.x()+256 || b.minZ()+delta.z<cell.z()-256 || b.maxZ()+delta.z>cell.z()+256 || lo<cell.y()-256 || hi>cell.y()+256))return false;
        }return true;
    }
    static boolean ready(ServerLevel level,List<ServerSubLevel> ships,Vector3d delta) {
        var columns=new HashSet<ChunkPos>();for(var s:ships){var b=s.boundingBox();for(int x=(int)Math.floor((b.minX()+delta.x)/16);x<=(int)Math.floor((b.maxX()+delta.x)/16);x++)for(int z=(int)Math.floor((b.minZ()+delta.z)/16);z<=(int)Math.floor((b.maxZ()+delta.z)/16);z++) {columns.add(new ChunkPos(x,z));if(columns.size()>128)return false;}}
        boolean ready=true;for(var pos:columns){level.getChunkSource().addRegionTicket(TICKET,pos,2,pos);var c=level.getChunkSource().getChunkNow(pos.x,pos.z);if(c==null || !c.isLightCorrect())ready=false;}
        return ready;
    }
    static Set<Entity> roots(ServerLevel level,List<ServerSubLevel> ships) {
        var members=new HashSet<>(ships);Set<Entity> roots=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var entity:level.getAllEntities())if(members.contains(SablePassengers.ship(entity)))roots.add(entity.getRootVehicle());return roots;
    }
    public static void move(ServerLevel level,ServerSubLevelContainer container,List<ServerSubLevel> ships,Set<Entity> roots,Vector3d delta,List<SpaceCells.Cell> cells) {
        var moves=ships.stream().map(s->{var p=s.logicalPose().position();return new SpaceTransferPayload.Move(s.getUniqueId(),p.x,p.y,p.z,delta.x,delta.y,delta.z);}).toList();
        var packet=new SpaceTransferPayload(level.dimension().location(),cells,moves);
        for(var p:level.players())if(p.connection.hasChannel(SpaceTransferPayload.TYPE))PacketDistributor.sendToPlayer(p,packet);
        var seated=new IdentityHashMap<Entity,Runnable>();for(var root:roots)if(Sable.HELPER.getContaining(root)!=null)seated.put(root,ShipSeatTransfer.prepare(root,delta.x,delta.y,delta.z));
        var pipeline=container.physicsSystem().getPipeline();
        for(var ship:ships){pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()).add(delta),ship.logicalPose().orientation());((ShipPoseHistory)ship).around_planet$shiftHistory(delta.x,delta.y,delta.z);ship.updateBoundingBox();}
        for(var root:roots){var seat=seated.get(root);if(seat!=null)seat.run();else TransferService.moveGroup(root,delta.x,delta.y,delta.z);}
    }
    private SpaceShipTransfers(){}
}

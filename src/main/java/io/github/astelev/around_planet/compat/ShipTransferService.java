package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.network.SeamShipTransferPayload;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import java.util.*;

/** Public physics API only; a joint group and its native attached passengers change coordinates together. */
public final class ShipTransferService {
    private static long serial;
    public static void check(ServerLevel level,ServerSubLevelContainer container) {
        var topology=ShipSeams.topology(level);if(topology==null || !PlanetConfig.SHIP_TRANSFER.getAsBoolean())return;
        if(container.getAllSubLevels().stream().noneMatch(s->!s.isRemoved() && (s.boundingBox().minX()<topology.west() || s.boundingBox().maxX()>=topology.east())))return;
        var pipeline=container.physicsSystem().getPipeline();
        if(!pipeline.getClass().getName().equals("dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline"))return;
        for(var group:ShipAssemblies.groups(level,container.getAllSubLevels())) {
            if(group.anchored() || group.ships().size()>SeamShipTransferPayload.MAX_SHIPS)continue;
            double mass=0,x=0;boolean valid=true;
            for(var ship:group.ships()) {
                var data=ship.getSelfMassTracker();double m=data.getMass();var center=data.getCenterOfMass();
                if(center==null || !Double.isFinite(m) || m<=0){valid=false;break;}
                var world=ship.logicalPose().transformPosition(new Vector3d(center));mass+=m;x+=world.x*m;
            }
            if(!valid || mass<=0 || !Double.isFinite(x) || !topology.outside(x/mass))continue;
            double dx=-Math.floor((x/mass-topology.west())/topology.width())*topology.width();
            boolean ready=true;
            for(var ship:group.ships()) {
                var local=ship.getPlot().getBoundingBox();
                var b=new dev.ryanhcode.sable.companion.math.BoundingBox3d(local.minX(),local.minY(),local.minZ(),local.maxX()+1.0,local.maxY()+1.0,local.maxZ()+1.0).transform(ship.logicalPose());if(b.minY()>=level.getMaxBuildHeight() || b.maxY()<level.getMinBuildHeight())continue;
                for(int cx=(int)Math.floor((b.minX()+dx)/16);cx<=(int)Math.floor((b.maxX()+dx-1e-7)/16) && ready;cx++)
                    for(int cz=(int)Math.floor(b.minZ()/16);cz<=(int)Math.floor((b.maxZ()-1e-7)/16);cz++)
                        if(level.getChunkSource().getChunkNow(SeamGeometry.sourceX(topology,cx),cz)==null){ready=false;break;}
            }
            if(!ready)continue;
            Set<ServerSubLevel> members=Collections.newSetFromMap(new IdentityHashMap<>());members.addAll(group.ships());
            Set<Entity> roots=Collections.newSetFromMap(new IdentityHashMap<>());
            for(var entity:level.getAllEntities()) {
                var tracked=SablePassengers.ship(entity);
                if(tracked instanceof ServerSubLevel ship && members.contains(ship))roots.add(entity.getRootVehicle());
            }
            Set<Entity> moving=Collections.newSetFromMap(new IdentityHashMap<>());
            for(var root:roots)root.getSelfAndPassengers().forEach(moving::add);
            boolean externalLeash=false;
            for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob && mob.isLeashed()
                    && moving.contains(mob)!=moving.contains(mob.getLeashHolder())){externalLeash=true;break;}
            if(externalLeash)continue;
            // Clients without our transfer channel keep ordinary Sable loading and never receive a partial group rebase.
            if(roots.stream().flatMap(e->e.getSelfAndPassengers()).filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast).anyMatch(p->!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && !p.connection.hasChannel(SeamShipTransferPayload.TYPE)))continue;
            if(Boolean.getBoolean("around_planet.traceShipTransfers"))io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SHIP_REBASE center={} dx={} ships={} roots={}",x/mass,dx,group.ships().stream().map(s->s.getUniqueId()+"@"+s.logicalPose().position().x).toList(),roots.stream().map(e->e.getId()+"@"+e.getX()).toList());
            var entries=group.ships().stream().map(s->new SeamShipTransferPayload.Ship(s.getUniqueId(),s.logicalPose().position().x)).toList();
            var payload=new SeamShipTransferPayload(level.dimension().location(),++serial,topology.width(),dx,entries);
            for(var player:level.players())if(player.connection.hasChannel(SeamShipTransferPayload.TYPE))PacketDistributor.sendToPlayer(player,payload);
            // Plot-local seats never change storage coordinates. Snapshot world riders before moving poses.
            var seated=new IdentityHashMap<Entity,Runnable>();
            for(var root:roots)if(dev.ryanhcode.sable.Sable.HELPER.getContaining(root)!=null)
                seated.put(root,ShipSeatTransfer.prepare(root,dx));
            for(var ship:group.ships()) {
                pipeline.teleport(ship,new Vector3d(ship.logicalPose().position()).add(dx,0,0),ship.logicalPose().orientation());
                ((ShipPoseHistory)ship).around_planet$shiftHistory(dx);ship.updateBoundingBox();
            }
            for(var root:roots) {
                var seatTransfer=seated.get(root);
                if(seatTransfer!=null)seatTransfer.run();else TransferService.moveGroup(root,dx);
            }
        }
    }
    private ShipTransferService(){}
}

package io.github.astelev.around_planet.compat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import java.util.*;
/** Dimensional Sable handles plots/constraints; our adapter preserves complete seat trees. */
public final class DimensionalTransfers {
    private static final dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType<UUID> ARRIVAL=dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet","dimensional_arrival"),net.minecraft.core.UUIDUtil.CODEC);
    private static final Map<ServerLevel,Map<UUID,Long>> ARRIVALS=new WeakHashMap<>();
    public static void init(){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DimensionalTransfers::tick);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DimensionalTransfers::stopping);}
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){for(var entry:ARRIVALS.entrySet()){var c=SubLevelContainer.getContainer(entry.getKey());for(var id:entry.getValue().keySet())if(c.getSubLevel(id) instanceof ServerSubLevel ship)c.removeForceLoadTicket(ship,ARRIVAL,id);}ARRIVALS.clear();}
    public static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event){if(!(event.getLevel() instanceof ServerLevel level))return;var arrivals=ARRIVALS.get(level);if(arrivals==null)return;var c=SubLevelContainer.getContainer(level);var it=arrivals.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(level.getGameTime()<entry.getValue())continue;var ship=c.getSubLevel(entry.getKey());if(ship instanceof ServerSubLevel serverShip)c.removeForceLoadTicket(serverShip,ARRIVAL,entry.getKey());it.remove();}}

    private static final ThreadLocal<Boolean> ACTIVE=ThreadLocal.withInitial(()->false);
    public static boolean active(){return ACTIVE.get();}
    public static boolean available(){return net.neoforged.fml.ModList.get().isLoaded("dimensional_sable");}
    private record Body(UUID id,Vector3d velocity,Vector3d angular){}
    private record Rider(Entity entity,UUID vehicle,Vector3d position,Vector3d velocity,Vector3d look,boolean local){}
    public static void move(List<ServerSubLevel> ships,Set<Entity> roots,ServerLevel destination,Vector3d old,Vector3d target,Quaterniond rotation,Vector3d boost){
        if(!available()||ships.isEmpty())throw new IllegalStateException("Dimensional Sable 1.1.0 is required for lunar landing");
        var source=ships.getFirst().getLevel();var pipeline=SubLevelContainer.getContainer(source).physicsSystem().getPipeline();
        var bodies=ships.stream().map(s->new Body(s.getUniqueId(),pipeline.getLinearVelocity(s,new Vector3d()),pipeline.getAngularVelocity(s,new Vector3d()))).toList();
        var entities=new LinkedHashSet<Entity>();for(var root:roots)root.getSelfAndPassengers().forEach(entities::add);
        var riders=entities.stream().map(e->new Rider(e,e.getVehicle()==null?null:e.getVehicle().getUUID(),new Vector3d(e.getX(),e.getY(),e.getZ()),new Vector3d(e.getDeltaMovement().x,e.getDeltaMovement().y,e.getDeltaMovement().z),new Vector3d(e.getLookAngle().x,e.getLookAngle().y,e.getLookAngle().z),Sable.HELPER.getContaining(e)!=null)).toList();
        var copies=new HashMap<UUID,Entity>();for(var rider:riders)if(!(rider.entity() instanceof net.minecraft.server.level.ServerPlayer)){var copy=rider.entity().getType().create(destination);if(copy==null)throw new IllegalStateException("Entity cannot transfer: "+rider.entity().getType());copy.restoreFrom(rider.entity());copies.put(rider.entity().getUUID(),copy);}
        io.github.astelev.around_planet.AroundPlanet.LOGGER.info("DIMENSIONAL_HANDOFF bodies={} passengers={}",ships.size(),riders.stream().map(r->r.entity().getType()+":"+r.entity().getUUID()+":"+r.vehicle()).toList());
        // The public overload accepts an explicit complete group, avoiding spatial guesses.
        var first=ships.getFirst();var anchor=SpaceRotation.point(first.logicalPose().position(),old,target,rotation);
        ACTIVE.set(true);
        try{var method=Class.forName("dev.egg.dimensionalsable.SubLevelWarper").getMethod("warpConnectedSubLevels",ServerSubLevel.class,List.class,ServerLevel.class,Vector3d.class,Quaterniond.class);int moved=(Integer)method.invoke(null,first,ships,destination,anchor,rotation);if(moved!=ships.size())throw new IllegalStateException("Incomplete dimensional transfer: "+moved);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Dimensional Sable transfer failed",e);}finally{ACTIVE.remove();}
        var container=SubLevelContainer.getContainer(destination);var destPipeline=container.physicsSystem().getPipeline();
        for(var body:bodies){var ship=(ServerSubLevel)container.getSubLevel(body.id());if(ship==null)throw new IllegalStateException("Missing transferred body "+body.id());container.addForceLoadTicket(ship,ARRIVAL,ship.getUniqueId());ARRIVALS.computeIfAbsent(destination,k->new HashMap<>()).put(ship.getUniqueId(),destination.getGameTime()+100);destPipeline.resetVelocity(ship);destPipeline.addLinearAndAngularVelocity(ship,rotation.transform(new Vector3d(body.velocity())).add(boost),rotation.transform(new Vector3d(body.angular())));ship.updateBoundingBox();ship.updateLastPose();}
        // Dismount explicitly before crossing dimensions, then rebuild relationships by UUID.
        for(var rider:riders)rider.entity().stopRiding();
        var movedEntities=new HashMap<UUID,Entity>();
        for(var rider:riders){var p=rider.local()?rider.position():SpaceRotation.point(rider.position(),old,target,rotation);var look=rider.local()?rider.look():rotation.transform(new Vector3d(rider.look()));float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z)),pitch=(float)Math.toDegrees(Math.atan2(-look.y,Math.hypot(look.x,look.z)));
            Entity moved;if(rider.entity() instanceof net.minecraft.server.level.ServerPlayer player){if(!player.teleportTo(destination,p.x,p.y,p.z,Set.of(),yaw,pitch))throw new IllegalStateException("Player dimension transfer rejected");moved=player;}else {moved=copies.get(rider.entity().getUUID());moved.moveTo(p.x,p.y,p.z,yaw,pitch);moved.setYHeadRot(yaw);rider.entity().setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);destination.addDuringTeleport(moved);}
            if(moved==null)throw new IllegalStateException("Passenger dimension transfer rejected "+rider.entity().getUUID());moved.setYRot(yaw);moved.setXRot(pitch);movedEntities.put(rider.entity().getUUID(),moved);var v=rider.local()?rider.velocity():rotation.transform(new Vector3d(rider.velocity()));if(!rider.local()&&rider.vehicle()==null)v.fma(1.0/20,boost);moved.setDeltaMovement(v.x,v.y,v.z);moved.fallDistance=0;
        }
        for(var rider:riders)if(rider.vehicle()!=null){var e=movedEntities.get(rider.entity().getUUID());var vehicle=movedEntities.get(rider.vehicle());if(e!=null&&vehicle!=null)e.startRiding(vehicle,true);}
    }
    private DimensionalTransfers(){}
}

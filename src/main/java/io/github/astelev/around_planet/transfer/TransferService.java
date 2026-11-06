package io.github.astelev.around_planet.transfer;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class TransferService {
    private static final TicketType<ChunkPos> PRELOAD = TicketType.create("around_planet_destination", Comparator.comparingLong(ChunkPos::toLong), 100);
    private record Lease(long refreshed,long playerUntil,int radius) {}
    private enum Readiness { JOINED, MISSING_CHUNKS, COLLISION, READY }
    private static final Map<ServerLevel, Map<ChunkPos, Lease>> REQUESTS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<Entity,Readiness>> PENDING = new IdentityHashMap<>();

    private static void pending(ServerLevel level,Entity entity,Readiness reason) {
        var set=PENDING.computeIfAbsent(level,ignored->new IdentityHashMap<>());
        if(set.size()<4096 || set.containsKey(entity))set.put(entity,reason);
    }
    private static void completed(ServerLevel level,Entity entity) {
        var set=PENDING.get(level);if(set!=null)set.remove(entity);
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel level) {
            var topology=PlanetBorders.get(level.getWorldBorder());
            if(topology!=null && topology.outside(event.getEntity().getX())
                    && !io.github.astelev.around_planet.compat.ShipyardSpace.reserved(level,event.getEntity().chunkPosition().x,event.getEntity().chunkPosition().z)) pending(level,event.getEntity(),Readiness.JOINED);
        }
    }
    @SubscribeEvent public static void retry(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var set=PENDING.get(level);if(set==null || set.isEmpty())return;
        for(var entity:set.keySet().toArray(Entity[]::new)) {
            if(entity.isRemoved() || entity.level()!=level || entity.isPassenger()) {set.remove(entity);continue;}
            check(entity);
        }
    }

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        var level = event.getLevel();
        if (!(level instanceof Level world) || !world.dimension().equals(Level.OVERWORLD)
                || !PlanetConfig.SPEC.isLoaded() || !PlanetConfig.ENABLED.getAsBoolean()) return;
        try {
            var topology=EastWestTopology.atBoundary(PlanetConfig.BOUNDARY.getAsInt());
            PlanetBorders.bind(world.getWorldBorder(), topology);
            if (world instanceof ServerLevel) AroundPlanet.LOGGER.info("East/west seams: X={} and X={}, circumference={} blocks",
                    topology.west(),topology.east(),topology.width());
        } catch (IllegalArgumentException exception) {
            AroundPlanet.LOGGER.error("East/west transfer disabled: {}", exception.getMessage());
        }
    }
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        REQUESTS.forEach((level, requests) -> requests.forEach((pos,lease) ->
                level.getChunkSource().removeRegionTicket(PRELOAD,pos,lease.radius,pos)));
        REQUESTS.clear();
        PENDING.clear();
    }
    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) PlanetBorders.unbind(level.getWorldBorder());
        if (event.getLevel() instanceof ServerLevel level) REQUESTS.remove(level);
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }
    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        check(event.getEntity());
    }
    /** Also called immediately after a validated movement packet, before another server tick. */
    public static void check(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved() || entity.isPassenger()) return;
        var topology = PlanetBorders.get(level.getWorldBorder());
        if (topology == null) return;
        if(topology.outside(entity.getX()) && io.github.astelev.around_planet.compat.ShipyardSpace.reserved(level,entity.chunkPosition().x,entity.chunkPosition().z)) {
            completed(level,entity);return;
        }
        if(!topology.outside(entity.getX())) completed(level,entity);
        double activation=approachDistance(entity,topology);
        if (topology.distanceToSeam(entity.getX()) > activation && !topology.outside(entity.getX())) return;
        var group = entity.getSelfAndPassengers().toList();
        boolean playerOwned=group.stream().anyMatch(ServerPlayer.class::isInstance);
        if(!playerOwned && !topology.outside(entity.getX()) && topology.distanceToSeam(entity.getX())
                >Math.min(activation,Math.max(8,Math.abs(entity.getKnownMovement().x)*40)))return;
        if (group.stream().anyMatch(e -> ShipPassengerGuard.attached(e) || e instanceof Mob mob && mob.isLeashed())) {
            completed(level,entity);return;
        }
        double targetX = topology.destinationX(entity.getX());
        // Preload the opposite edge even before crossing, rather than an out-of-world image coordinate.
        double preloadX = topology.canonicalX(targetX);
        if (!topology.outside(entity.getX())) preloadX = entity.getX() < 0 ? topology.east()-1 : topology.west();
        var target = new ChunkPos((int)Math.floor(preloadX/16), entity.chunkPosition().z);
        request(level,target,playerOwned);
        if (!topology.outside(entity.getX())) return;
        double dx = targetX-entity.getX();
        var readiness=ready(level,group,dx);pending(level,entity,readiness);
        if (readiness!=Readiness.READY) {
            // Stay on the crossing side of the seam so even stationary entities retry next tick.
            double x = entity.getX() >= topology.east() ? topology.east() : topology.west()-0.001;
            double holdDx=x-entity.getX();
            if (Math.abs(holdDx)>1.0e-6 && group.stream().allMatch(e -> chunksReady(level,e.getBoundingBox().move(holdDx,0,0)))) moveGroup(entity,holdDx);
            return;
        }
        moveGroup(entity, dx);
        completed(level,entity);
    }
    public static double approachDistance(Entity entity,EastWestTopology topology) {
        return Math.min(topology.width()/4.0,Math.max(Math.max(PlanetConfig.ACTIVATION.getAsInt(),
                (PlanetConfig.RADIUS.getAsInt()+3)*16),Math.min(1024,Math.abs(entity.getKnownMovement().x)*40)));
    }
    private static void request(ServerLevel level,ChunkPos pos,boolean player) {
        var requests=REQUESTS.computeIfAbsent(level,ignored->new HashMap<>());
        long now=level.getGameTime();requests.entrySet().removeIf(e->now-e.getValue().refreshed>=100);
        var previous=requests.get(pos);
        int radius=PlanetConfig.RADIUS.getAsInt()+((player || previous!=null && previous.playerUntil>=now)?1:0);
        long playerUntil=player?now+40:previous==null?Long.MIN_VALUE:previous.playerUntil;
        if(previous!=null && now-previous.refreshed<20 && previous.radius==radius) {
            if(player)requests.put(pos,new Lease(previous.refreshed,playerUntil,radius));
            return;
        }
        if(previous==null && requests.size()>=16) {
            if(!player)return;
            // Stationary mobs must never occupy every slot while a player waits indefinitely.
            var victim=requests.entrySet().stream().min(Comparator
                    .<Map.Entry<ChunkPos,Lease>>comparingInt(e->e.getValue().playerUntil>=now?1:0)
                    .thenComparingLong(e->e.getValue().playerUntil).thenComparingLong(e->e.getValue().refreshed)
                    .thenComparingLong(e->e.getKey().toLong())).orElseThrow();
            level.getChunkSource().removeRegionTicket(PRELOAD,victim.getKey(),victim.getValue().radius,victim.getKey());
            requests.remove(victim.getKey());
        }
        if(previous!=null && previous.radius!=radius)level.getChunkSource().removeRegionTicket(PRELOAD,pos,previous.radius,pos);
        // Player images need the extra outer ring; ordinary entities retain the smaller crossing square.
        level.getChunkSource().addRegionTicket(PRELOAD,pos,radius,pos);
        requests.put(pos,new Lease(now,playerUntil,radius));
    }
    private static Readiness ready(ServerLevel level,java.util.List<Entity> group,double dx) {
        for(var member:group) {
            var box=member.getBoundingBox().move(dx,0,0);
            if(!chunksReady(level,box))return Readiness.MISSING_CHUNKS;
            if(!level.noCollision(member,box))return Readiness.COLLISION;
        }
        return Readiness.READY;
    }
    public static String diagnostics(ServerPlayer player) {
        var level=player.serverLevel();var topology=PlanetBorders.get(level.getWorldBorder());
        if(topology==null)return "East/west seam is disabled in this dimension.";
        int half=topology.width()/32,radius=PlanetConfig.RADIUS.getAsInt(),full=0,lit=0;
        boolean east=player.getX()>=0;int z0=player.chunkPosition().z;
        for(int d=0;d<=radius+1;d++)for(int dz=-radius-1;dz<=radius+1;dz++) {
            var chunk=level.getChunkSource().getChunkNow(east?-half+d:half-1-d,z0+dz);
            if(chunk!=null){full++;if(chunk.isLightCorrect())lit++;}
        }
        var pos=new ChunkPos(east?-half:half-1,z0);var requests=REQUESTS.get(level);
        long now=level.getGameTime();var lease=requests==null?null:requests.get(pos);
        if(lease!=null && now-lease.refreshed>=100)lease=null;
        long active=requests==null?0:requests.values().stream().filter(l->now-l.refreshed<100).count();
        var pending=PENDING.get(level);
        var reason=pending==null?null:pending.get(player.getRootVehicle());
        return "Destination "+pos+"; preview FULL="+full+"/"+((radius+2)*(radius*2+3))+", lit="+lit
                +"; preload="+(lease==null?"NONE":(lease.playerUntil>=level.getGameTime()?"PLAYER":"ENTITY")+", radius="+lease.radius)
                +"; leases="+active+"/16; gate="+(reason==null?"NO_PENDING_CROSSING":reason);
    }
    private static boolean chunksReady(ServerLevel level, net.minecraft.world.phys.AABB box) {
        for(int x=(int)Math.floor(box.minX/16);x<=(int)Math.floor(box.maxX/16);x++)
            for(int z=(int)Math.floor(box.minZ/16);z<=(int)Math.floor(box.maxZ/16);z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    private static void moveGroup(Entity root, double dx) {
        var members=root.getSelfAndPassengers().toList();
        var velocities=members.stream().map(Entity::getDeltaMovement).toList();
        // Relative position flags also preserve the client's creative/elytra movement and previous pose.
        root.teleportRelative(dx,0,0);
        if(root.getControllingPassenger() instanceof ServerPlayer controller) {
            var movement=(io.github.astelev.around_planet.mixin.VehicleMovementAccessor)controller.connection;
            movement.around_planet$vehicle(root);
            movement.around_planet$firstX(root.getX());movement.around_planet$firstY(root.getY());movement.around_planet$firstZ(root.getZ());
            movement.around_planet$lastX(root.getX());movement.around_planet$lastY(root.getY());movement.around_planet$lastZ(root.getZ());
            movement.around_planet$floating(false);movement.around_planet$floatingTicks(0);
        }
        for(var member:members) if(member instanceof ServerPlayer player) {
            // Set the destination watch view before move() updates entity trackers against it.
            // Otherwise a tracked vehicle is temporarily removed from its own passenger's client.
            ((io.github.astelev.around_planet.mixin.ChunkTrackingAccessor)player.serverLevel().getChunkSource().chunkMap)
                    .around_planet$updateChunkTracking(player);
            player.serverLevel().getChunkSource().move(player);
            if(member!=root) {
                for(var vehicle:members) if(!vehicle.getPassengers().isEmpty())
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(vehicle));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket(root));
            }
        }
        // Vanilla places riders after moving the vehicle; send each player its corrected position without dismounting.
        for(int i=0;i<members.size();i++) {
            var member=members.get(i);
            if(member instanceof ServerPlayer player && member!=root)
                player.teleportTo(player.getX(),player.getY(),player.getZ());
            member.setDeltaMovement(velocities.get(i));
            // A player's server-side velocity is not its predicted flight velocity. Do not send a
            // manufactured self motion packet (often zero) immediately after preserving client motion.
            if(member!=root || !(member instanceof ServerPlayer))member.hurtMarked=true;
        }
        for(var member:members) if(member instanceof ServerPlayer player && member!=root
                && player.connection.hasChannel(io.github.astelev.around_planet.network.SeamVehiclePayload.TYPE)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new io.github.astelev.around_planet.network.SeamVehiclePayload(root.level().dimension().location(),root.getId(),root.getUUID(),
                            root.getX(),root.getY(),root.getZ(),root.getYRot(),root.getXRot(),player.getVehicle().getId(),player.getVehicle().getUUID()));
        }
    }
    private TransferService() {}
}

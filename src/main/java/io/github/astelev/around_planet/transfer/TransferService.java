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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class TransferService {
    private static final TicketType<ChunkPos> PRELOAD = TicketType.create("around_planet_destination", Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final Map<ServerLevel, Map<ChunkPos, Long>> REQUESTS = new IdentityHashMap<>();

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        var level = event.getLevel();
        if (!(level instanceof Level world) || !world.dimension().equals(Level.OVERWORLD)
                || !PlanetConfig.SPEC.isLoaded() || !PlanetConfig.ENABLED.getAsBoolean()) return;
        try {
            PlanetBorders.bind(world.getWorldBorder(), new EastWestTopology(PlanetConfig.WIDTH.getAsInt()));
        } catch (IllegalArgumentException exception) {
            AroundPlanet.LOGGER.error("East/west transfer disabled: {}", exception.getMessage());
        }
    }
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        REQUESTS.forEach((level, requests) -> requests.keySet().forEach(pos ->
                level.getChunkSource().removeRegionTicket(PRELOAD,pos,PlanetConfig.RADIUS.getAsInt(),pos)));
        REQUESTS.clear();
    }
    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) PlanetBorders.unbind(level.getWorldBorder());
        if (event.getLevel() instanceof ServerLevel level) REQUESTS.remove(level);
    }
    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved() || entity.isPassenger()) return;
        var topology = PlanetBorders.get(level.getWorldBorder());
        if (topology == null || topology.distanceToSeam(entity.getX()) > Math.min(PlanetConfig.ACTIVATION.getAsInt(), topology.width()/4.0)
                && !topology.outside(entity.getX())) return;
        var group = entity.getSelfAndPassengers().toList();
        if (group.stream().anyMatch(e -> ShipPassengerGuard.attached(e) || e instanceof Mob mob && mob.isLeashed())) return;
        double targetX = topology.destinationX(entity.getX());
        // Preload the opposite edge even before crossing, rather than an out-of-world image coordinate.
        double preloadX = topology.canonicalX(targetX);
        if (!topology.outside(entity.getX())) preloadX = entity.getX() < 0 ? topology.east()-1 : topology.west();
        var target = new ChunkPos((int)Math.floor(preloadX/16), entity.chunkPosition().z);
        request(level, target);
        if (!topology.outside(entity.getX())) return;
        double dx = targetX-entity.getX();
        if (!ready(level, group, dx)) {
            // Stay on the crossing side of the seam so even stationary entities retry next tick.
            double x = entity.getX() >= topology.east() ? topology.east() : topology.west()-0.001;
            double holdDx=x-entity.getX();
            if (group.stream().allMatch(e -> chunksReady(level,e.getBoundingBox().move(holdDx,0,0)))) moveGroup(entity,holdDx);
            return;
        }
        moveGroup(entity, dx);
    }
    private static void request(ServerLevel level, ChunkPos pos) {
        var requests = REQUESTS.computeIfAbsent(level, ignored -> new HashMap<>());
        long now = level.getGameTime();
        requests.entrySet().removeIf(e -> now-e.getValue() >= 100);
        Long previous = requests.get(pos);
        if (previous != null && now-previous < 20 || previous == null && requests.size() >= 16) return;
        int radius=PlanetConfig.RADIUS.getAsInt();
        level.getChunkSource().addRegionTicket(PRELOAD, pos, radius, pos);
        // The vanilla ticket manager schedules loading on following ticks.
        // Even getChunkFuture blocks on the server thread in 1.21.1; never call it here.
        requests.put(pos,now);
    }
    private static boolean ready(ServerLevel level, java.util.List<Entity> group, double dx) {
        for (var member : group) {
            var box=member.getBoundingBox().move(dx,0,0);
            if (!chunksReady(level,box)) return false;
            if (!level.noCollision(member,box)) return false;
        }
        return true;
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
        root.teleportTo(root.getX()+dx,root.getY(),root.getZ());
        // Vanilla places riders after moving the vehicle; send each player its corrected position without dismounting.
        for(int i=0;i<members.size();i++) {
            var member=members.get(i);
            if(member instanceof ServerPlayer player && member!=root)
                player.teleportTo(player.getX(),player.getY(),player.getZ());
            member.setDeltaMovement(velocities.get(i));
            member.hurtMarked=true;
        }
    }
    private TransferService() {}
}

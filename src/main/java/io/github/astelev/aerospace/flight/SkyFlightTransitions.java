package io.github.astelev.aerospace.flight;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.astelev.aerospace.mixin.sable.PhysicsChunkTicketsAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Quaterniond;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Optional Sable integration: main-thread tickets, never blocking chunk futures. */
public final class SkyFlightTransitions {
    private static final TicketType<ChunkPos> DESCENT = TicketType.create("aerospace_descent", java.util.Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final Map<ServerLevel, Map<UUID, Flight>> FLIGHTS = new IdentityHashMap<>();
    private static final Map<ServerLevel, ChunkRequestBudget> REQUESTS = new IdentityHashMap<>();
    private static final Map<ServerLevel, TickWorkBudget> COLLIDERS = new IdentityHashMap<>();

    private SkyFlightTransitions() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkyFlightTransitions::beforePhysics);
        NeoForge.EVENT_BUS.addListener(SkyFlightTransitions::afterPhysics);
        NeoForge.EVENT_BUS.addListener(SkyFlightTransitions::afterLevelTick);
        NeoForge.EVENT_BUS.addListener(SkyFlightTransitions::unload);
    }

    public static void markSky(ServerLevel level, ServerSubLevel ship) {
        var flights = FLIGHTS.computeIfAbsent(level, key -> new HashMap<>());
        var flight = flights.computeIfAbsent(ship.getUniqueId(), key -> new Flight());
        flight.lastSeen = level.getGameTime();
        if (!flight.holding) {
            flight.capture(ship);
        }
    }

    private static void beforePhysics(ForgeSablePrePhysicsTickEvent event) {
        update(event.getPhysicsSystem(), event.getTimeStep(), false);
    }

    private static void afterPhysics(ForgeSablePostPhysicsTickEvent event) {
        update(event.getPhysicsSystem(), event.getTimeStep(), true);
    }

    private static void update(SubLevelPhysicsSystem system, double timeStep, boolean afterStep) {
        var level = system.getLevel();
        var flights = FLIGHTS.get(level);
        if (flights == null) {
            return;
        }
        var container = SubLevelContainer.getContainer(level);
        var pipeline = system.getPipeline();
        if (!FlightConfig.ENABLED.getAsBoolean()) {
            for (var entry : flights.entrySet()) {
                if (entry.getValue().holding && container.getSubLevel(entry.getKey()) instanceof ServerSubLevel ship) {
                    pipeline.resetVelocity(ship);
                    pipeline.addLinearAndAngularVelocity(ship, entry.getValue().linearVelocity, entry.getValue().angularVelocity);
                }
            }
            flights.clear();
            return;
        }
        var iterator = flights.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var flight = entry.getValue();
            if (!(container.getSubLevel(entry.getKey()) instanceof ServerSubLevel ship)
                    || ship.isRemoved() || level.getGameTime() - flight.lastSeen > 20) {
                iterator.remove();
                continue;
            }
            double threshold = Math.max(FlightConfig.MIN_ALTITUDE.getAsInt(), level.getMaxBuildHeight() + 21.0);
            // Sable's public world bounds are refreshed once per game tick.
            // Evaluate the current pose here so fast/rotating ships cannot cross
            // the handoff boundary between physics substeps unnoticed.
            var local = ship.getPlot().getBoundingBox();
            flight.bounds.set(local.minX(), local.minY(), local.minZ(), local.maxX() + 1.0, local.maxY() + 1.0, local.maxZ() + 1.0);
            flight.bounds.transform(ship.logicalPose(), flight.transform, flight.bounds);
            double minY = flight.bounds.minY();
            var velocity = pipeline.getLinearVelocity(ship, new Vector3d());
            double predictedMinY = minY + Math.min(0, velocity.y * timeStep);
            boolean approaching = flight.holding || minY <= threshold || predictedMinY <= threshold
                    || minY <= threshold + 64 && velocity.y < -1.0;
            if (!approaching) {
                if (minY > threshold) {
                    flight.capture(ship);
                }
                continue;
            }

            boolean ready = requestFootprint(level, ship, flight.bounds, flight.holding ? flight.position : ship.logicalPose().position());
            if (ready) {
                ready = prepareColliders(system, ship, flight.bounds, flight.holding ? flight.position : ship.logicalPose().position());
            }
            if (ready) {
                if (flight.holding) {
                    pipeline.resetVelocity(ship);
                    pipeline.addLinearAndAngularVelocity(ship, flight.linearVelocity, flight.angularVelocity);
                    flight.holding = false;
                }
                if (minY <= threshold) {
                    iterator.remove();
                } else {
                    flight.capture(ship);
                }
                continue;
            }

            // Predict the next substep, then check again after it for gravity,
            // rotation, forces, or an unexpectedly large displacement.
            if (flight.holding || predictedMinY <= threshold) {
                if (!flight.holding) {
                    flight.linearVelocity.set(velocity);
                    pipeline.getAngularVelocity(ship, flight.angularVelocity);
                    flight.holding = true;
                }
                pipeline.teleport(ship, flight.position, flight.orientation);
                pipeline.resetVelocity(ship);
                system.updatePose(ship);
                ship.updateBoundingBox();
                if (level.getGameTime() - flight.lastNotice >= 20) {
                    flight.lastNotice = level.getGameTime();
                    var b = ship.boundingBox();
                    for (var player : level.players()) {
                        if (player.getX() >= b.minX() - 2 && player.getX() <= b.maxX() + 2
                                && player.getZ() >= b.minZ() - 2 && player.getZ() <= b.maxZ() + 2
                                && player.getY() >= b.minY() - 2 && player.getY() <= b.maxY() + 3) {
                            player.displayClientMessage(Component.translatable("aerospace.flight.waiting_for_terrain"), true);
                        }
                    }
                }
            } else if (afterStep && minY > threshold) {
                flight.capture(ship);
            }
        }
    }

    private static boolean requestFootprint(ServerLevel level, ServerSubLevel ship, BoundingBox3d bounds, Vector3d target) {
        var budget = REQUESTS.computeIfAbsent(level, key -> new ChunkRequestBudget(4, 256));
        // Include a ring for Sable's collision bounds. During a hold use the
        // saved safe position rather than the attempted out-of-bounds pose.
        double dx = target.x - ship.logicalPose().position().x;
        double dz = target.z - ship.logicalPose().position().z;
        int minX = ((int) Math.floor(bounds.minX() + dx) >> 4) - 1;
        int maxX = ((int) Math.floor(bounds.maxX() + dx) >> 4) + 1;
        int minZ = ((int) Math.floor(bounds.minZ() + dz) >> 4) - 1;
        int maxZ = ((int) Math.floor(bounds.maxZ() + dz) >> 4) + 1;
        long count = ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
        if (count <= 0 || count > 256) {
            // Fail closed for a ship too large for the bounded queue.
            return false;
        }
        boolean ready = true;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkPos.asLong(x, z);
                budget.request(key, level.getGameTime(), chunk -> {
                    var pos = new ChunkPos(chunk);
                    level.getChunkSource().addRegionTicket(DESCENT, pos, 2, pos);
                });
                ready &= level.getChunkSource().getChunkNow(x, z) != null
                        && level.getChunkSource().chunkMap.getDistanceManager().inBlockTickingRange(key);
            }
        }
        return ready;
    }

    private static void afterLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var budget = REQUESTS.get(level);
            if (budget != null) {
                budget.expire(level.getGameTime(), 20, key -> {
                    var pos = new ChunkPos(key);
                    level.getChunkSource().removeRegionTicket(DESCENT, pos, 2, pos);
                });
            }
        }
    }

    private static boolean prepareColliders(SubLevelPhysicsSystem system, ServerSubLevel ship, BoundingBox3d bounds, Vector3d target) {
        var level = system.getLevel();
        var budget = COLLIDERS.computeIfAbsent(level, key -> new TickWorkBudget(4));
        var manager = (PhysicsChunkTicketsAccessor) system.getTicketManager();
        double dx = target.x - ship.logicalPose().position().x;
        double dz = target.z - ship.logicalPose().position().z;
        int minX = ((int) Math.floor(bounds.minX() + dx) >> 4) - 1;
        int maxX = ((int) Math.floor(bounds.maxX() + dx) >> 4) + 1;
        int minZ = ((int) Math.floor(bounds.minZ() + dz) >> 4) - 1;
        int maxZ = ((int) Math.floor(bounds.maxZ() + dz) >> 4) + 1;
        boolean ready = true;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    ready = false;
                    continue;
                }
                for (int index = 0; index < chunk.getSectionsCount(); index++) {
                    if (chunk.getSection(index).hasOnlyAir()) {
                        continue;
                    }
                    int y = level.getSectionYFromSectionIndex(index);
                    var pos = SectionPos.of(x, y, z);
                    if (manager.aerospace$physicsChunks().containsKey(pos) || budget.acquire(level.getGameTime())) {
                        // Already loaded real chunks only: this Sable call cannot
                        // wait on generation. Refresh existing section tickets as
                        // well so a large handoff cannot expire earlier uploads.
                        manager.aerospace$addSection(level, system.getPipeline(), pos, x, y, z, index, level.getGameTime());
                    } else {
                        ready = false;
                    }
                }
            }
        }
        return ready;
    }

    private static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FLIGHTS.remove(level);
            REQUESTS.remove(level);
            COLLIDERS.remove(level);
        }
    }

    private static final class Flight {
        private final Vector3d position = new Vector3d();
        private final Quaterniond orientation = new Quaterniond();
        private final Vector3d linearVelocity = new Vector3d();
        private final Vector3d angularVelocity = new Vector3d();
        private final BoundingBox3d bounds = new BoundingBox3d();
        private final Matrix4d transform = new Matrix4d();
        private long lastSeen;
        private long lastNotice = Long.MIN_VALUE / 2;
        private boolean holding;

        private void capture(ServerSubLevel ship) {
            position.set(ship.logicalPose().position());
            orientation.set(ship.logicalPose().orientation());
        }
    }
}

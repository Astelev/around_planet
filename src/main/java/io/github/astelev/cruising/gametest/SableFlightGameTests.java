package io.github.astelev.cruising.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import io.github.astelev.cruising.flight.FlightHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registered only in a development runtime with Sable installed. */
@PrefixGameTestTemplate(false)
public final class SableFlightGameTests {
    @GameTest(template = "empty", templateNamespace = "cruising", timeoutTicks = 100)
    public static void nativeTrackedPassengerSurvivesPoseHandoff(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(1,2,1));
        level.setBlockAndUpdate(anchor, Blocks.IRON_BLOCK.defaultBlockState());
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor),
                new BoundingBox3i(anchor.getX(),anchor.getY(),anchor.getZ(),anchor.getX(),anchor.getY(),anchor.getZ()));
        var container = SubLevelContainer.getContainer(level);
        var system = container.physicsSystem();
        var pipeline = system.getPipeline();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "AeroSkyCrew"));
        var tracking = (dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension) player;
        boolean listed = level.players().contains(player);
        try {
            var previous = new Vector3d(1100008, 1000, 1100008);
            var current = new Vector3d(previous).add(1024,0,0);
            pipeline.teleport(ship, previous, ship.logicalPose().orientation());
            ship.updateBoundingBox();
            ship.updateLastPose();
            pipeline.teleport(ship, current, ship.logicalPose().orientation());
            tracking.sable$setTrackingSubLevel(ship);
            player.setPos(previous.x + 512, previous.y + 1, previous.z);
            helper.assertTrue(!io.github.astelev.cruising.flight.ShipGeometry.containsPassenger(ship,player),
                    "Probe must be outside both endpoint hulls during pose handoff");
            helper.assertTrue(FlightHooks.isHighShipEntity(player), "Native tracking must grant temporary terrain mode");
            var pos = player.chunkPosition();
            helper.assertTrue(level.getChunkSource().getChunkNow(pos.x,pos.z) == null, "Probe terrain must stay unloaded");
            if (!listed) level.players().add(player);
            system.getTicketManager().update(level,container,system,pipeline,1.0/20.0);
            helper.assertTrue(container.getSubLevel(ship.getUniqueId()) == ship && !ship.isRemoved(),
                    "Native tracked passenger must prevent sky ship unload during pose handoff");
            helper.assertTrue(level.getChunkSource().getChunkNow(pos.x,pos.z) == null,
                    "Protecting crew must not force terrain generation");
            tracking.sable$setTrackingSubLevel(null);
            tracking.sable$setLastTrackingSubLevelID(null);
            helper.assertTrue(!io.github.astelev.cruising.flight.ShipGeometry.isPassenger(ship,player),
                    "Unattached players between poses must not become crew");
            helper.assertTrue(!FlightHooks.isHighShipEntity(player), "Removed native attachment must end the exemption outside the hull");
            helper.succeed();
        } finally {
            tracking.sable$setTrackingSubLevel(null);
            tracking.sable$setLastTrackingSubLevelID(null);
            if (!listed) level.players().remove(player);
            if (container.getSubLevel(ship.getUniqueId()) != null) container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
        }
    }

    @GameTest(template = "empty", templateNamespace = "cruising", timeoutTicks = 100)
    public static void nativeForcedDescentKeepsVelocity(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(anchor, Blocks.IRON_BLOCK.defaultBlockState());
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor),
                new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ()));
        var container = SubLevelContainer.getContainer(level);
        var system = container.physicsSystem();
        var pipeline = system.getPipeline();
        var target = new ChunkPos(200000 + level.random.nextInt(10000), 200000);
        container.addForceLoadTicket(ship, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
        try {
            pipeline.teleport(ship, new Vector3d(target.getMiddleBlockX(), 350, target.getMiddleBlockZ()), ship.logicalPose().orientation());
            system.updatePose(ship);
            ship.updateBoundingBox();
            ship.updateLastPose();
            pipeline.addLinearAndAngularVelocity(ship, new Vector3d(123, -45, 67), new Vector3d(0.2, 0.3, 0.4));
            var linear = pipeline.getLinearVelocity(ship, new Vector3d());
            var angular = pipeline.getAngularVelocity(ship, new Vector3d());
            var position = new Vector3d(ship.logicalPose().position());
            helper.assertTrue(level.getChunkSource().getChunkNow(target.x, target.z) == null, "Descent probe must start over missing terrain");

            // Exercise the actual Sable force-loaded-ship path, including its
            // blocking native FULL request, without stepping the physics world.
            system.getTicketManager().update(level, container, system, pipeline, 1.0 / 20.0);

            helper.assertTrue(level.getChunkSource().getChunkNow(target.x, target.z) != null, "Sable must load real terrain on descent");
            helper.assertTrue(linear.distance(pipeline.getLinearVelocity(ship, new Vector3d())) < 1.0e-8,
                    "Native terrain loading must preserve all linear velocity components");
            helper.assertTrue(angular.distance(pipeline.getAngularVelocity(ship, new Vector3d())) < 1.0e-8,
                    "Native terrain loading must preserve all angular velocity components");
            helper.assertTrue(position.distance(ship.logicalPose().position()) < 1.0e-8,
                    "CruisingSpeed must not roll the ship back during native loading");
            helper.succeed();
        } finally {
            container.removeForceLoadTicket(ship, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
            container.removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
        }
    }

    @GameTest(template = "empty", templateNamespace = "cruising", timeoutTicks = 100)
    public static void fastFlightUsesLivePassengerBounds(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(anchor, Blocks.IRON_BLOCK.defaultBlockState());
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor),
                new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ()));
        var container = SubLevelContainer.getContainer(level);
        var pipeline = container.physicsSystem().getPipeline();
        var previous = new Vector3d(1000008, 1000, 1000008);
        var current = new Vector3d(previous).add(1024, 0, 0);
        try {
            pipeline.teleport(ship, previous, ship.logicalPose().orientation());
            ship.updateBoundingBox();
            ship.updateLastPose();
            pipeline.teleport(ship, current, ship.logicalPose().orientation());
            // Deliberately leave Sable's public AABB at the previous tick's pose.
            helper.assertTrue(ship.boundingBox().maxX() < current.x - 1000, "Probe must have stale cached bounds");
            var passenger = FakePlayerFactory.getMinecraft(level);
            passenger.setPos(current.x, current.y + 1, current.z);
            helper.assertTrue(FlightHooks.isHighShipEntity(passenger), "Live-pose passenger must keep temporary loading despite a stale AABB");
            var pos = passenger.chunkPosition();
            helper.assertTrue(level.getChunkSource().getChunkNow(pos.x, pos.z) == null, "Probe column must be unloaded");
            helper.assertTrue(FlightHooks.movementChunk(passenger, level, pos.x, pos.z)
                    instanceof io.github.astelev.cruising.flight.TemporaryLevelChunk,
                    "Fast movement must not fall through to synchronous terrain generation");
            helper.assertTrue(level.getChunkSource().getChunkNow(pos.x, pos.z) == null, "Movement fallback must leave real terrain unloaded");
            boolean listed = level.players().contains(passenger);
            if (!listed) {
                level.players().add(passenger);
            }
            try {
                // Sable also uses a cached-AABB player predicate to decide
                // whether to unload the ship. Exercise that real code path.
                container.physicsSystem().getTicketManager().update(level, container, container.physicsSystem(), pipeline, 1.0 / 20.0);
                helper.assertTrue(!ship.isRemoved() && container.getSubLevel(ship.getUniqueId()) == ship,
                        "Sable must not unload a live-pose player-inhabited ship because its cached bounds lag");
                helper.assertTrue(level.getChunkSource().getChunkNow(pos.x, pos.z) == null,
                        "Keeping the fast ship active must not generate terrain");
            } finally {
                if (!listed) {
                    level.players().remove(passenger);
                }
            }
            passenger.setPos(previous.x, previous.y + 1, previous.z);
            helper.assertTrue(FlightHooks.isHighShipEntity(passenger), "Passenger at the previous pose must remain associated during tick handoff");
            passenger.setPos(previous.x + 512, previous.y + 1, previous.z);
            helper.assertTrue(!FlightHooks.isHighShipEntity(passenger), "Unrelated players between distant poses must not be treated as crew");
            passenger.setPosRaw(current.x, current.y + 1, current.z);
            helper.assertTrue(FlightHooks.isHighShipEntity(passenger),
                    "Raw movement must use the updated passenger position before its AABB is refreshed");
            pipeline.teleport(ship, new Vector3d(current.x, 350, current.z), ship.logicalPose().orientation());
            passenger.setPos(current.x, current.y + 1, current.z);
            helper.assertTrue(!FlightHooks.isHighShipEntity(passenger), "Live descent must end temporary loading even while cached bounds remain high");

            var low = new Vector3d(previous).add(4096, -936, 0);
            var high = new Vector3d(low).add(1024, 936, 0);
            pipeline.teleport(ship, low, ship.logicalPose().orientation());
            ship.updateBoundingBox();
            ship.updateLastPose();
            pipeline.teleport(ship, high, ship.logicalPose().orientation());
            passenger.setPos(high.x, high.y + 1, high.z);
            var lowColumn = new ChunkPos((int) low.x >> 4, (int) low.z >> 4);
            helper.assertTrue(level.getChunkSource().getChunkNow(lowColumn.x, lowColumn.z) == null,
                    "Stale ascent column must start unloaded");
            if (!listed) {
                level.players().add(passenger);
            }
            try {
                container.physicsSystem().getTicketManager().update(level, container, container.physicsSystem(), pipeline, 1.0 / 20.0);
                helper.assertTrue(!ship.isRemoved(), "Rapidly ascending player-inhabited ship must stay active");
                helper.assertTrue(level.getChunkSource().getChunkNow(lowColumn.x, lowColumn.z) == null,
                        "Cached low-altitude bounds must not load collision terrain after a rapid ascent");
            } finally {
                if (!listed) {
                    level.players().remove(passenger);
                }
            }
            helper.succeed();
        } finally {
            if (!ship.isRemoved()) {
                container.removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
            }
        }
    }

    @GameTest(template = "empty", templateNamespace = "cruising", timeoutTicks = 100)
    public static void skyShipBypassesForcedTerrainGeneration(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(anchor, Blocks.IRON_BLOCK.defaultBlockState());
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor),
                new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ()));
        var container = SubLevelContainer.getContainer(level);
        try {
            var system = container.physicsSystem();
            var pipeline = system.getPipeline();
            var target = new ChunkPos(100000, 100000);
            pipeline.teleport(ship, new Vector3d(target.getMiddleBlockX(), 400, target.getMiddleBlockZ()), ship.logicalPose().orientation());
            system.updatePose(ship);
            ship.updateBoundingBox();
            ship.updateLastPose();
            helper.assertTrue(ship.boundingBox().minY() > 356, "Probe hull must be above the threshold");

            var manager = new PhysicsChunkTicketManager();
            var method = PhysicsChunkTicketManager.class.getDeclaredMethod("inhabitChunk", ServerLevel.class,
                    DistanceManager.class, UUID.class, long.class, long.class, int.class, int.class);
            method.setAccessible(true);
            var ticketsField = PhysicsChunkTicketManager.class.getDeclaredField("forcedInhabitedChunks");
            ticketsField.setAccessible(true);
            var distanceManager = level.getChunkSource().chunkMap.getDistanceManager();
            method.invoke(manager, level, distanceManager, ship.getUniqueId(), level.getGameTime(), target.toLong(), target.x, target.z);
            helper.assertTrue(((Map<?, ?>) ticketsField.get(manager)).isEmpty(), "Sky requests must not create Sable terrain tickets");
            helper.assertTrue(level.getChunkSource().getChunkNow(target.x, target.z) == null, "Sky request must not generate remote terrain");

            var passenger = FakePlayerFactory.getMinecraft(level);
            passenger.setPos(ship.logicalPose().position().x, 401, ship.logicalPose().position().z);
            helper.assertTrue(FlightHooks.isHighShipEntity(passenger), "A ship passenger should use the temporary view");
            var skip = ChunkMap.class.getDeclaredMethod("skipPlayer", ServerPlayer.class);
            skip.setAccessible(true);
            helper.assertTrue((Boolean) skip.invoke(level.getChunkSource().chunkMap, passenger), "Sky passenger terrain tickets must be suspended");
            var movement = FlightHooks.movementChunk(passenger, level, target.x, target.z);
            helper.assertTrue(movement instanceof io.github.astelev.cruising.flight.TemporaryLevelChunk, "Missing movement terrain must be temporary");
            passenger.setPos(anchor.getX(), 64, anchor.getZ());
            helper.assertTrue(!(Boolean) skip.invoke(level.getChunkSource().chunkMap, passenger), "Ground player terrain tickets must remain normal");

            // Below the threshold, the same intercepted method must execute normally.
            pipeline.teleport(ship, new Vector3d(anchor.getX(), 350, anchor.getZ()), ship.logicalPose().orientation());
            system.updatePose(ship);
            ship.updateBoundingBox();
            var local = new ChunkPos(anchor);
            method.invoke(manager, level, distanceManager, ship.getUniqueId(), level.getGameTime(), local.toLong(), local.x, local.z);
            helper.assertTrue(!((Map<?, ?>) ticketsField.get(manager)).isEmpty(), "Descent must restore ordinary Sable tickets");
            var expire = PhysicsChunkTicketManager.class.getDeclaredMethod("expireForcedInhabitedChunks", long.class, DistanceManager.class);
            expire.setAccessible(true);
            expire.invoke(manager, level.getGameTime() + 21, distanceManager);
            helper.succeed();
        } finally {
            container.removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
        }
    }
}

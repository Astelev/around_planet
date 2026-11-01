package io.github.astelev.aerospace.gametest;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import io.github.astelev.aerospace.flight.FlightHooks;
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
    @GameTest(template = "empty", templateNamespace = "aerospace", timeoutTicks = 400)
    public static void fastDescentWaitsForRealTerrain(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(anchor, Blocks.IRON_BLOCK.defaultBlockState());
        var ship = SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor),
                new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX(), anchor.getY(), anchor.getZ()));
        var container = SubLevelContainer.getContainer(level);
        var system = container.physicsSystem();
        var pipeline = system.getPipeline();
        var target = new ChunkPos(200000 + level.random.nextInt(10000), 200000);
        pipeline.teleport(ship, new Vector3d(target.getMiddleBlockX(), 1000, target.getMiddleBlockZ()), ship.logicalPose().orientation());
        system.updatePose(ship);
        ship.updateBoundingBox();
        ship.updateLastPose();
        // A Sable force-load ticket represents the non-player loading path.
        container.addForceLoadTicket(ship, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
        var launched = new java.util.concurrent.atomic.AtomicBoolean();
        var waited = new java.util.concurrent.atomic.AtomicBoolean();
        helper.onEachTick(() -> {
            if (level.getChunkSource().getChunkNow(target.x, target.z) == null) {
                helper.assertTrue(ship.boundingBox().minY() > 356, "Ship must stay above missing real terrain");
                if (launched.get() && pipeline.getLinearVelocity(ship, new Vector3d()).lengthSquared() < 1) {
                    waited.set(true);
                }
            }
        });
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    pipeline.addLinearAndAngularVelocity(ship, new Vector3d(0, -4000, 0), new Vector3d());
                    launched.set(true);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(!ship.isRemoved(), "Forced ship must remain active over temporary air");
                    helper.assertTrue(ship.boundingBox().minY() <= 356, "Ship must eventually finish the handoff");
                    helper.assertTrue(level.getChunkSource().getChunkNow(target.x, target.z) != null, "Handoff requires real terrain");
                    helper.assertTrue(waited.get(), "Rapid descent must exercise a nonblocking hold before terrain is ready");
                })
                .thenExecute(() -> {
                    container.removeForceLoadTicket(ship, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
                    container.removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", templateNamespace = "aerospace", timeoutTicks = 100)
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
            helper.assertTrue(movement instanceof io.github.astelev.aerospace.flight.TemporaryLevelChunk, "Missing movement terrain must be temporary");
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

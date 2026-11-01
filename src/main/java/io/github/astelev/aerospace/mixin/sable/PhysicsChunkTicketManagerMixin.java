package io.github.astelev.aerospace.mixin.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import io.github.astelev.aerospace.flight.FlightConfig;
import io.github.astelev.aerospace.flight.HighAltitudePolicy;
import io.github.astelev.aerospace.flight.SkyFlightTransitions;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Sable has already decided that a player or a sub-level loading ticket keeps
 * this ship active before invoking inhabitChunk. At sky altitude it needs no
 * terrain collision sections, so its synchronous terrain request is unnecessary.
 * No ChunkHolder, block data, save flags, or player tickets are changed here.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager", remap = false)
public abstract class PhysicsChunkTicketManagerMixin {
    @Inject(method = "inhabitChunk", at = @At("HEAD"), cancellable = true)
    private void aerospace$skipSkyTerrainTicket(ServerLevel level, DistanceManager distanceManager,
                                                UUID subLevelId, long gameTime, long chunkLong,
                                                int x, int z, CallbackInfo ci) {
        var container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        var ship = container.getSubLevel(subLevelId);
        if (ship == null) {
            return;
        }

        if (HighAltitudePolicy.maySkipTerrainTicket(
                FlightConfig.ENABLED.getAsBoolean(),
                level.dimension().equals(Level.OVERWORLD),
                container.inBounds(x, z),
                ship.boundingBox().minY(),
                level.getMaxBuildHeight(),
                FlightConfig.MIN_ALTITUDE.getAsInt())) {
            if (ship instanceof ServerSubLevel serverShip) {
                SkyFlightTransitions.markSky(level, serverShip);
            }
            ci.cancel();
        }
    }
}

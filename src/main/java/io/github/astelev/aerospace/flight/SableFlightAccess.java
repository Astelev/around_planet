package io.github.astelev.aerospace.flight;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Loaded only when Sable is installed. */
final class SableFlightAccess {
    private SableFlightAccess() {
    }

    static boolean isHighShipEntity(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || entity.getY() <= FlightConfig.MIN_ALTITUDE.getAsInt()) {
            return false;
        }
        SubLevel ship = Sable.HELPER.getTrackingSubLevel(entity);
        if (ship == null && entity.getVehicle() != null) {
            ship = Sable.HELPER.getTrackingSubLevel(entity.getVehicle());
        }
        // Also cover passengers standing/jumping inside a cabin, as Sable's
        // own player-inhabited test does, without granting free-flight mode.
        if (ship == null) {
            var container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return false;
            }
            for (var candidate : container.getAllSubLevels()) {
                var b = candidate.boundingBox();
                var center = entity.getBoundingBox().getCenter();
                if (!candidate.isRemoved()
                        && center.x >= b.minX() - 1 && center.x <= b.maxX() + 1
                        && center.z >= b.minZ() - 1 && center.z <= b.maxZ() + 1
                        && center.y >= b.minY() - 1 && center.y <= b.maxY() + 2) {
                    ship = candidate;
                    break;
                }
            }
        }
        return ship != null && !ship.isRemoved() && HighAltitudePolicy.maySkipTerrainTicket(
                FlightConfig.ENABLED.getAsBoolean(), level.dimension().equals(Level.OVERWORLD), false,
                ship.boundingBox().minY(), level.getMaxBuildHeight(), FlightConfig.MIN_ALTITUDE.getAsInt());
    }
}

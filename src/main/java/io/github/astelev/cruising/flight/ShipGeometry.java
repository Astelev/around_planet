package io.github.astelev.cruising.flight;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4d;

/** Live pose geometry; Sable's cached world AABB may be one game tick behind. */
public final class ShipGeometry {
    private ShipGeometry() {
    }

    public static double currentMinY(SubLevel ship) {
        var bounds = currentBounds(ship);
        return bounds == null ? Double.NEGATIVE_INFINITY : bounds.minY();
    }

    public static BoundingBox3d currentBounds(SubLevel ship) {
        return atPose(ship, ship.logicalPose());
    }

    /** Native attachment is stronger evidence than an asynchronously sampled AABB. */
    public static boolean isPassenger(SubLevel ship, Entity entity) {
        if (ship.isRemoved() || entity.level() != ship.getLevel()) return false;
        if (Sable.HELPER.getTrackingSubLevel(entity) == ship) return true;
        var vehicle = entity.getVehicle();
        for (int depth = 0; vehicle != null && depth < 16; depth++, vehicle = vehicle.getVehicle()) {
            if (Sable.HELPER.getTrackingSubLevel(vehicle) == ship || Sable.HELPER.getContaining(vehicle) == ship) return true;
        }
        return containsPassenger(ship, entity);
    }

    public static boolean containsPassenger(SubLevel ship, Entity entity) {
        var current = atPose(ship, ship.logicalPose());
        if (contains(current, entity)) {
            return true;
        }
        // Ship and passenger positions are updated in different parts of a tick.
        // Test the last pose separately, rather than filling the huge swept box
        // between poses and treating unrelated players along the route as crew.
        return contains(atPose(ship, ship.lastPose()), entity);
    }

    private static boolean contains(BoundingBox3d bounds, Entity entity) {
        if (bounds == null) {
            return false;
        }
        // Entity.setPosRaw performs NeoForge's terrain lookup before its caller
        // refreshes the entity AABB. Use the already-updated position here.
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() / 2.0;
        double z = entity.getZ();
        return x >= bounds.minX() - 1 && x <= bounds.maxX() + 1
                && z >= bounds.minZ() - 1 && z <= bounds.maxZ() + 1
                && y >= bounds.minY() - 1 && y <= bounds.maxY() + 2;
    }

    private static BoundingBox3d atPose(SubLevel ship, Pose3dc pose) {
        var local = ship.getPlot().getBoundingBox();
        if (local == null) {
            return null;
        }
        var world = new BoundingBox3d(local.minX(), local.minY(), local.minZ(),
                local.maxX() + 1.0, local.maxY() + 1.0, local.maxZ() + 1.0);
        return world.transform(pose, new Matrix4d(), world);
    }
}

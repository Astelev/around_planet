package io.github.astelev.around_planet.transfer;

import io.github.astelev.around_planet.AroundPlanet;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;

/** Do not detach a Sable passenger before whole-ship transfer is implemented. No mandatory Sable dependency. */
public final class ShipPassengerGuard {
    private static boolean initialized, unavailable;
    private static Object helper;
    private static Method tracking, lastTracking;
    public static boolean attached(Entity entity) {
        if (!ModList.get().isLoaded("sable")) return false;
        try {
            if (!initialized) {
                initialized = true;
                helper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
                tracking = helper.getClass().getMethod("getTrackingSubLevel", Entity.class);
                lastTracking = Class.forName("dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension")
                        .getMethod("sable$getLastTrackingSubLevelID");
            }
            if (unavailable) return true;
            return tracking.invoke(helper, entity) != null || lastTracking.invoke(entity) != null;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            if (!unavailable) AroundPlanet.LOGGER.warn("Sable passenger guard unavailable; managed portal transfers are suspended", exception);
            unavailable = true;
            return true;
        }
    }
    private ShipPassengerGuard() {}
}

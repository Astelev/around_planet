package io.github.astelev.aerospace.compat.dh;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/** Re-rank waiting tasks only; native limits and in-progress jobs remain intact. */
public final class DhGenerationPriority {
    private static Access access;
    private static final ThreadLocal<SortContext> SORT_MOTION = new ThreadLocal<>();
    private record SortContext(boolean enabled, FlightPathPriority.Motion motion) {}
    public static void beginSort() {
        SORT_MOTION.set(new SortContext(DhFlightState.bypassMotionPause() && DhFlightConfig.FORWARD_PRIORITY.getAsBoolean(),
                DhFlightState.travelMotion()));
    }
    public static void endSort() { SORT_MOTION.remove(); }
    private DhGenerationPriority() {}

    public static Object rank(Object original, Object target, Map.Entry<?, ?> entry) {
        if (!DhFlightState.bypassMotionPause() || !DhFlightConfig.NEARBY_PRIORITY.getAsBoolean()) return original;
        try {
            var a = access();
            long pos = ((Number) entry.getKey()).longValue();
            return a.pair.newInstance(entry.getValue(), priority(a, pos, target, DhFlightState.travelMotion()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("DH 3.3.3 generation priority link failed", e);
        }
    }

    public static int compare(int original, Object target, long first, long second) {
        var context = SORT_MOTION.get();
        boolean enabled = context == null ? DhFlightState.bypassMotionPause() && DhFlightConfig.FORWARD_PRIORITY.getAsBoolean() : context.enabled;
        var motion = context == null ? DhFlightState.travelMotion() : context.motion;
        if (!enabled || !motion.moving()) return original;
        try {
            var a = access();
            int result = Integer.compare(priority(a, first, target, motion), priority(a, second, target, motion));
            return result == 0 ? original : result;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("DH queue comparison link failed", e); }
    }

    public static int compareSections(int original, Object target, Object first, Object second) {
        var context = SORT_MOTION.get();
        if (context != null ? !context.enabled : !DhFlightState.bypassMotionPause() || !DhFlightConfig.FORWARD_PRIORITY.getAsBoolean()) return original;
        try {
            var a = access();
            return compare(original, target, a.sectionPos.getLong(first), a.sectionPos.getLong(second));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("DH mesh queue link failed", e); }
    }

    private static int priority(Access a, long pos, Object target, FlightPathPriority.Motion motion) throws ReflectiveOperationException {
        int distance = (Integer) a.distance.invoke(null, pos, target);
        if (SORT_MOTION.get() != null ? SORT_MOTION.get().enabled : DhFlightConfig.FORWARD_PRIORITY.getAsBoolean()) {
            double dx = (Integer) a.centerX.invoke(null, pos) - (double) a.targetX.getInt(target);
            double dz = (Integer) a.centerZ.invoke(null, pos) - (double) a.targetZ.getInt(target);
            distance = FlightPathPriority.distance(distance, dx, dz, motion);
        }
        return score(distance, (Byte) a.detail.invoke(null, pos));
    }

    public static int score(int signedEdgeDistance, int detail) {
        // A containing section comes first; equal-distance tasks prefer smaller
        // sections. Distance is in blocks, without DH's coarse-detail discount.
        return Math.min(Math.max(signedEdgeDistance, 0), (Integer.MAX_VALUE - 63) / 64) * 64 + Math.min(detail, 63);
    }

    private static synchronized Access access() throws ReflectiveOperationException {
        if (access == null) access = new Access();
        return access;
    }

    private static final class Access {
        final Method distance, detail, centerX, centerZ;
        final java.lang.reflect.Field targetX, targetZ, sectionPos;
        final Constructor<?> pair;
        Access() throws ReflectiveOperationException {
            var pos = Class.forName("com.seibel.distanthorizons.core.pos.DhSectionPos");
            distance = pos.getMethod("getChebyshevSignedBlockDistance", long.class,
                    Class.forName("com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D"));
            detail = pos.getMethod("getDetailLevel", long.class);
            centerX = pos.getMethod("getCenterBlockPosX", long.class);
            centerZ = pos.getMethod("getCenterBlockPosZ", long.class);
            var target = Class.forName("com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D");
            targetX = target.getField("x");
            targetZ = target.getField("z");
            sectionPos = Class.forName("com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection").getField("pos");
            pair = Class.forName("com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue$TaskDistancePair")
                    .getDeclaredConstructor(Class.forName("com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalTask"), int.class);
            pair.setAccessible(true);
        }
    }
}

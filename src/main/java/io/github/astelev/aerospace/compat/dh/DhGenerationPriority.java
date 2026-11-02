package io.github.astelev.aerospace.compat.dh;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/** Re-rank waiting tasks only; native limits and in-progress jobs remain intact. */
public final class DhGenerationPriority {
    private static Access access;
    private DhGenerationPriority() {}

    public static Object rank(Object original, Object target, Map.Entry<?, ?> entry) {
        if (!DhFlightState.bypassMotionPause() || !DhFlightConfig.NEARBY_PRIORITY.getAsBoolean()) return original;
        try {
            var a = access();
            long pos = ((Number) entry.getKey()).longValue();
            int edgeDistance = (Integer) a.distance.invoke(null, pos, target);
            int detail = (Byte) a.detail.invoke(null, pos);
            return a.pair.newInstance(entry.getValue(), score(edgeDistance, detail));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("DH 3.3.3 generation priority link failed", e);
        }
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
        final Method distance, detail;
        final Constructor<?> pair;
        Access() throws ReflectiveOperationException {
            var pos = Class.forName("com.seibel.distanthorizons.core.pos.DhSectionPos");
            distance = pos.getMethod("getChebyshevSignedBlockDistance", long.class,
                    Class.forName("com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D"));
            detail = pos.getMethod("getDetailLevel", long.class);
            pair = Class.forName("com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue$TaskDistancePair")
                    .getDeclaredConstructor(Class.forName("com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalTask"), int.class);
            pair.setAccessible(true);
        }
    }
}

package io.github.astelev.aerospace.flight;

/** Pure policy shared by the Sable integration and its boundary tests. */
public final class HighAltitudePolicy {
    // Sable predicts up to 20 blocks vertically and expands collision bounds by one.
    private static final int COLLISION_MARGIN = 21;

    private HighAltitudePolicy() {
    }

    public static boolean maySkipTerrainTicket(boolean enabled, boolean overworld,
                                               boolean plotChunk, double hullMinY,
                                               int maxBuildHeight, int threshold) {
        return enabled
                && overworld
                && !plotChunk
                && Double.isFinite(hullMinY)
                && hullMinY > Math.max((double) threshold, (double) maxBuildHeight + COLLISION_MARGIN);
    }
}

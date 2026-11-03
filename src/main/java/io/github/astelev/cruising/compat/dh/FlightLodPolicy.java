package io.github.astelev.cruising.compat.dh;

/** Time-based refinement independent of client frame rate and DH worker threads. */
public final class FlightLodPolicy {
    private final int coarseDetail;
    private final double fastSpeed;
    private final long quietDelay;
    private final long refineInterval;
    private boolean active;
    private long lastFastTime;

    public FlightLodPolicy(int coarseDetail, double fastSpeed, long quietDelay, long refineInterval) {
        if (coarseDetail < 0 || coarseDetail > 6 || !Double.isFinite(fastSpeed) || fastSpeed <= 0
                || quietDelay < 0 || refineInterval <= 0) {
            throw new IllegalArgumentException("Invalid flight LOD settings");
        }
        this.coarseDetail = coarseDetail;
        this.fastSpeed = fastSpeed;
        this.quietDelay = quietDelay;
        this.refineInterval = refineInterval;
    }

    public int sample(boolean inSkyFlight, double speed, long nowMillis) {
        if (!inSkyFlight) {
            active = false;
            return 0;
        }
        if (!active || !Double.isFinite(speed) || speed >= fastSpeed) {
            lastFastTime = nowMillis;
        }
        active = true;
        long quietTime = Math.max(0, nowMillis - lastFastTime);
        long refinementSteps = quietTime < quietDelay ? 0 : 1 + (quietTime - quietDelay) / refineInterval;
        return (int) Math.max(0, coarseDetail - refinementSteps);
    }
}

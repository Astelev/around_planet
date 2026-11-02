package io.github.astelev.aerospace.compat.dh;

/** Distance remains dominant nearby; farther work favors the travel corridor. */
public final class FlightPathPriority {
    private FlightPathPriority() {}
    public record Motion(double x, double z) {
        public static final Motion STILL = new Motion(0, 0);
        public boolean moving() { return x != 0 || z != 0; }
    }
    public static Motion velocity(double x, double z) {
        double speed = Math.hypot(x, z);
        return !Double.isFinite(speed) || speed < 5 || speed > 10000 ? Motion.STILL : new Motion(x / speed, z / speed);
    }
    public static int distance(int signedDistance, double dx, double dz, Motion motion) {
        int distance = Math.max(0, signedDistance);
        if (distance <= 512 || !motion.moving()) return distance;
        double length = Math.hypot(dx, dz);
        if (length < 1) return distance;
        double alignment = Math.max(-1, Math.min(1, (dx * motion.x + dz * motion.z) / length));
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(distance - 0.6 * alignment * (distance - 512)));
    }
}

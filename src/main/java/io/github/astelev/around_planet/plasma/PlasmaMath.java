package io.github.astelev.around_planet.plasma;

/** Visual dynamic-pressure response; has no effect on physics or temperature. */
public final class PlasmaMath {
    public static final double THRESHOLD = 150;
    public static double intensity(double speed, double density) {
        if (!Double.isFinite(speed) || !Double.isFinite(density) || speed <= THRESHOLD || density <= 0) return 0;
        return -Math.expm1(-Math.min(1, density) * (speed * speed - THRESHOLD * THRESHOLD) / 12000);
    }
    private PlasmaMath() {}
}

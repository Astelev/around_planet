package io.github.astelev.aerospace.flight;

/** A bounded, non-persistent description of a temporary chunk view. */
public record TemporaryView(int centerX, int centerZ, int radius) {
    public TemporaryView {
        if (radius < 0 || radius > 32) {
            throw new IllegalArgumentException("View radius must be between 0 and 32");
        }
    }

    public boolean contains(int x, int z) {
        return Math.abs((long) x - centerX) <= radius && Math.abs((long) z - centerZ) <= radius;
    }
}

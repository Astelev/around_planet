package io.github.astelev.around_planet.topology;

/** Canonical half-open X interval; Y, Z, orientation and vectors are unchanged. */
public record EastWestTopology(int width) {
    public EastWestTopology {
        if (width < 256 || width > 59_000_000 || width % 32 != 0)
            throw new IllegalArgumentException("Width must be a multiple of 32 between 256 and 59000000");
    }
    public static EastWestTopology atBoundary(int boundaryX) {
        if (boundaryX < 128 || boundaryX > 29_500_000) throw new IllegalArgumentException("Invalid boundaryX");
        return new EastWestTopology(boundaryX * 2);
    }
    public double west() { return -width / 2.0; }
    public double east() { return width / 2.0; }
    public boolean outside(double x) { return x < west() || x >= east(); }
    public double canonicalX(double x) {
        if (!Double.isFinite(x)) throw new IllegalArgumentException("Non-finite X");
        return x - Math.floor((x - west()) / width) * width;
    }
    public double destinationX(double x) {
        return outside(x) ? canonicalX(x) : x < 0 ? x + width : x - width;
    }
    public double distanceToSeam(double x) { return Math.min(Math.abs(x-west()), Math.abs(x-east())); }
}

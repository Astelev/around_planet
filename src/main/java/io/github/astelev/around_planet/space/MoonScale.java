package io.github.astelev.around_planet.space;

/** One chunk-aligned lunar circumference for the chart, terrain, borders and physical radius. */
public record MoonScale(int circumference) {
  public static final double EARTH_RATIO = .2724;

  public MoonScale {
    if (circumference < 512 || circumference > 59_000_000 || circumference % 32 != 0)
      throw new IllegalArgumentException(
          "Lunar circumference must be a multiple of 32 in [512,59000000]");
  }

  public static MoonScale fromEarthCircumference(double earth) {
    if (!Double.isFinite(earth) || earth <= 0)
      throw new IllegalArgumentException("Earth circumference");
    // PlanetScale -> radius -> circumference can drift a few ULPs at a rounding tie.
    // Recover the integral configured circumference before applying the lunar ratio.
    double integral = Math.rint(earth);
    if (Math.abs(earth - integral) <= Math.ulp(earth) * 4) earth = integral;
    long width = Math.max(512, Math.round(earth * EARTH_RATIO / 32) * 32);
    if (width > 59_000_000) throw new IllegalArgumentException("Lunar circumference too large");
    return new MoonScale((int) width);
  }

  public static MoonScale fromEarthRadius(double radius) {
    return fromEarthCircumference(radius * 2 * Math.PI);
  }

  public double radius() {
    return circumference / (2 * Math.PI);
  }

  public int boundaryX() {
    return circumference / 2;
  }

  public int boundaryZ() {
    return circumference / 4;
  }

  public PlanetGeometry geometry() {
    return new PlanetGeometry(boundaryX(), boundaryZ());
  }
}

package io.github.astelev.around_planet.manufacturing;

/** Higher normalized values mean better resistance/capacity; temperature is degrees Celsius. */
public record MaterialStats(double wear, double temperature, double load, double corrosion) {
  public MaterialStats {
    for (double n : new double[] {wear, load, corrosion})
      if (!Double.isFinite(n) || n < 0 || n > 1)
        throw new IllegalArgumentException("Material resistance/capacity must be in [0,1]");
    if (!Double.isFinite(temperature) || temperature < 0 || temperature > 100000)
      throw new IllegalArgumentException("Working temperature must be in [0,100000] Celsius");
  }

  public boolean satisfies(MaterialStats minimum) {
    return wear >= minimum.wear
        && temperature >= minimum.temperature
        && load >= minimum.load
        && corrosion >= minimum.corrosion;
  }

  public static final MaterialStats NONE = new MaterialStats(0, 0, 0, 0);
}

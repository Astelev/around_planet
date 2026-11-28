package io.github.astelev.around_planet.space;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MoonScaleTest {
  @Test
  void stockMoonSharesOneAlignedPeriod() {
    var scale = MoonScale.fromEarthCircumference(131072);
    assertEquals(35712, scale.circumference());
    assertEquals(17856, scale.boundaryX());
    assertEquals(8928, scale.boundaryZ());
    assertEquals(scale.radius(), scale.geometry().radius(), 1e-9);
    var system = new CelestialSystem(131072 / (2 * Math.PI), 4e9, 0);
    assertEquals(scale.radius(), system.moonRadius(), 1e-9);
    assertEquals(scale.geometry(), MoonSurface.geometry(system));
  }

  @Test
  void bordersScaleWithPlanetAndWrapExactly() {
    for (int earth : new int[] {4096, 131072, 262144, 59000000}) {
      var scale = MoonScale.fromEarthCircumference(earth);
      var seam =
          new io.github.astelev.around_planet.topology.EastWestTopology(scale.circumference());
      assertEquals(0, scale.circumference() % 32);
      assertTrue(Math.abs(scale.circumference() - earth * MoonScale.EARTH_RATIO) <= 16);
      assertEquals(-scale.boundaryX() + .5, seam.canonicalX(scale.boundaryX() + .5), 1e-9);
      var chart = scale.geometry();
      assertTrue(
          chart
                  .position(-scale.boundaryX(), 100, 50)
                  .subtract(chart.position(scale.boundaryX(), 100, 50))
                  .length()
              < 1e-8);
    }
  }

  @Test
  void chartMinimumAndInvalidSizesAreHandled() {
    assertEquals(512, MoonScale.fromEarthCircumference(256).circumference());
    assertThrows(
        IllegalArgumentException.class, () -> MoonScale.fromEarthCircumference(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new MoonScale(513));
  }

  @Test
  void radiusRoundTripCannotMoveBorderAtRoundingTies() {
    for (int units = 1250; units <= 1843750; units += 2500) {
      double earth = units * 32d;
      assertEquals(
          MoonScale.fromEarthCircumference(earth),
          MoonScale.fromEarthRadius(earth / (2 * Math.PI)),
          "Earth circumference " + earth);
    }
  }
}

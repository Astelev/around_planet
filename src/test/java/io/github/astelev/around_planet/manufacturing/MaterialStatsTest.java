package io.github.astelev.around_planet.manufacturing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MaterialStatsTest {
  @Test
  void anyMaterialMeetingEveryRequirementIsAccepted() {
    var requirement = new MaterialStats(.3, 500, .4, .3);
    assertTrue(new MaterialStats(.3, 600, .4, .75).satisfies(requirement));
    assertTrue(new MaterialStats(.65, 1200, .7, .35).satisfies(requirement));
    assertFalse(new MaterialStats(.2, 300, .3, .65).satisfies(requirement));
  }

  @Test
  void noAttributeMayCompensateForAFailedAttribute() {
    var r = new MaterialStats(.5, 900, .6, .6);
    assertFalse(new MaterialStats(1, 10000, 1, .59).satisfies(r));
    assertFalse(new MaterialStats(.49, 10000, 1, 1).satisfies(r));
    assertFalse(new MaterialStats(1, 899, 1, 1).satisfies(r));
    assertFalse(new MaterialStats(1, 10000, .59, 1).satisfies(r));
    assertTrue(r.satisfies(r));
  }

  @Test
  void invalidMaterialDataFailsInsteadOfProducingInfiniteQuality() {
    for (double n : new double[] {-.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertThrows(IllegalArgumentException.class, () -> new MaterialStats(n, 500, .5, .5));
      assertThrows(IllegalArgumentException.class, () -> new MaterialStats(.5, 500, n, .5));
      assertThrows(IllegalArgumentException.class, () -> new MaterialStats(.5, 500, .5, n));
    }
    assertThrows(IllegalArgumentException.class, () -> new MaterialStats(.5, Double.NaN, .5, .5));
  }
}

package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.event.level.LevelEvent;

@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class MoonBorderGameTests {
  @GameTest(
      template = "empty",
      templateNamespace = "around_planet",
      batch = "moonBorders",
      timeoutTicks = 300)
  public static void lunarBordersBindScaleAndWrapRidersWithoutTouchingEarth(GameTestHelper h) {
    var moon = h.getLevel().getServer().getLevel(MoonSurface.dimension());
    boolean enabled = PlanetConfig.ENABLED.get();
    int size = PlanetConfig.PLANET_SIZE.get();
    var old = PlanetBorders.get(moon.getWorldBorder());
    double oldZ = PlanetBorders.boundaryZ(moon.getWorldBorder());
    var earth = PlanetBorders.get(h.getLevel().getWorldBorder());
    var scale = MoonScale.fromEarthCircumference(size);
    try {
      PlanetConfig.ENABLED.set(true);
      double vanilla = moon.getWorldBorder().getSize();
      TransferService.load(new LevelEvent.Load(moon));
      var seam = PlanetBorders.get(moon.getWorldBorder());
      h.assertTrue(
          seam != null && seam.width() == scale.circumference(),
          "Moon did not bind its own circumference");
      h.assertTrue(
          java.util.Objects.equals(earth, PlanetBorders.get(h.getLevel().getWorldBorder())),
          "Lunar load overwrote Earth's seam");
      h.assertTrue(
          moon.getWorldBorder().getSize() == vanilla,
          "Binding modified saved vanilla border settings");
      h.assertTrue(
          moon.getWorldBorder().getMinZ() == -scale.boundaryZ()
              && moon.getWorldBorder().getMaxZ() == scale.boundaryZ(),
          "Wrong lunar polar walls");
      h.assertTrue(
          !moon.getWorldBorder().isWithinBounds(new BlockPos(0, 300, scale.boundaryZ() + 1))
              && moon.getWorldBorder().isWithinBounds(new BlockPos(scale.boundaryX() + 2, 300, 0)),
          "Traversable seam became solid or polar wall is missing");
      var terrain =
          ((io.github.astelev.around_planet.moon.MoonChunkGenerator)
                  moon.getChunkSource().getGenerator())
              .terrain();
      h.assertTrue(
          terrain.circumference() == seam.width(), "Generator and seam use different periods");
      for (int x : new int[] {-scale.boundaryX() / 16, scale.boundaryX() / 16 - 1})
        for (int z = 2; z <= 3; z++) moon.getChunk(x, z);
      var boat = EntityType.BOAT.create(moon);
      var rider = EntityType.PIG.create(moon);
      boat.setPos(scale.boundaryX() + 2.5, 450, 40);
      rider.setPos(boat.position());
      rider.startRiding(boat, true);
      boat.setDeltaMovement(.4, -.1, .2);
      TransferService.check(boat);
      h.assertTrue(
          Math.abs(boat.getX() - (-scale.boundaryX() + 2.5)) < 1e-7
              && rider.getVehicle() == boat
              && Math.abs(rider.getX() - boat.getX()) < 1,
          "Lunar east crossing separated rider tree or used Earth width");
      h.assertTrue(
          boat.getDeltaMovement().x == .4 && boat.getDeltaMovement().y == -.1,
          "Seam changed velocity");
      boat.setPos(-scale.boundaryX() - 2.5, 450, 40);
      boat.positionRider(rider);
      TransferService.check(boat);
      h.assertTrue(
          Math.abs(boat.getX() - (scale.boundaryX() - 2.5)) < 1e-7 && rider.getVehicle() == boat,
          "West crossing failed");
      rider.stopRiding();
      boat.discard();
      rider.discard();
      PlanetConfig.PLANET_SIZE.set(size * 2);
      TransferService.load(new LevelEvent.Load(moon));
      h.assertTrue(
          PlanetBorders.get(moon.getWorldBorder()).width()
              == MoonScale.fromEarthCircumference(size * 2).circumference(),
          "Lunar border did not follow configured planet size");
      h.succeed();
    } finally {
      PlanetConfig.PLANET_SIZE.set(size);
      PlanetConfig.ENABLED.set(enabled);
      PlanetBorders.unbind(moon.getWorldBorder());
      if (old != null) PlanetBorders.bind(moon.getWorldBorder(), old, oldZ);
    }
  }
}

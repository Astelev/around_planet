package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.transfer.TransferService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Vector3d;

@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class MoonShipBorderGameTests {
  @GameTest(
      template = "empty",
      templateNamespace = "around_planet",
      batch = "moonShipBorders",
      timeoutTicks = 300)
  public static void lunarRigidBodyWrapsWithLinearAndAngularVelocityIntact(GameTestHelper h) {
    var moon = h.getLevel().getServer().getLevel(MoonSurface.dimension());
    boolean enabled = PlanetConfig.ENABLED.get();
    var old = PlanetBorders.get(moon.getWorldBorder());
    double oldZ = PlanetBorders.boundaryZ(moon.getWorldBorder());
    var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(moon);
    dev.ryanhcode.sable.sublevel.ServerSubLevel ship = null;
    try {
      PlanetConfig.ENABLED.set(true);
      TransferService.load(new LevelEvent.Load(moon));
      var seam = PlanetBorders.get(moon.getWorldBorder());
      var p = new BlockPos(10, 300, 10);
      moon.setBlockAndUpdate(
          p, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState());
      ship =
          dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(
              moon,
              p,
              java.util.List.of(p),
              new dev.ryanhcode.sable.companion.math.BoundingBox3i(10, 300, 10, 10, 300, 10));
      var pipe = container.physicsSystem().getPipeline();
      pipe.teleport(ship, new Vector3d(seam.east() + 3, 450, 40), ship.logicalPose().orientation());
      ship.updateBoundingBox();
      ship.updateLastPose();
      pipe.addLinearAndAngularVelocity(ship, new Vector3d(5, .2, 2), new Vector3d(.03, .04, 0));
      var v = pipe.getLinearVelocity(ship, new Vector3d());
      var w = pipe.getAngularVelocity(ship, new Vector3d());
      double start = ship.logicalPose().position().x;
      io.github.astelev.around_planet.compat.ShipTransferService.check(moon, container);
      h.assertTrue(
          Math.abs(ship.logicalPose().position().x - start + seam.width()) < 1e-6,
          "Lunar ship used wrong seam width or failed to wrap");
      h.assertTrue(
          pipe.getLinearVelocity(ship, new Vector3d()).distance(v) < 1e-8
              && pipe.getAngularVelocity(ship, new Vector3d()).distance(w) < 1e-8,
          "Lunar wrap altered physical velocity");
      pipe.teleport(ship, new Vector3d(seam.west() - 3, 450, 40), ship.logicalPose().orientation());
      ship.updateBoundingBox();
      ship.updateLastPose();
      start = ship.logicalPose().position().x;
      io.github.astelev.around_planet.compat.ShipTransferService.check(moon, container);
      h.assertTrue(
          Math.abs(ship.logicalPose().position().x - start - seam.width()) < 1e-6,
          "Lunar westward ship crossing failed");
      h.succeed();
    } finally {
      if (ship != null && !ship.isRemoved())
        container.removeSubLevel(
            ship, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
      PlanetConfig.ENABLED.set(enabled);
      PlanetBorders.unbind(moon.getWorldBorder());
      if (old != null) PlanetBorders.bind(moon.getWorldBorder(), old, oldZ);
    }
  }
}

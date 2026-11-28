package io.github.astelev.around_planet.gametest;

import static io.github.astelev.around_planet.gametest.ManufacturingGameTests.*;

import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ManufacturingShipGameTests {
  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void orbitalMachineUsesPhysicalPositionAndNativeInventory(GameTestHelper h) {
    if (!net.neoforged.fml.ModList.get().isLoaded("sable")) {
      h.succeed();
      return;
    }
    var be = machine(h, MachineKind.LATHE);
    be.items.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK));
    be.energy.restore(100000);
    var l = h.getLevel();
    var p = be.getBlockPos();
    var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);
    var body =
        dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(
            l,
            p,
            java.util.List.of(p),
            new dev.ryanhcode.sable.companion.math.BoundingBox3i(
                p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ()));
    var cell =
        new io.github.astelev.around_planet.space.SpaceCells.Cell(
            java.util.UUID.randomUUID(),
            17,
            12000,
            6144,
            0,
            30,
            2200,
            30,
            java.util.List.of(body.getUniqueId()));
    try {
      container
          .physicsSystem()
          .getPipeline()
          .teleport(body, new org.joml.Vector3d(12000, 6144, 0), body.logicalPose().orientation());
      body.updateBoundingBox();
      body.updateLastPose();
      io.github.astelev.around_planet.space.SpaceCells.get(l).add(l, cell);
      var machine =
          body.getPlot().getLoadedChunks().stream()
              .flatMap(c -> c.getChunk().getBlockEntities().values().stream())
              .filter(ManufacturingBlockEntity.class::isInstance)
              .map(ManufacturingBlockEntity.class::cast)
              .findFirst()
              .orElseThrow();
      h.assertTrue(
          machine.environment().equals("orbit"),
          "Sable plot coordinate mistaken for real machine position");
      tick(machine, 120);
      var data = machine.items.getStackInSlot(1).get(ManufacturingRegistry.PART_DATA.get());
      h.assertTrue(
          data != null
              && data.environment().equals("orbit")
              && machine.energy.getEnergyStored() == 92800,
          "Orbital bonus or production environment incorrect");
      h.assertTrue(
          l.getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), Direction.DOWN)
                  .extractItem(0, 1, false)
                  .getItem()
              instanceof PartItem,
          "Sable machine output capability missing");
      var moon =
          l.getServer().getLevel(io.github.astelev.around_planet.space.MoonSurface.dimension());
      var lunar = new ManufacturingBlockEntity(p, be.getBlockState());
      lunar.setLevel(moon);
      lunar.items.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK));
      h.assertTrue(
          lunar.environment().equals("moon")
              && lunar
                  .preview()
                  .get(ManufacturingRegistry.PART_DATA.get())
                  .environment()
                  .equals("moon"),
          "Moon provenance missing");
      h.succeed();
    } finally {
      io.github.astelev.around_planet.space.SpaceCells.get(l).remove(l, cell);
      if (!body.isRemoved())
        container.removeSubLevel(
            body, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
    }
  }
}

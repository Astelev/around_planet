package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.client.ManufacturingScreen;
import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "around_planet", value = Dist.CLIENT)
public final class ManufacturingClientSmoke {
  private static int ticks, phase, start, index;
  private static volatile Throwable failure;
  private static volatile boolean ready, checked;
  private static BlockPos target;

  @SubscribeEvent
  public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    if (mc.screen != null
        && mc.screen.getClass().getName().contains("pondus")
        && mc.screen.getClass().getSimpleName().contains("Import")) {
      for (var w : mc.screen.children())
        if (w instanceof net.minecraft.client.gui.components.Button b
            && b.getMessage().getString().equals("Skip")) {
          b.onPress();
          return;
        }
    }
    if (mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen) {
      for (var w : mc.screen.children())
        if (w instanceof net.minecraft.client.gui.components.Button b
            && b.getMessage().getString().contains("I know")) {
          b.onPress();
          return;
        }
    }
    if (++ticks > 3000)
      throw new AssertionError("Manufacturing timeout phase=" + phase + " index=" + index);
    if (failure != null) throw new AssertionError("Manufacturing client", failure);
    if (mc.player == null || mc.getSingleplayerServer() == null || ticks < 100) return;
    if (phase == 0) {
      phase = 1;
      start = ticks;
      mc.options.renderDistance().set(3);
      mc.options.hideGui = false;
      mc.getSingleplayerServer()
          .execute(
              () -> {
                try {
                  setup(mc);
                } catch (Throwable t) {
                  failure = t;
                }
              });
      return;
    }
    if (!ready) return;
    if (phase == 1 && ticks - start > 50) {
      mc.player.setYRot(180);
      mc.player.setXRot(12);
      shot(mc, "manufacturing-machines-alpha75.png");
      phase = 2;
      start = ticks;
      return;
    }
    if (phase == 2) {
      target = new BlockPos(30 + index * 2, 201, 30);
      if (!(mc.level.getBlockEntity(target) instanceof ManufacturingBlockEntity)) return;
      mc.getSingleplayerServer()
          .execute(
              () ->
                  ((ManufacturingBlockEntity)
                          mc.getSingleplayerServer().overworld().getBlockEntity(target))
                      .energy.restore(200000));
      var result =
          mc.gameMode.useItemOn(
              mc.player,
              net.minecraft.world.InteractionHand.MAIN_HAND,
              new net.minecraft.world.phys.BlockHitResult(
                  net.minecraft.world.phys.Vec3.atCenterOf(target),
                  Direction.SOUTH,
                  target,
                  false));
      if (!result.consumesAction()) throw new AssertionError("Empty hand interaction passed");
      phase = 3;
      start = ticks;
      return;
    }
    if (phase == 3 && mc.screen instanceof ManufacturingScreen screen && ticks - start > 20) {
      var menu = screen.getMenu();
      if (menu.energy() < 65536) throw new AssertionError("FE sync truncated: " + menu.energy());
      if (menu.quickMoveStack(mc.player, 2) != ItemStack.EMPTY)
        throw new AssertionError("Ghost preview extractable");
      int choice = index == 1 ? PartKind.IMPELLER.ordinal() : PartKind.LAVAL_NOZZLE.ordinal();
      mc.gameMode.handleInventoryButtonClick(menu.containerId, choice);
      phase = 4;
      start = ticks;
      return;
    }
    if (phase == 4 && mc.screen instanceof ManufacturingScreen screen && ticks - start > 25) {
      int choice = index == 1 ? PartKind.IMPELLER.ordinal() : PartKind.LAVAL_NOZZLE.ordinal();
      if (screen.getMenu().data.get(0) != choice)
        throw new AssertionError("Selection not synchronized");
      mc.getSingleplayerServer()
          .execute(
              () -> {
                var be =
                    (ManufacturingBlockEntity)
                        mc.getSingleplayerServer().overworld().getBlockEntity(target);
                be.items.setStackInSlot(1, ItemStack.EMPTY);
                be.energy.restore(200000);
              });
      shot(mc, "manufacturing-" + MachineKind.values()[index].getSerializedName() + "-alpha75.png");
      phase = 5;
      start = ticks;
      return;
    }
    if (phase == 5 && mc.screen instanceof ManufacturingScreen screen && ticks - start > 330) {
      var out = screen.getMenu().getSlot(1).getItem();
      if (!(out.getItem() instanceof PartItem partItem)
          || partItem.kind != (index == 1 ? PartKind.IMPELLER : PartKind.LAVAL_NOZZLE)
          || out.get(ManufacturingRegistry.PART_DATA.get()) == null)
        throw new AssertionError("Output or properties missing on client: " + out);
      var server = mc.getSingleplayerServer();
      checked = false;
      server.execute(
          () -> {
            try {
              var be = (ManufacturingBlockEntity) server.overworld().getBlockEntity(target);
              if (be.selected().ordinal()
                  != (index == 1 ? PartKind.IMPELLER : PartKind.LAVAL_NOZZLE).ordinal())
                throw new AssertionError("Server selection wrong");
              checked = true;
            } catch (Throwable t) {
              failure = t;
            }
          });
      phase = 6;
      start = ticks;
      return;
    }
    if (phase == 6 && checked) {
      mc.player.closeContainer();
      if (++index < 3) {
        phase = 2;
        start = ticks;
      } else {
        AroundPlanet.LOGGER.info(
            "MANUFACTURING_CLIENT_PASS threeMenus=true outputComponents=true"
                + " selectionRoundTrip=true energyAbove65535=true ghostProtected=true");
        phase = 7;
        mc.stop();
      }
    }
  }

  private static void setup(Minecraft mc) {
    var l = mc.getSingleplayerServer().overworld();
    var p = l.players().getFirst();
    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    p.setNoGravity(true);
    p.getAbilities().flying = true;
    p.onUpdateAbilities();
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
    for (int x = 28; x <= 36; x++)
      for (int z = 28; z <= 35; z++)
        l.setBlockAndUpdate(new BlockPos(x, 200, z), Blocks.SMOOTH_STONE.defaultBlockState());
    for (int i = 0; i < 3; i++) {
      var at = new BlockPos(30 + i * 2, 201, 30);
      var kind = MachineKind.values()[i];
      l.setBlockAndUpdate(
          at,
          ManufacturingRegistry.MACHINES
              .get(kind)
              .get()
              .defaultBlockState()
              .setValue(
                  net.minecraft.world.level.block.state.properties.BlockStateProperties
                      .HORIZONTAL_FACING,
                  Direction.SOUTH));
      var be = (ManufacturingBlockEntity) l.getBlockEntity(at);
      be.energy.restore(200000);
      be.items.setStackInSlot(
          0,
          kind == MachineKind.PRINTER
              ? new ItemStack(ManufacturingRegistry.DUSTS.get(1).get(), 64)
              : new ItemStack(Items.IRON_BLOCK, 8));
    }
    p.teleportTo(32, 202, 33);
    p.setYRot(180);
    p.setXRot(12);
    ready = true;
  }

  private static void shot(Minecraft mc, String name) {
    net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {});
  }
}

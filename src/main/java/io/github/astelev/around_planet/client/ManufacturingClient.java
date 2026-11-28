package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.manufacturing.*;

@net.neoforged.fml.common.EventBusSubscriber(
    modid = "around_planet",
    value = net.neoforged.api.distmarker.Dist.CLIENT,
    bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class ManufacturingClient {
  @net.neoforged.bus.api.SubscribeEvent
  public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
    e.register(ManufacturingRegistry.MENU.get(), ManufacturingScreen::new);
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
    e.register(
        (stack, index) -> {
          var data = stack.get(ManufacturingRegistry.PART_DATA.get());
          return 0xff000000 | (data == null ? 0xcccccc : data.color());
        },
        ManufacturingRegistry.PARTS.values().stream()
            .map(java.util.function.Supplier::get)
            .toArray(net.minecraft.world.item.Item[]::new));
  }
}

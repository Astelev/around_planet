package io.github.astelev.around_planet.network;

import io.github.astelev.around_planet.manufacturing.ManufacturingMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** JEI ghost drops only configure an open, accessible menu; they never give real items. */
public record ManufacturingFilterPayload(int menuId, ItemStack filter)
    implements CustomPacketPayload {
  public static final Type<ManufacturingFilterPayload> TYPE =
      new Type<>(ResourceLocation.parse("around_planet:manufacturing_filter"));
  public static final StreamCodec<RegistryFriendlyByteBuf, ManufacturingFilterPayload> CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT,
          ManufacturingFilterPayload::menuId,
          ItemStack.OPTIONAL_STREAM_CODEC,
          ManufacturingFilterPayload::filter,
          ManufacturingFilterPayload::new);

  @Override
  public Type<ManufacturingFilterPayload> type() {
    return TYPE;
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("1")
        .optional()
        .playToServer(
            TYPE,
            CODEC,
            (p, c) ->
                c.enqueueWork(
                    () -> {
                      var player = c.player();
                      if (player.containerMenu instanceof ManufacturingMenu menu
                          && menu.containerId == p.menuId) menu.setGhostFilter(player, p.filter);
                    }));
  }
}

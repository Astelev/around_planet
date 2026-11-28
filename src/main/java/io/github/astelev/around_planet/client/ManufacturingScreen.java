package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public final class ManufacturingScreen extends AbstractContainerScreen<ManufacturingMenu> {
  public ManufacturingScreen(
      ManufacturingMenu menu,
      net.minecraft.world.entity.player.Inventory inventory,
      Component title) {
    super(menu, inventory, title);
    imageWidth = 176;
    imageHeight = 203;
    inventoryLabelY = 109;
  }

  public net.minecraft.client.renderer.Rect2i filterArea() {
    return new net.minecraft.client.renderer.Rect2i(leftPos + 79, topPos + 37, 18, 18);
  }

  public void acceptGhostFilter(net.minecraft.world.item.ItemStack stack) {
    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
        new io.github.astelev.around_planet.network.ManufacturingFilterPayload(
            menu.containerId, stack.copyWithCount(1)));
  }

  @Override
  protected void renderBg(GuiGraphics g, float dt, int mx, int my) {
    g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff162334);
    g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xff6c8494);
    for (var slot : menu.slots) {
      int x = leftPos + slot.x, y = topPos + slot.y;
      g.fill(
          x - 1,
          y - 1,
          x + 17,
          y + 17,
          slot.index == ManufacturingMenu.FILTER_SLOT ? 0xff254758 : 0xff080f16);
      g.renderOutline(x - 1, y - 1, 18, 18, 0xff657582);
    }
    g.drawString(font, ">", leftPos + 80, topPos + 77, 0xff90b6ca, false);
    int width = menu.total() > 0 ? Math.clamp(menu.progress() * 106 / menu.total(), 0, 106) : 0;
    g.fill(leftPos + 35, topPos + 96, leftPos + 141, topPos + 100, 0xff080f16);
    g.fill(leftPos + 35, topPos + 96, leftPos + 35 + width, topPos + 100, 0xff65cfaa);
    g.fill(leftPos + 158, topPos + 66, leftPos + 166, topPos + 99, 0xff080f16);
    int h = menu.energy() * 33 / ManufacturingBlockEntity.CAPACITY;
    g.fill(leftPos + 158, topPos + 99 - h, leftPos + 166, topPos + 99, 0xffdfad43);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mx, int my) {
    g.drawString(font, title, 8, 7, 0xffe5edf4, false);
    g.drawString(
        font,
        Component.translatable("around_planet.manufacturing.output_filter"),
        8,
        42,
        0xffa9d9df,
        false);
    g.drawString(
        font,
        Component.translatable(
            "around_planet.manufacturing.env."
                + switch (menu.data.get(7)) {
                  case 1 -> "moon";
                  case 2 -> "orbit";
                  default -> "ground";
                }),
        8,
        17,
        0xff90b6ca,
        false);
    g.drawString(
        font,
        Component.translatable("around_planet.manufacturing.status." + menu.data.get(8)),
        8,
        63,
        0xffa9d9df,
        false);
    g.drawString(
        font,
        "FE " + menu.energy() + " / " + ManufacturingBlockEntity.CAPACITY,
        8,
        103,
        0xffe4c77a,
        false);
    g.drawString(font, playerInventoryTitle, 8, inventoryLabelY + 5, 0xffc5ced6, false);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float dt) {
    super.render(g, mx, my, dt);
    renderTooltip(g, mx, my);
    if (filterArea().contains(mx, my)
        && menu.getSlot(ManufacturingMenu.FILTER_SLOT).getItem().isEmpty())
      g.renderTooltip(
          font,
          font.split(Component.translatable("around_planet.manufacturing.filter_help"), 210),
          mx,
          my);
  }
}

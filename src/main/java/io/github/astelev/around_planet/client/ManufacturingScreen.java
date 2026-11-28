package io.github.astelev.around_planet.client;

import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public final class ManufacturingScreen extends AbstractContainerScreen<ManufacturingMenu> {
  private final java.util.Map<PartKind, Button> selectionButtons =
      new java.util.EnumMap<>(PartKind.class);

  public ManufacturingScreen(
      ManufacturingMenu menu,
      net.minecraft.world.entity.player.Inventory inventory,
      Component title) {
    super(menu, inventory, title);
    imageWidth = 176;
    imageHeight = 203;
    inventoryLabelY = 109;
  }

  @Override
  protected void init() {
    super.init();
    selectionButtons.clear();
    int n = 0;
    for (var part : PartKind.values())
      if (menu.kind.supports(part)) {
        int id = part.ordinal();
        var button =
            Button.builder(
                    Component.translatable(
                        "around_planet.manufacturing.select." + part.getSerializedName()),
                    b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                .bounds(leftPos + 5 + (n % 2) * 84, topPos + 27 + (n / 2) * 18, 82, 17)
                .build();
        button.setTooltip(
            net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("item.around_planet." + part.getSerializedName())));
        addRenderableWidget(button);
        selectionButtons.put(part, button);
        n++;
      }
  }

  @Override
  protected void renderBg(GuiGraphics g, float dt, int mx, int my) {
    g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff162334);
    g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xff6c8494);
    for (var slot : menu.slots) {
      int x = leftPos + slot.x, y = topPos + slot.y;
      g.fill(x - 1, y - 1, x + 17, y + 17, slot.index == 2 ? 0xff254758 : 0xff080f16);
      g.renderOutline(x - 1, y - 1, 18, 18, 0xff657582);
    }
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
    selectionButtons.forEach((part, button) -> button.active = part.ordinal() != menu.data.get(0));
    super.render(g, mx, my, dt);
    renderTooltip(g, mx, my);
  }
}

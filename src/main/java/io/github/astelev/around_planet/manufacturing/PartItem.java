package io.github.astelev.around_planet.manufacturing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class PartItem extends Item {
  public final PartKind kind;

  public PartItem(Properties p, PartKind kind) {
    super(p);
    this.kind = kind;
  }

  @Override
  public Component getName(ItemStack stack) {
    var data = stack.get(ManufacturingRegistry.PART_DATA.get());
    if (data == null) return super.getName(stack);
    var id = data.material();
    var name =
        Component.translatableWithFallback(
            "material." + id.getNamespace() + "." + id.getPath(), id.getPath().replace('_', ' '));
    return Component.translatable(
        "around_planet.manufacturing.part_name", super.getName(stack), name);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      Item.TooltipContext context,
      java.util.List<Component> lines,
      TooltipFlag flag) {
    var data = stack.get(ManufacturingRegistry.PART_DATA.get());
    if (data == null) {
      lines.add(
          Component.translatable("around_planet.manufacturing.unfinished")
              .withStyle(net.minecraft.ChatFormatting.RED));
      return;
    }
    var s = data.stats();
    lines.add(Component.translatable("around_planet.manufacturing.wear", s.wear()));
    lines.add(Component.translatable("around_planet.manufacturing.temperature", s.temperature()));
    lines.add(Component.translatable("around_planet.manufacturing.load", s.load()));
    lines.add(Component.translatable("around_planet.manufacturing.corrosion", s.corrosion()));
    lines.add(
        Component.translatable(
            "around_planet.manufacturing.environment",
            Component.translatable("around_planet.manufacturing.env." + data.environment())));
  }
}

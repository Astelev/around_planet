package io.github.astelev.around_planet.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Loaded only with Create. Delegates list, attribute, blacklist and data matching to Create. */
public final class ManufacturingCreateFilter {
  public static boolean isFilter(ItemStack stack) {
    return stack.getItem() instanceof com.simibubi.create.content.logistics.filter.FilterItem;
  }

  public static boolean test(Level level, ItemStack filter, ItemStack result) {
    return com.simibubi.create.content.logistics.filter.FilterItemStack.of(filter)
        .test(level, result);
  }
}

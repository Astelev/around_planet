package io.github.astelev.around_planet.gametest;

import static io.github.astelev.around_planet.gametest.ManufacturingGameTests.*;

import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ManufacturingCreateGameTests {
  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void mechanicalAssemblyUsesThresholdsAndCannotUseWorkbench(GameTestHelper h)
      throws Exception {
    if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
      h.succeed();
      return;
    }
    var holder =
        h.getLevel()
            .getRecipeManager()
            .byKey(ResourceLocation.parse("around_planet:liquid_rocket_engine"))
            .orElseThrow();
    h.assertTrue(
        holder.value()
            instanceof com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe,
        "Engine retained workbench recipe");
    var recipe =
        (com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe) holder.value();
    for (var material :
        java.util.List.of(
            iron(),
            new PartData(
                ResourceLocation.parse("around_planet:netherite"),
                new MaterialStats(.95, 2400, .95, .95),
                "orbit",
                0x62565d))) {
      var stacks = new java.util.ArrayList<ItemStack>();
      for (var ingredient : recipe.getIngredients()) {
        var custom = ingredient.getCustomIngredient();
        stacks.add(
            custom instanceof PartIngredient part
                ? ManufacturingRegistry.part(part.part(), material, 1)
                : ingredient.getItems()[0].copy());
      }
      h.assertTrue(
          recipe.matches(grid(stacks), h.getLevel()),
          "Create rejected qualifying material " + material.material());
      h.assertTrue(
          !recipe.matches(CraftingInput.of(3, 3, stacks), h.getLevel()),
          "Workbench bypassed mechanical crafting");
      int nozzle = 5;
      stacks.set(
          nozzle,
          ManufacturingRegistry.part(
              PartKind.LAVAL_NOZZLE,
              new PartData(
                  ResourceLocation.parse("around_planet:copper"),
                  new MaterialStats(.3, 600, .4, .75),
                  "ground",
                  0xc27a51),
              1));
      h.assertTrue(
          !recipe.matches(grid(stacks), h.getLevel()), "Create accepted low-temperature nozzle");
    }
    for (String id :
        java.util.List.of(
            "printer",
            "solid_rocket_nozzle",
            "rsu",
            "rsu_distributor",
            "inertia_damper",
            "orbital_sensor",
            "maneuver_planner",
            "telescope",
            "ship_loader"))
      h.assertTrue(
          h.getLevel()
                  .getRecipeManager()
                  .byKey(ResourceLocation.parse("around_planet:" + id))
                  .orElseThrow()
                  .value()
              instanceof com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe,
          "Wrong assembly type: " + id);
    h.succeed();
  }

  private static CraftingInput grid(java.util.List<ItemStack> stacks) throws Exception {
    var group = new com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems();
    var field = group.getClass().getDeclaredField("grid");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map =
        (java.util.Map<org.apache.commons.lang3.tuple.Pair<Integer, Integer>, ItemStack>)
            field.get(group);
    for (int y = 0; y < 3; y++)
      for (int x = 0; x < 3; x++)
        map.put(org.apache.commons.lang3.tuple.Pair.of(x, 2 - y), stacks.get(y * 3 + x));
    group.calcStats();
    return com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput.of(group);
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void nativeCreateListFilterControlsOutput(GameTestHelper h) {
    var be = machine(h, MachineKind.LATHE);
    var filter =
        new ItemStack(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("create:filter")));
    filter.set(
        com.simibubi.create.AllDataComponents.FILTER_ITEMS,
        net.minecraft.world.item.component.ItemContainerContents.fromItems(
            java.util.List.of(
                new ItemStack(ManufacturingRegistry.PARTS.get(PartKind.PRECISION_GEAR).get()))));
    be.setFilter(filter);
    be.items.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK));
    be.energy.restore(100000);
    tick(be, 160);
    h.assertTrue(
        be.items
            .getStackInSlot(1)
            .is(ManufacturingRegistry.PARTS.get(PartKind.PRECISION_GEAR).get()),
        "Native Create list filter ignored");
    filter.set(com.simibubi.create.AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
    be.setFilter(filter);
    be.items.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK));
    be.items.setStackInSlot(1, ItemStack.EMPTY);
    tick(be, 160);
    h.assertTrue(
        be.items.getStackInSlot(1).is(ManufacturingRegistry.PARTS.get(PartKind.LAVAL_NOZZLE).get()),
        "Native Create blacklist ignored");
    h.succeed();
  }
}

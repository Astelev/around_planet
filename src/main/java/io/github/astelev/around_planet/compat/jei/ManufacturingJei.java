package io.github.astelev.around_planet.compat.jei;

import io.github.astelev.around_planet.client.ManufacturingScreen;
import io.github.astelev.around_planet.manufacturing.*;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Discovered by JEI only; no JEI classes are referenced from the common bootstrap. */
@JeiPlugin
public final class ManufacturingJei implements IModPlugin {
  public static mezz.jei.api.runtime.IJeiRuntime runtime;

  @Override
  public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime value) {
    runtime = value;
  }

  @Override
  public void onRuntimeUnavailable() {
    runtime = null;
  }

  public static final Map<
          MachineKind, mezz.jei.api.recipe.RecipeType<RecipeHolder<MachiningRecipe>>>
      TYPES = new EnumMap<>(MachineKind.class);

  static {
    for (var kind : MachineKind.values())
      TYPES.put(
          kind,
          mezz.jei.api.recipe.RecipeType.createRecipeHolderType(
              ResourceLocation.parse("around_planet:" + kind.getSerializedName())));
  }

  @Override
  public ResourceLocation getPluginUid() {
    return ResourceLocation.parse("around_planet:manufacturing");
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registration) {
    for (var kind : MachineKind.values())
      registration.addRecipeCategories(
          new Category(
              kind,
              registration
                  .getJeiHelpers()
                  .getGuiHelper()
                  .createDrawableItemStack(
                      new ItemStack(ManufacturingRegistry.MACHINES.get(kind).get()))));
  }

  @Override
  public void registerExtraIngredients(IExtraIngredientRegistration registration) {
    var level = Minecraft.getInstance().level;
    if (level == null) return;
    var stacks = new java.util.ArrayList<ItemStack>();
    for (var holder :
        level.getRecipeManager().getAllRecipesFor(ManufacturingRegistry.RECIPE_TYPE.get())) {
      var stack = holder.value().getResultItem(level.registryAccess());
      if (stack.getItem() instanceof PartItem
          && stacks.stream().noneMatch(old -> ItemStack.isSameItemSameComponents(old, stack)))
        stacks.add(stack);
    }
    registration.addExtraItemStacks(stacks);
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    var level = Minecraft.getInstance().level;
    if (level == null) return;
    var recipes =
        level.getRecipeManager().getAllRecipesFor(ManufacturingRegistry.RECIPE_TYPE.get());
    for (var kind : MachineKind.values())
      registration.addRecipes(
          TYPES.get(kind), recipes.stream().filter(r -> r.value().machine() == kind).toList());
    io.github.astelev.around_planet.AroundPlanet.LOGGER.info(
        "MANUFACTURING_JEI recipes={}", recipes.size());
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    for (var kind : MachineKind.values())
      registration.addRecipeCatalyst(
          new ItemStack(ManufacturingRegistry.MACHINES.get(kind).get()), TYPES.get(kind));
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registration) {
    for (var item : ManufacturingRegistry.PARTS.values())
      registration.registerSubtypeInterpreter(
          item.get(),
          (mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack>)
              (stack, context) -> {
                var data = stack.get(ManufacturingRegistry.PART_DATA.get());
                return data == null ? "" : data.material().toString();
              });
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGhostIngredientHandler(
        ManufacturingScreen.class,
        new IGhostIngredientHandler<ManufacturingScreen>() {
          @Override
          public <I> List<Target<I>> getTargetsTyped(
              ManufacturingScreen screen,
              mezz.jei.api.ingredients.ITypedIngredient<I> ingredient,
              boolean start) {
            if (!(ingredient.getIngredient() instanceof ItemStack)) return List.of();
            return List.of(
                new Target<I>() {
                  public net.minecraft.client.renderer.Rect2i getArea() {
                    return screen.filterArea();
                  }

                  public void accept(I value) {
                    if (value instanceof ItemStack stack) screen.acceptGhostFilter(stack);
                  }
                });
          }

          @Override
          public void onComplete() {}
        });
  }

  private record Category(MachineKind kind, IDrawable icon)
      implements IRecipeCategory<RecipeHolder<MachiningRecipe>> {
    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<MachiningRecipe>> getRecipeType() {
      return TYPES.get(kind);
    }

    @Override
    public Component getTitle() {
      return Component.translatable("block.around_planet." + kind.getSerializedName());
    }

    @Override
    public IDrawable getIcon() {
      return icon;
    }

    @Override
    public int getWidth() {
      return 162;
    }

    @Override
    public int getHeight() {
      return 85;
    }

    @Override
    public ResourceLocation getRegistryName(RecipeHolder<MachiningRecipe> recipe) {
      return recipe.id();
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<MachiningRecipe> holder, IFocusGroup focus) {
      var recipe = holder.value();
      var input =
          Arrays.stream(recipe.inputs().getFirst().ingredient().getItems())
              .filter(recipe::accepts)
              .map(s -> s.copyWithCount(recipe.inputCount()))
              .toList();
      builder.addInputSlot(24, 9).addItemStacks(input);
      builder
          .addOutputSlot(119, 9)
          .addItemStack(
              recipe.result(recipe.environment().equals("any") ? "ground" : recipe.environment()));
    }

    @Override
    public void draw(
        RecipeHolder<MachiningRecipe> holder,
        IRecipeSlotsView slots,
        GuiGraphics graphics,
        double mx,
        double my) {
      var recipe = holder.value();
      var font = Minecraft.getInstance().font;
      graphics.drawString(font, "-->", 71, 13, 0xff888888, false);
      graphics.drawString(
          font,
          Component.translatable(
              "around_planet.manufacturing.jei_cost",
              recipe.ticks() / 20d,
              recipe.energy(),
              (long) recipe.energy() * recipe.ticks()),
          2,
          35,
          0xff555555,
          false);
      graphics.drawString(
          font,
          Component.translatable(
              "around_planet.manufacturing.jei_environment",
              Component.translatable("around_planet.manufacturing.env." + recipe.environment())),
          2,
          49,
          0xff555555,
          false);
      if (recipe.orbitalTicks() > 0)
        graphics.drawString(
            font,
            Component.translatable(
                "around_planet.manufacturing.jei_orbit",
                recipe.orbitalTicks() / 20d,
                (long) recipe.orbitalTicks() * recipe.energy()),
            2,
            63,
            0xff555555,
            false);
    }
  }
}

package io.github.astelev.around_planet.manufacturing;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Explicit processing inputs/outputs: visible to recipe browsers and editable with event.custom.
 */
public record MachiningRecipe(
    MachineKind machine,
    List<SizedIngredient> inputs,
    List<ItemStack> results,
    int ticks,
    int energy,
    String environment,
    int orbitalTicks)
    implements Recipe<SingleRecipeInput> {
  public static final MapCodec<MachiningRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      MachineKind.CODEC.fieldOf("machine").forGetter(MachiningRecipe::machine),
                      SizedIngredient.FLAT_CODEC
                          .listOf(1, 64)
                          .fieldOf("ingredients")
                          .forGetter(MachiningRecipe::inputs),
                      ItemStack.STRICT_CODEC
                          .listOf(1, 1)
                          .fieldOf("results")
                          .forGetter(MachiningRecipe::results),
                      Codec.intRange(1, 72000)
                          .fieldOf("processingTime")
                          .forGetter(MachiningRecipe::ticks),
                      Codec.intRange(1, 200000)
                          .optionalFieldOf("energy_per_tick", 60)
                          .forGetter(MachiningRecipe::energy),
                      Codec.STRING
                          .optionalFieldOf("environment", "any")
                          .forGetter(MachiningRecipe::environment),
                      Codec.intRange(0, 72000)
                          .optionalFieldOf("orbital_processing_ticks", 0)
                          .forGetter(MachiningRecipe::orbitalTicks))
                  .apply(i, MachiningRecipe::new));

  public MachiningRecipe {
    inputs = List.copyOf(inputs);
    results = results.stream().map(ItemStack::copy).toList();
    if (inputs.isEmpty()
        || inputs.stream().mapToLong(SizedIngredient::count).sum() > 64
        || results.size() != 1)
      throw new IllegalArgumentException(
          "Machining needs one input stack (at most 64 items) and one result stack");
  }

  public int inputCount() {
    return inputs.stream().mapToInt(SizedIngredient::count).sum();
  }

  public boolean accepts(ItemStack stack) {
    return inputs.stream().allMatch(i -> i.ingredient().test(stack));
  }

  public int processingTicks(String env) {
    return env.equals("orbit") && orbitalTicks > 0 ? orbitalTicks : ticks;
  }

  public ItemStack result(String env) {
    var out = results.getFirst().copy();
    var data = out.get(ManufacturingRegistry.PART_DATA.get());
    if (data != null)
      out.set(
          ManufacturingRegistry.PART_DATA.get(),
          new PartData(data.material(), data.stats(), env, data.color()));
    return out;
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    var list = NonNullList.<Ingredient>create();
    inputs.forEach(i -> list.add(i.ingredient()));
    return list;
  }

  @Override
  public boolean matches(SingleRecipeInput input, Level l) {
    return accepts(input.item()) && input.item().getCount() >= inputCount();
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider lookup) {
    return result("ground");
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider lookup) {
    return result(environment.equals("any") ? "ground" : environment);
  }

  @Override
  public boolean canCraftInDimensions(int w, int h) {
    return w * h >= 1;
  }

  @Override
  public boolean isSpecial() {
    return true;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ManufacturingRegistry.RECIPE_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return ManufacturingRegistry.RECIPE_TYPE.get();
  }
}

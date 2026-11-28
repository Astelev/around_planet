package io.github.astelev.around_planet.manufacturing;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Generic process; the selected material supplies input tags and resulting properties. */
public record MachiningRecipe(
    MachineKind machine,
    PartKind part,
    int inputCount,
    int outputCount,
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
                      PartKind.CODEC.fieldOf("part").forGetter(MachiningRecipe::part),
                      Codec.intRange(1, 64)
                          .fieldOf("input_count")
                          .forGetter(MachiningRecipe::inputCount),
                      Codec.intRange(1, 64)
                          .optionalFieldOf("output_count", 1)
                          .forGetter(MachiningRecipe::outputCount),
                      Codec.intRange(1, 72000)
                          .fieldOf("processing_ticks")
                          .forGetter(MachiningRecipe::ticks),
                      Codec.intRange(1, 200000)
                          .fieldOf("energy_per_tick")
                          .forGetter(MachiningRecipe::energy),
                      Codec.STRING
                          .optionalFieldOf("environment", "any")
                          .forGetter(MachiningRecipe::environment),
                      Codec.intRange(0, 72000)
                          .optionalFieldOf("orbital_processing_ticks", 0)
                          .forGetter(MachiningRecipe::orbitalTicks))
                  .apply(i, MachiningRecipe::new));

  public MachiningRecipe {
    if (!machine.supports(part))
      throw new IllegalArgumentException("Unsupported machine/part combination");
  }

  public int processingTicks(String env) {
    return env.equals("orbit") && orbitalTicks > 0 ? orbitalTicks : ticks;
  }

  @Override
  public boolean matches(SingleRecipeInput input, Level level) {
    return input.item().getCount() >= inputCount
        && MaterialDefinitions.find(input.item(), machine == MachineKind.PRINTER).isPresent();
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider lookup) {
    return MaterialDefinitions.find(input.item(), machine == MachineKind.PRINTER)
        .map(m -> result(m, "ground"))
        .orElse(ItemStack.EMPTY);
  }

  public ItemStack result(MaterialDefinitions.Material m, String env) {
    return ManufacturingRegistry.part(
        part, new PartData(m.id(), m.stats(), env, m.color()), outputCount);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider lookup) {
    return new ItemStack(ManufacturingRegistry.PARTS.get(part).get());
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

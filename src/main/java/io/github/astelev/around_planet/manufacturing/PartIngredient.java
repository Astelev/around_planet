package io.github.astelev.around_planet.manufacturing;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.*;

/** Strict stack predicate. Never simplified into an item ID during recipe/network caching. */
public record PartIngredient(PartKind part, MaterialStats minimum, String environment)
    implements ICustomIngredient {
  public static final Codec<MaterialStats> MINIMUM =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      Codec.doubleRange(0, 1)
                          .optionalFieldOf("wear_resistance", 0d)
                          .forGetter(MaterialStats::wear),
                      Codec.doubleRange(0, 100000)
                          .optionalFieldOf("working_temperature", 0d)
                          .forGetter(MaterialStats::temperature),
                      Codec.doubleRange(0, 1)
                          .optionalFieldOf("load_capacity", 0d)
                          .forGetter(MaterialStats::load),
                      Codec.doubleRange(0, 1)
                          .optionalFieldOf("corrosion_resistance", 0d)
                          .forGetter(MaterialStats::corrosion))
                  .apply(i, MaterialStats::new));
  public static final MapCodec<PartIngredient> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      PartKind.CODEC.fieldOf("part").forGetter(PartIngredient::part),
                      MINIMUM
                          .optionalFieldOf("minimum", MaterialStats.NONE)
                          .forGetter(PartIngredient::minimum),
                      Codec.STRING
                          .optionalFieldOf("environment", "any")
                          .forGetter(PartIngredient::environment))
                  .apply(i, PartIngredient::new));

  @Override
  public boolean test(ItemStack stack) {
    var data = stack.get(ManufacturingRegistry.PART_DATA.get());
    return stack.getItem() instanceof PartItem p
        && p.kind == part
        && data != null
        && data.stats().satisfies(minimum)
        && (environment.equals("any") || environment.equals(data.environment()));
  }

  @Override
  public java.util.stream.Stream<ItemStack> getItems() {
    var examples =
        MaterialDefinitions.examples().stream()
            .filter(d -> d.stats().satisfies(minimum))
            .map(
                d ->
                    ManufacturingRegistry.part(
                        part,
                        new PartData(
                            d.material(),
                            d.stats(),
                            environment.equals("any") ? d.environment() : environment,
                            d.color()),
                        1))
            .toList();
    if (!examples.isEmpty()) return examples.stream();
    return java.util.stream.Stream.of(
        ManufacturingRegistry.part(
            part,
            new PartData(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "around_planet", "example"),
                minimum,
                environment.equals("any") ? "ground" : environment,
                0xcccccc),
            1));
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return ManufacturingRegistry.PART_INGREDIENT.get();
  }
}

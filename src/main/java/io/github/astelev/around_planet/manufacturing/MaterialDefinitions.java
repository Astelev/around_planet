package io.github.astelev.around_planet.manufacturing;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** Datapack-defined materials, deterministic priority, no required external metals mod. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "around_planet")
public final class MaterialDefinitions {
  public record Material(
      ResourceLocation id,
      Ingredient block,
      Ingredient dust,
      MaterialStats stats,
      int color,
      int priority) {}

  private record Definition(
      Ingredient block, Ingredient dust, MaterialStats stats, int color, int priority) {
    static final Codec<Definition> CODEC =
        RecordCodecBuilder.create(
            i ->
                i.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("block").forGetter(Definition::block),
                        Ingredient.CODEC_NONEMPTY.fieldOf("dust").forGetter(Definition::dust),
                        PartData.STATS.fieldOf("stats").forGetter(Definition::stats),
                        Codec.intRange(0, 0xffffff)
                            .optionalFieldOf("color", 0xcccccc)
                            .forGetter(Definition::color),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(Definition::priority))
                    .apply(i, Definition::new));
  }

  private static volatile List<Material> materials = List.of();

  public static Optional<Material> find(ItemStack stack, boolean powder) {
    return materials.stream().filter(m -> (powder ? m.dust : m.block).test(stack)).findFirst();
  }

  public static List<Material> all() {
    return materials;
  }

  public static List<PartData> examples() {
    if (!materials.isEmpty())
      return materials.stream().map(m -> new PartData(m.id, m.stats, "ground", m.color)).toList();
    return List.of(
        example("copper", new MaterialStats(.3, 600, .4, .75), 0xc27a51),
        example("iron", new MaterialStats(.65, 1200, .7, .35), 0xc9ced2),
        example("zinc", new MaterialStats(.2, 300, .3, .65), 0x9cafa6),
        example("netherite", new MaterialStats(.95, 2400, .95, .95), 0x62565d));
  }

  private static PartData example(String id, MaterialStats stats, int color) {
    return new PartData(
        ResourceLocation.fromNamespaceAndPath("around_planet", id), stats, "ground", color);
  }

  @net.neoforged.bus.api.SubscribeEvent
  public static void reload(net.neoforged.neoforge.event.AddReloadListenerEvent e) {
    var lookup = e.getRegistryAccess();
    e.addListener(
        new net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener(
            new Gson(), "manufacturing_materials") {
          @Override
          protected void apply(
              Map<ResourceLocation, JsonElement> json,
              net.minecraft.server.packs.resources.ResourceManager r,
              net.minecraft.util.profiling.ProfilerFiller p) {
            var next = new ArrayList<Material>();
            for (var entry : json.entrySet()) {
              var parsed =
                  Definition.CODEC.parse(
                      lookup.createSerializationContext(JsonOps.INSTANCE), entry.getValue());
              var d = parsed.getOrThrow();
              next.add(new Material(entry.getKey(), d.block, d.dust, d.stats, d.color, d.priority));
            }
            next.sort(
                Comparator.comparingInt(Material::priority)
                    .reversed()
                    .thenComparing(m -> m.id.toString()));
            materials = List.copyOf(next);
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info(
                "MANUFACTURING_MATERIALS loaded {}", next.size());
          }
        });
  }

  private MaterialDefinitions() {}
}

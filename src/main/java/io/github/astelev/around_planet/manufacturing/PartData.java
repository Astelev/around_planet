package io.github.astelev.around_planet.manufacturing;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Immutable material snapshot: saved/synchronized parts retain their qualities across reloads. */
public record PartData(
    ResourceLocation material, MaterialStats stats, String environment, int color) {
  public static final Codec<MaterialStats> STATS =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      Codec.doubleRange(0, 1)
                          .fieldOf("wear_resistance")
                          .forGetter(MaterialStats::wear),
                      Codec.doubleRange(0, 100000)
                          .fieldOf("working_temperature")
                          .forGetter(MaterialStats::temperature),
                      Codec.doubleRange(0, 1)
                          .fieldOf("load_capacity")
                          .forGetter(MaterialStats::load),
                      Codec.doubleRange(0, 1)
                          .fieldOf("corrosion_resistance")
                          .forGetter(MaterialStats::corrosion))
                  .apply(i, MaterialStats::new));
  public static final Codec<PartData> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      ResourceLocation.CODEC.fieldOf("material").forGetter(PartData::material),
                      STATS.fieldOf("stats").forGetter(PartData::stats),
                      Codec.STRING
                          .optionalFieldOf("environment", "ground")
                          .forGetter(PartData::environment),
                      Codec.intRange(0, 0xffffff)
                          .optionalFieldOf("color", 0xcccccc)
                          .forGetter(PartData::color))
                  .apply(i, PartData::new));
}

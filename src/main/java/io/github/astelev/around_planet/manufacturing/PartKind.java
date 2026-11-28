package io.github.astelev.around_planet.manufacturing;

public enum PartKind implements net.minecraft.util.StringRepresentable {
  PRECISION_GEAR,
  LAVAL_NOZZLE,
  IMPELLER,
  PRECISION_HOUSING;
  public static final com.mojang.serialization.Codec<PartKind> CODEC =
      net.minecraft.util.StringRepresentable.fromEnum(PartKind::values);

  @Override
  public String getSerializedName() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}

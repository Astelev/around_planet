package io.github.astelev.around_planet.manufacturing;

public enum MachineKind implements net.minecraft.util.StringRepresentable {
  LATHE,
  MILL,
  PRINTER;
  public static final com.mojang.serialization.Codec<MachineKind> CODEC =
      net.minecraft.util.StringRepresentable.fromEnum(MachineKind::values);

  @Override
  public String getSerializedName() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }

  public boolean supports(PartKind p) {
    return this == PRINTER
        || (this == LATHE
            ? (p == PartKind.PRECISION_GEAR || p == PartKind.LAVAL_NOZZLE)
            : (p == PartKind.PRECISION_HOUSING || p == PartKind.IMPELLER));
  }

  public PartKind first() {
    return this == MILL ? PartKind.PRECISION_HOUSING : PartKind.PRECISION_GEAR;
  }
}

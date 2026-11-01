package io.github.astelev.aerospace.compat.dh;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DhFlightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue COARSE_DETAIL;
    public static final ModConfigSpec.DoubleValue FAST_SPEED;
    public static final ModConfigSpec.IntValue QUIET_SECONDS;
    public static final ModConfigSpec.IntValue REFINE_SECONDS;
    public static final ModConfigSpec.BooleanValue DIAGNOSTICS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("distantHorizonsFlight");
        ENABLED = builder.comment("Adapt DH 3.3.3 only while the server-authorized temporary sky view is active.",
                "Allows generation during fast flight without disabling DH's server-load or queue safeguards.")
                .define("enabled", true);
        COARSE_DETAIL = builder.comment("Minimum LOD cell size during cruise: 2^value blocks. 4 means 16-block cells.",
                "Existing farther, coarser LODs remain coarser.").defineInRange("coarseDetail", 4, 1, 6);
        FAST_SPEED = builder.comment("Camera speed in blocks/second that restarts coarse mode.")
                .defineInRange("fastSpeed", 20.0, 1.0, 1000000.0);
        QUIET_SECONDS = builder.comment("Quiet time before the first refinement step.")
                .defineInRange("quietSeconds", 5, 0, 300);
        REFINE_SECONDS = builder.comment("Seconds between subsequent refinement steps; each step halves the cell width.")
                .defineInRange("refineSeconds", 10, 1, 300);
        DIAGNOSTICS = builder.comment("Show the flight LOD mode and DH's generation gate in F3.",
                "Also log a short status at most once every 30 seconds during sky flight.")
                .define("diagnostics", true);
        builder.pop();
        SPEC = builder.build();
    }

    private DhFlightConfig() {}
}

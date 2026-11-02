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
    public static final ModConfigSpec.BooleanValue FALLBACK_PLANE;
    public static final ModConfigSpec.IntValue FALLBACK_HEIGHT;
    public static final ModConfigSpec.IntValue FALLBACK_COLOR;
    public static final ModConfigSpec.DoubleValue FALLBACK_BRIGHTNESS;
    public static final ModConfigSpec.BooleanValue FALLBACK_BIOMES;
    public static final ModConfigSpec.BooleanValue NEARBY_PRIORITY, FORWARD_PRIORITY, FALLBACK_HEIGHTS;

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
        FALLBACK_PLANE = builder.comment("Draw a temporary Y=60 surface in the DH terrain target, before native lighting/fog compositing.")
                .define("fallbackPlane", true);
        FALLBACK_HEIGHT = builder.defineInRange("fallbackHeight", 60, -2032, 2032);
        FALLBACK_COLOR = builder.comment("RGB color used until the budgeted biome sampler has calculated a local surface color.")
                .defineInRange("fallbackColor", 0x718a54, 0, 0xffffff);
        FALLBACK_BRIGHTNESS = builder.comment("Surface tint brightness before lighting. Lower values better match textured terrain.")
                .defineInRange("fallbackBrightness", 0.55, 0.0, 1.0);
        FALLBACK_BIOMES = builder.comment("Sample surface biomes independently of DH terrain work in a bounded 128-block grid. No chunks are generated.")
                .define("fallbackBiomeColors", true);
        NEARBY_PRIORITY = builder.comment("During sky flight, rank waiting DH generation tasks by distance to their bounds, nearest first.")
                .define("nearbyGenerationPriority", true);
        FORWARD_PRIORITY = builder.comment("Prefer LOD generation, retrieval and mesh requests along camera travel; keep the nearest 512 blocks first.")
                .define("forwardGenerationPriority", true);
        FALLBACK_HEIGHTS = builder.comment("Use approximate biome height offsets from a static table. No terrain generation or height queries.")
                .define("fallbackBiomeHeights", true);
        builder.pop();
        SPEC = builder.build();
    }

    private DhFlightConfig() {}
}

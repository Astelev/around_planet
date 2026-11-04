package io.github.astelev.around_planet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlanetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue BOUNDARY, MIN_Y, MAX_Y, HALF_SPAN_Z;
    public static final ModConfigSpec.BooleanValue DH_PORTAL_RENDER;
    public static final ModConfigSpec.IntValue DH_PREVIEW_DISTANCE;
    public static final ModConfigSpec.IntValue CHUNK_PRELOAD_DISTANCE,CHUNK_RADIUS;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("eastWest");
        ENABLED = b.comment("Create two Aeronautics global portals in the Overworld. Restart after changing settings.",
                "Only around_planet-tagged portals are managed. Generation is not made periodic. Whole-ship global traversal is a later stage.")
                .define("enabled", false);
        BOUNDARY = b.comment("Absolute X of each seam: +boundaryX wraps to -boundaryX, not to zero.",
                "Multiple of 16; total circumference is twice this value. Replaces the old widthBlocks setting.")
                .defineInRange("boundaryX", 65536, 128, 29500000);
        MIN_Y = b.comment("Bottom of the portal plane; independent of the world's build height. Must be below portalMaxY.")
                .defineInRange("portalMinY", -2048, -20000000, 20000000);
        MAX_Y = b.comment("Top of the portal plane. Default includes high-altitude flight.")
                .defineInRange("portalMaxY", 16384, -20000000, 20000000);
        HALF_SPAN_Z = b.comment("Each plane spans [-portalHalfSpanZ, +portalHalfSpanZ]. Creates no north/south portal.")
                .defineInRange("portalHalfSpanZ", 29999984, 16, 29999984);
        CHUNK_PRELOAD_DISTANCE=b.comment("Begin loading real opposite-side chunks this many blocks before the managed seam.",
                "The radius ramps up gradually across this approach distance; restart after changing.")
                .defineInRange("chunkPreloadDistanceBlocks",512,32,4096);
        CHUNK_RADIUS=b.comment("Maximum opposite-side chunk radius; also capped by the player's ordinary view distance.",
                "Larger values cost generation, server memory and network traffic.")
                .defineInRange("chunkRadius",16,2,32);
        b.pop();
        b.push("compatibility");
        DH_PORTAL_RENDER=b.comment("Prepare a second DH 3.3.3 render tree and render terrain through the managed EW seam.",
                "One extra render tree near the seam costs memory and background work. Restart after changing.")
                .define("dhPortalRendering",true);
        DH_PREVIEW_DISTANCE=b.comment("Distance in blocks from the seam at which the opposite DH tree starts loading.")
                .defineInRange("dhPreviewDistanceBlocks",512,32,4096);
        b.pop(); SPEC = b.build();
    }
    private PlanetConfig() {}
}

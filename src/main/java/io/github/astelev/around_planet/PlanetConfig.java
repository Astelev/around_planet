package io.github.astelev.around_planet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlanetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,RETAIN_MESHES;
    public static final ModConfigSpec.IntValue BOUNDARY, ACTIVATION, RADIUS;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("eastWest");
        ENABLED = b.comment("Experimental Overworld east/west transfer. Enable in a test world. Restart after changing these settings.",
                "Renders loaded chunks and ordinary entities across the seam. Physics-ship transfer is not implemented. Does not change world generation.")
                .define("enabled", false);
        BOUNDARY = b.comment("Absolute X of each seam: +boundaryX wraps to -boundaryX, not to zero.",
                "Multiple of 16; total circumference is twice this value. Replaces the old widthBlocks setting.")
                .defineInRange("boundaryX", 65536, 128, 29500000);
        ACTIVATION = b.comment("Distance from either seam at which destination preloading begins; limited to one quarter of width.")
                .defineInRange("activationDistance", 128, 16, 1024);
        RADIUS = b.comment("Destination chunk radius. Bounded transient tickets; no synchronous generation in the entity tick.")
                .defineInRange("destinationRadius", 2, 1, 4);
        RETAIN_MESHES=b.comment("Retain bounded canonical GPU meshes from seam images during a crossing.",
                "Uses the native block compiler on one worker; 64 MiB/512 sections maximum. Disable to compare the original renderer.")
                .define("retainTransitionMeshes",true);
        b.pop(); SPEC = b.build();
    }
    private PlanetConfig() {}
}

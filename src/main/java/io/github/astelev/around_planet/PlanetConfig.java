package io.github.astelev.around_planet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlanetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue WIDTH, ACTIVATION, RADIUS;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("eastWest");
        ENABLED = b.comment("Experimental Overworld east/west transfer. Enable in a test world. Restart after changing these settings.",
                "No through-seam rendering or physics-ship transfer yet. Does not change world generation.")
                .define("enabled", false);
        WIDTH = b.comment("Circumference in blocks, centered on X=0; multiple of 32. Y and Z do not wrap.")
                .defineInRange("widthBlocks", 65536, 256, 59000000);
        ACTIVATION = b.comment("Distance from either seam at which destination preloading begins; limited to one quarter of width.")
                .defineInRange("activationDistance", 128, 16, 1024);
        RADIUS = b.comment("Destination chunk radius. Bounded transient tickets; no synchronous generation in the entity tick.")
                .defineInRange("destinationRadius", 2, 1, 4);
        b.pop(); SPEC = b.build();
    }
    private PlanetConfig() {}
}

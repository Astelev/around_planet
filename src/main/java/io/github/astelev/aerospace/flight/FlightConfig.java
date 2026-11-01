package io.github.astelev.aerospace.flight;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FlightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue MIN_ALTITUDE;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("highAltitudeFlight");
        ENABLED = builder
                .comment("Skip Sable's forced terrain tickets above the world while its own loading rules keep the ship active.",
                        "Uses temporary client chunks and suspends terrain tickets for high-altitude ship passengers.")
                .define("enabled", true);
        MIN_ALTITUDE = builder
                .comment("The entire ship must be strictly above this Y. Overworld only.",
                        "The world build height plus a collision margin is also respected.")
                .defineInRange("minAltitude", 356, -2032, 2032);
        builder.pop();
        SPEC = builder.build();
    }

    private FlightConfig() {
    }
}

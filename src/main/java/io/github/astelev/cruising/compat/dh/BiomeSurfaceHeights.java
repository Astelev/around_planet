package io.github.astelev.cruising.compat.dh;

/** Artist-defined offsets above the configured sea plane, NOT measured terrain heights. */
public final class BiomeSurfaceHeights {
    private BiomeSurfaceHeights() {}
    public static int seaOffset(int seaLevel, int neutralBase) { return seaLevel - neutralBase; }
    public static int offset(String id, boolean mountain, boolean hill) {
        if (id == null) return 0;
        if (id.equals("minecraft:cherry_grove")) return 64;
        if (id.equals("minecraft:meadow")) return 56;
        if (id.equals("minecraft:savanna_plateau")) return 40;
        if (id.equals("minecraft:wooded_badlands") || id.equals("minecraft:eroded_badlands")) return 40;
        if (mountain) return 112;
        if (hill) return 32;
        if (id.contains("ocean") || id.contains("river") || id.contains("beach") || id.endsWith(":stony_shore")) return 0;
        if (id.equals("minecraft:swamp") || id.equals("minecraft:mangrove_swamp")) return 4;
        if (id.equals("minecraft:badlands")) return 24;
        if (id.equals("minecraft:old_growth_pine_taiga") || id.equals("minecraft:old_growth_spruce_taiga")) return 24;
        if (id.startsWith("minecraft:") && (id.contains("forest") || id.contains("taiga") || id.contains("jungle"))) return 16;
        if (id.startsWith("minecraft:") && (id.contains("plains") || id.endsWith(":desert") || id.endsWith(":savanna"))) return 8;
        return 0; // Unknown/modded biomes stay at the safe neutral base unless tagged.
    }
}

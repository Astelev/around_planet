package io.github.astelev.around_planet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlanetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,RETAIN_MESHES,DH_SEAM,SHIP_SEAM,SHIP_TRANSFER;
    public static final ModConfigSpec.IntValue BOUNDARY, ACTIVATION, RADIUS,SNAPSHOT_INTERVAL,SNAPSHOT_BUDGET,DH_PREVIEW,SHIP_BAND,GUARD_MARGIN;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("eastWest");
        ENABLED = b.comment("Experimental Overworld east/west transfer. Enable in a test world. Restart after changing these settings.",
                "Renders loaded chunks, ordinary entities and optional Sable ships across the seam. Optional Sable assemblies transfer by their combined center of mass. Does not change world generation.")
                .define("enabled", false);
        BOUNDARY = b.comment("Absolute X of each seam: +boundaryX wraps to -boundaryX, not to zero.",
                "Multiple of 16; total circumference is twice this value. Replaces the old widthBlocks setting.")
                .defineInRange("boundaryX", 65536, 128, 29500000);
        ACTIVATION = b.comment("Distance from either seam at which destination preloading begins; limited to one quarter of width.")
                .defineInRange("activationDistance", 128, 16, 1024);
        RADIUS = b.comment("Destination chunk radius. Bounded transient tickets; no synchronous generation in the entity tick.")
                .defineInRange("destinationRadius", 2, 1, 4);
        SNAPSHOT_INTERVAL=b.comment("Minimum ticks between checks of a visual chunk. Only changed snapshots are sent; queue budget can add latency.")
                .defineInRange("snapshotRefreshTicks",10,1,200);
        SNAPSHOT_BUDGET=b.comment("Maximum ready visual columns serialized per player per server tick. Oldest due columns take priority.")
                .defineInRange("snapshotChunksPerTick",2,1,8);
        DH_SEAM=b.comment("Render canonical Distant Horizons 3.3.3 terrain across the seam using an independent translated view.")
                .define("dhSeamRendering",true);
        DH_PREVIEW=b.comment("Approach distance for preparing the opposite DH tree. Adds one retained render tree sharing existing data.")
                .defineInRange("dhPreviewDistanceBlocks",512,32,4096);
        RETAIN_MESHES=b.comment("Retain bounded canonical GPU meshes from seam images and native shores during a crossing.",
                "Uses the native block compiler on one worker; 64 MiB/512 sections maximum. Disable to compare the original renderer.")
                .define("retainTransitionMeshes",true);
        SHIP_SEAM=b.comment("Optional Sable 2.0.6: render ships across the seam and mirror terrain collision sections. The separate shipSeamTransfers switch controls transport.")
                .define("shipSeamSupport",true);
        SHIP_BAND=b.comment("Width in chunks of the physics collision strip beyond each seam. Only sections requested by Sable are mirrored.")
                .defineInRange("shipSeamBandChunks",8,1,32);
        SHIP_TRANSFER=b.comment("Transfer a connected Sable rigid assembly when its combined center of mass crosses the seam.")
                .define("shipSeamTransfers",true);
        GUARD_MARGIN=b.comment("Safety border distance beyond each seam. Keep larger than the maximum ship overhang plus passenger reach.")
                .defineInRange("guardMarginBlocks",1024,16,16384);
        b.pop(); SPEC = b.build();
    }
    private PlanetConfig() {}
}

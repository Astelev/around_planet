package io.github.astelev.around_planet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlanetConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> PLANET_SIZE;
    public static final java.util.function.IntSupplier BOUNDARY=()->PlanetConfig.PLANET_SIZE.get()/2,BOUNDARY_Z=BOUNDARY;
    public static final ModConfigSpec.DoubleValue SPACE_GRAVITY,SPACE_UNLOAD_SPEED,ATMOSPHERE_DRAG,REENTRY_SPEED;
    public static final ModConfigSpec.BooleanValue ENABLED,RETAIN_MESHES,DH_SEAM,SHIP_SEAM,SHIP_TRANSFER,TERRAIN_BLEND,DIAGNOSTICS,PROGRESSIVE,SPACE;
    public static final ModConfigSpec.IntValue SPACE_ENTRY,SPACE_Y,ATMOSPHERE_TOP, ACTIVATION, RADIUS,SNAPSHOT_INTERVAL,SNAPSHOT_BUDGET,DH_PREVIEW,SHIP_BAND,GUARD_MARGIN,TERRAIN_BAND,MESH_MIB,MESH_SECTIONS,PROGRESSIVE_DETAIL,PROGRESSIVE_RADIUS,PROGRESSIVE_PENDING;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("planet");
        PLANET_SIZE=b.comment("Equatorial circumference in blocks. Single source for both world borders (+/- half) and cosmic radius (circumference / 2pi).",
                "Multiple of 32, from 256 to 59000000. Default preserves +/-65536 borders. Restart required.").define("circumferenceBlocks",131072,v->v instanceof Integer n && n>=256 && n<=59000000 && n%32==0);
        b.pop();b.push("eastWest");
        ENABLED = b.comment("Experimental Overworld east/west transfer. Enable in a test world. Restart after changing these settings.",
                "Renders loaded chunks, ordinary entities and optional Sable ships across the seam. Optional Sable assemblies transfer by their combined center of mass. Optional terrainSeamBlending smooths noise-based generation near the seam.")
                .define("enabled", false);
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
                "Uses the native block compiler on one worker; Configurable bounded GPU cache. Disable to compare the original renderer.")
                .define("retainTransitionMeshes",true);
        MESH_MIB=b.comment("Maximum GPU bytes for retained transition meshes, in MiB. Existing cache is trimmed to this bound.").defineInRange("transitionMeshMaxMiB",128,16,512);
        MESH_SECTIONS=b.comment("Maximum retained sections. Visible seam geometry is preferred over off-screen cached geometry.").defineInRange("transitionMeshMaxSections",1536,128,8192);
        SHIP_SEAM=b.comment("Optional Sable 2.0.6: render ships across the seam and mirror terrain collision sections. The separate shipSeamTransfers switch controls transport.")
                .define("shipSeamSupport",true);
        SHIP_BAND=b.comment("Width in chunks of the physics collision strip beyond each seam. Only sections requested by Sable are mirrored.")
                .defineInRange("shipSeamBandChunks",8,1,32);
        SHIP_TRANSFER=b.comment("Transfer a connected Sable rigid assembly when its combined center of mass crosses the seam.")
                .define("shipSeamTransfers",true);
        GUARD_MARGIN=b.comment("Safety border distance beyond each seam. Keep larger than the maximum ship overhang plus passenger reach.")
                .defineInRange("guardMarginBlocks",1024,16,16384);
        TERRAIN_BLEND=b.comment("Blend Overworld noise-based terrain and climate at both seams. Only newly generated terrain changes; restart required.")
                .define("terrainSeamBlending",true);
        TERRAIN_BAND=b.comment("Blend width in blocks on each shore, capped at one quarter of circumference. Existing saved chunks are never rewritten.")
                .defineInRange("terrainBlendWidthBlocks",256,32,2048);
        DIAGNOSTICS=b.comment("Bounded AP-DIAG summaries every 5 seconds, chunk/cache/DH counters and timings. Toggling telemetry does not change generation or render policy.").define("seamDiagnostics",true);
        b.pop();b.push("dhProgressive");
        PROGRESSIVE=b.comment("Experimental DH 3.3.3 coarse surface first, then incremental detail. Independent of seam activation; restart to change.").define("enabled",true);
        PROGRESSIVE_DETAIL=b.comment("Initial surface cell exponent: 3 means 8-block cells, refined through 4/2/1 blocks.").defineInRange("initialDataDetail",3,1,4);
        PROGRESSIVE_RADIUS=b.comment("Radius around generation target where preview stages get priority, in blocks.").defineInRange("previewRadiusBlocks",1024,128,4096);
        PROGRESSIVE_PENDING=b.comment("Bound on deferred logical requests per DH provider; shared preview stages are bounded separately.").defineInRange("maxDeferredRequests",128,16,512);
        b.pop();b.push("space");
        SPACE=b.comment("Experimental same-Overworld Sable assembly transfer to high-altitude cells. Requires eastWest.enabled and matching client. Restart required.").define("enabled",true);
        SPACE_ENTRY=b.comment("Assembly center-of-mass ascent threshold. The whole assembly must clear normal terrain.").defineInRange("entryY",3000,400,3200);
        ATMOSPHERE_TOP=b.comment("Atmosphere top above the planet radius, in blocks. Inbound virtual flight slows below it; effective minimum is entryY + 480.").defineInRange("atmosphereTop",6000,400,1000000);
        ATMOSPHERE_DRAG=b.comment("Quadratic inbound atmospheric drag per block at the native return altitude; density fades quadratically toward atmosphereTop.").defineInRange("atmosphereDrag",0.02,0.0,1.0);
        REENTRY_SPEED=b.comment("Maximum virtual center-of-mass speed in blocks/s passed to normal physics on reentry. Internal relative/angular motion is retained.").defineInRange("reentryMaxSpeed",20.0,1.0,100.0);
        SPACE_UNLOAD_SPEED=b.comment("Detach a free native group when it unloads with relative center-of-mass speed above this value, in blocks/s. Does not count orbital speed; owner and constrained groups are excluded.").defineInRange("unloadDetachSpeed",1.0,0.0,1000.0);
        SPACE_GRAVITY=b.comment("Planet surface gravity in blocks/s^2 for virtual two-body trajectories; zero selects straight inertial motion. Captured on entry.").defineInRange("surfaceGravity",9.81,0.0,50.0);
        SPACE_Y=b.comment("Local space-cell center Y. Keep ship and passengers within signed BlockPos Y range.").defineInRange("cellY",1536,1400,1700);
        b.pop(); SPEC = b.build();
    }
    private PlanetConfig() {}
}

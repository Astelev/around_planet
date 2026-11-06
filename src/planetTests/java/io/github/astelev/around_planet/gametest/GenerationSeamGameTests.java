package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.generation.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runs the seeded vanilla density graph and the real interpolated/aquifer column generator. */
@PrefixGameTestTemplate(false)
public final class GenerationSeamGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=400)
    public static void seededTerrainAndClimateMeetWithoutLoadingTheOtherShore(GameTestHelper h) {
        var level=h.getLevel();int edge=65536,z0=192000;
        var settings=level.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var noises=level.registryAccess().registryOrThrow(Registries.NOISE).asLookup();
        var raw=RandomState.create(settings.value(),noises,72931L);
        var state=RandomState.create(settings.value(),noises,72931L);
        var originalSampler=state.sampler();
        long samplerSeed=fabricSeed(originalSampler);
        if(net.neoforged.fml.ModList.get().isLoaded("fabric_biome_api_v1"))require(samplerSeed==72931L,"Fabric sampler did not retain the world seed");
        var blend=SeamBlend.of(edge,256);((SeamGenerationState)(Object)state).around_planet$configureGeneration(blend);
        require(state.sampler()==originalSampler,"Generation replaced the seeded sampler object");
        require(fabricSeed(state.sampler())==samplerSeed,"Generation lost Fabric Biome API sampler seed");
        var plains=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        var generator=new NoiseBasedChunkGenerator(new FixedBiomeSource(plains),settings);
        double rawJump=0,smoothJump=0;int worstZ=z0,worst=0;
        long began=System.nanoTime();
        for(int i=0;i<16;i++) {
            int z=z0+i*137;
            require(level.getChunkSource().getChunkNow(-edge/16,z>>4)==null,"Test shore was already loaded");
            for(int y=-48;y<=288;y+=24) {
                var a=new DensityFunction.SinglePointContext(edge,y,z);var b=new DensityFunction.SinglePointContext(-edge,y,z);
                require(state.router().finalDensity().compute(a)==state.router().finalDensity().compute(b),"Density differs at the identified seam");
                var inside=new DensityFunction.SinglePointContext(edge-512,y,z);
                require(state.router().finalDensity().compute(inside)==raw.router().finalDensity().compute(inside),"Interior density changed");
            }
            require(state.sampler().sample(edge/4,20,z/4).equals(state.sampler().sample(-edge/4,20,z/4)),"Climate differs at seam");
            int a=generator.getBaseHeight(edge-1,z,Heightmap.Types.OCEAN_FLOOR_WG,level,raw);
            int b=generator.getBaseHeight(-edge,z,Heightmap.Types.OCEAN_FLOOR_WG,level,raw);
            int sa=generator.getBaseHeight(edge-1,z,Heightmap.Types.OCEAN_FLOOR_WG,level,state);
            int sb=generator.getBaseHeight(-edge,z,Heightmap.Types.OCEAN_FLOOR_WG,level,state);
            int jump=Math.abs(a-b);rawJump+=jump;smoothJump+=Math.abs(sa-sb);
            if(jump>worst){worst=jump;worstZ=z;}
            require(level.getChunkSource().getChunkNow(-edge/16,z>>4)==null,"Column sampling loaded opposite chunks");
        }
        require(rawJump>16,"Seed did not exercise a terrain discontinuity");
        require(smoothJump<rawJump*.25,"Terrain height discontinuity was not reduced: "+rawJump+" -> "+smoothJump);
        var interior=generator.getBaseColumn(edge-512,worstZ,level,state);var original=generator.getBaseColumn(edge-512,worstZ,level,raw);
        for(int y=-64;y<320;y++)require(interior.getBlock(y)==original.getBlock(y),"Interior base column changed at Y="+y);
        var disabled=(SeamGenerationState)(Object)state;disabled.around_planet$configureGeneration(null);
        require(state.router().finalDensity().compute(new DensityFunction.SinglePointContext(edge,80,worstZ))==raw.router().finalDensity().compute(new DensityFunction.SinglePointContext(edge,80,worstZ)),"Disable did not restore original density");
        require(state.sampler()==originalSampler && state.sampler().sample(edge/4,20,worstZ/4).equals(raw.sampler().sample(edge/4,20,worstZ/4)),"Disable did not restore original climate queries");
        disabled.around_planet$configureGeneration(blend);
        try {
            var csv=new StringBuilder("offset,vanilla,blended\n");
            for(int dx=-512;dx<=512;dx+=8) {
                int x=dx<0?edge+dx:-edge+dx;
                csv.append(dx).append(',').append(generator.getBaseHeight(x,worstZ,Heightmap.Types.OCEAN_FLOOR_WG,level,raw))
                        .append(',').append(generator.getBaseHeight(x,worstZ,Heightmap.Types.OCEAN_FLOOR_WG,level,state)).append('\n');
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of("../build/generation-seam-profile.csv"),csv);
        }catch(java.io.IOException e){throw new AssertionError(e);}
        for(var dimension:java.util.List.of(Level.NETHER,Level.END)) {
            var other=level.getServer().getLevel(dimension);if(other!=null)
                require(((SeamGenerationState)(Object)other.getChunkSource().randomState()).around_planet$generationBlend()==null,"Other dimension received Overworld blend");
        }
        AroundPlanet.LOGGER.info("GENERATION_SEAM PASS: seeded terrain edge jump sum {} -> {}; continuous climate/density; unchanged interior/disable; no remote chunks loaded; {} ms",rawJump,smoothJump,(System.nanoTime()-began)/1_000_000);
        h.succeed();
    }
    private static long fabricSeed(Object sampler) {
        if(!net.neoforged.fml.ModList.get().isLoaded("fabric_biome_api_v1"))return 0;
        try{return (long)Class.forName("net.fabricmc.fabric.impl.biome.MultiNoiseSamplerHooks").getMethod("fabric_getSeed").invoke(sampler);}
        catch(ReflectiveOperationException e){throw new AssertionError("Fabric sampler lost its seed",e);}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

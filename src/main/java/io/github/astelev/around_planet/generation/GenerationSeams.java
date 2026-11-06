package io.github.astelev.around_planet.generation;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid=AroundPlanet.MOD_ID)
public final class GenerationSeams {
    public static void bind(ServerLevel level,ServerChunkCache chunks) {
        if(level.dimension()!=Level.OVERWORLD || !(chunks.getGenerator() instanceof NoiseBasedChunkGenerator)
                || !PlanetConfig.SPEC.isLoaded() || !PlanetConfig.ENABLED.getAsBoolean() || !PlanetConfig.TERRAIN_BLEND.getAsBoolean())return;
        int boundary=PlanetConfig.BOUNDARY.getAsInt();if(boundary%16!=0)return;
        var blend=SeamBlend.of(boundary,PlanetConfig.TERRAIN_BAND.getAsInt());
        var state=(SeamGenerationState)(Object)chunks.randomState();
        if(blend.equals(state.around_planet$generationBlend()))return;
        state.around_planet$configureGeneration(blend);
        AroundPlanet.LOGGER.info("Generation seam blend: X=±{}, band={} blocks per shore; new terrain only",boundary,blend.band());
    }
    @SubscribeEvent public static void load(LevelEvent.Load event){if(event.getLevel() instanceof ServerLevel level)bind(level,level.getChunkSource());}
    private GenerationSeams(){}
}

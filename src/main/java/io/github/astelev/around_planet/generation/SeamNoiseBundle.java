package io.github.astelev.around_planet.generation;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
/** One atomically published, immutable pair for terrain generation and biome queries. */
public record SeamNoiseBundle(SeamBlend blend,NoiseRouter router,Climate.Sampler sampler) {}

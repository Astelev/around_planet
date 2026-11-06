package io.github.astelev.around_planet.generation;
import net.minecraft.world.level.levelgen.NoiseRouter;
/** Immutable terrain router; climate queries stay attached to the original sampler object. */
public record SeamNoiseBundle(SeamBlend blend,NoiseRouter router) {}

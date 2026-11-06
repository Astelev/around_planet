package io.github.astelev.around_planet.generation;

import net.minecraft.world.level.levelgen.DensityFunction;
import java.util.Map;

/** Route queries on the original sampler, retaining seed and extensions installed by other mods. */
public interface SeamClimateState {
    void around_planet$generationRouting(Map<DensityFunction,DensityFunction> functions);
}

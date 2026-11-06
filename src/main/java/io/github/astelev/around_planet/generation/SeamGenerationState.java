package io.github.astelev.around_planet.generation;

/** Per-RandomState attachment; never shares a topology with another dimension/server. */
public interface SeamGenerationState {
    void around_planet$configureGeneration(SeamBlend blend);
    SeamBlend around_planet$generationBlend();
}

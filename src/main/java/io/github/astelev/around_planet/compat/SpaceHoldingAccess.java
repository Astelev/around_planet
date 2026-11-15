package io.github.astelev.around_planet.compat;
/** Remove a held dependency chain from its owning chunk before creating live native bodies. */
public interface SpaceHoldingAccess {
    boolean around_planet$discard(java.util.UUID ship,net.minecraft.world.level.ChunkPos fallback);
    boolean around_planet$queued(java.util.UUID ship);
    boolean around_planet$wake(java.util.UUID ship,net.minecraft.world.level.ChunkPos fallback);
}

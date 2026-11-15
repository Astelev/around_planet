package io.github.astelev.around_planet.compat;
/** Drop one serialized body without waking its dependency chain or touching neighbouring bodies. */
public interface SpaceHeldChunkAccess {
    dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel around_planet$discard(java.util.UUID id);
}

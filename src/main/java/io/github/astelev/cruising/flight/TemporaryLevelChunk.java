package io.github.astelev.cruising.flight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.EmptyLevelChunk;

/**
 * The type itself marks temporary air. Never inserted into server ChunkHolders,
 * the client chunk storage, lighting, or the save/generation pipelines.
 */
public final class TemporaryLevelChunk extends EmptyLevelChunk {
    public TemporaryLevelChunk(Level level, ChunkPos pos) {
        super(level, pos, level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
    }

    @Override
    public boolean isUnsaved() {
        return false;
    }

    @Override
    public void setUnsaved(boolean unsaved) {
        // This object can never become a persisted empty column.
    }
}

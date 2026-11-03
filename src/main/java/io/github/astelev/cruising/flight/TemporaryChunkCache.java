package io.github.astelev.cruising.flight;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/** A small disposable cache, independent of Minecraft's actual chunk storage. */
public final class TemporaryChunkCache {
    private final Level level;
    private final Map<Long, TemporaryLevelChunk> chunks = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, TemporaryLevelChunk> eldest) {
            return size() > 64;
        }
    };

    public TemporaryChunkCache(Level level) {
        this.level = level;
    }

    public synchronized TemporaryLevelChunk get(int x, int z) {
        return chunks.computeIfAbsent(ChunkPos.asLong(x, z), key -> new TemporaryLevelChunk(level, new ChunkPos(x, z)));
    }
}

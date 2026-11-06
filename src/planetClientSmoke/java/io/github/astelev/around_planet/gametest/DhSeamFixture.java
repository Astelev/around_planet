package io.github.astelev.around_planet.gametest;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper_neoforge;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper_neoforge;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import net.minecraft.server.level.ServerLevel;

/** Publish the edited canonical chunks through DH's normal source update path, even outside vanilla tracking. */
final class DhSeamFixture {
    static void publish(ServerLevel level) {
        var wrapper=ServerLevelWrapper_neoforge.getWrapper(level);
        for(int z=2;z<=4;z++)ServerApi.INSTANCE.serverChunkSaveEvent(new ChunkWrapper_neoforge(level.getChunk(-4,z),wrapper),wrapper);
    }
    private DhSeamFixture() {}
}

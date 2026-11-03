package io.github.astelev.cruising.gametest;

import io.github.astelev.cruising.flight.TemporaryChunkCache;
import io.github.astelev.cruising.network.TemporaryViewPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("cruising")
@PrefixGameTestTemplate(false)
public final class TemporaryChunkGameTests {
    @GameTest(template = "empty")
    public static void temporaryAirNeverEntersWorldStorage(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new ChunkPos(900000, 900000);
        helper.assertTrue(level.getChunkSource().getChunkNow(pos.x, pos.z) == null, "Probe column must start unloaded");
        var cache = new TemporaryChunkCache(level);
        var chunk = cache.get(pos.x, pos.z);
        chunk.setUnsaved(true);
        helper.assertTrue(!chunk.isUnsaved(), "Temporary air must never be marked for saving");
        helper.assertTrue(chunk.getPos().equals(pos), "Temporary chunk must have its requested coordinates");
        helper.assertTrue(chunk.setBlockState(new BlockPos(pos.getMinBlockX(), 64, pos.getMinBlockZ()),
                Blocks.STONE.defaultBlockState(), false) == null, "Temporary air must reject writes");
        helper.assertTrue(level.getChunkSource().getChunkNow(pos.x, pos.z) == null, "Temporary air must not be registered as real terrain");
        helper.assertTrue(cache.get(pos.x, pos.z) == chunk, "Repeated reads should reuse the bounded cache");
        for (int i = 1; i <= 64; i++) {
            cache.get(pos.x + i, pos.z);
        }
        helper.assertTrue(cache.get(pos.x, pos.z) != chunk, "Long flights must evict old temporary objects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void temporaryViewSurvivesNetworkRoundTrip(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var original = new TemporaryViewPayload(helper.getLevel().dimension().location(), true, -100, 200, 12);
            TemporaryViewPayload.CODEC.encode(buffer, original);
            helper.assertTrue(original.equals(TemporaryViewPayload.CODEC.decode(buffer)), "View payload must preserve dimension and coordinates");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}

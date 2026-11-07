package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class SeamServerChunkDiagnosticsMixin {
    @Shadow @Final private LongSet pendingChunks;
    @Shadow private float desiredChunksPerTick;
    @Shadow private int unacknowledgedBatches;
    @Shadow private int maxUnacknowledgedBatches;
    @Unique private long around_planet$nextReport;
    @Inject(method="sendNextChunks",at=@At("HEAD"))
    private void around_planet$queue(ServerPlayer player,CallbackInfo ci) {
        if(!SeamDiagnostics.enabled())return;
        if(!pendingChunks.isEmpty() && unacknowledgedBatches>=maxUnacknowledgedBatches)SeamDiagnostics.count("server","native.blocked_on_ack");
        long now=System.nanoTime();if(now<around_planet$nextReport)return;around_planet$nextReport=now+5_000_000_000L;
        SeamDiagnostics.event("server","native.send_queue",()->"player="+player.getId()+" pending="+pendingChunks.size()+" unacked="+unacknowledgedBatches+" maxUnacked="+maxUnacknowledgedBatches+" desiredChunksPerTick="+desiredChunksPerTick);
    }
    @Inject(method="sendChunk",at=@At("RETURN"))
    private static void around_planet$sent(ServerGamePacketListenerImpl listener,ServerLevel level,LevelChunk chunk,CallbackInfo ci) {
        SeamDiagnostics.count("server","native.chunk_sent");
    }
    @Inject(method="dropChunk",at=@At("HEAD"))
    private void around_planet$drop(ServerPlayer player,ChunkPos pos,CallbackInfo ci) {
        SeamDiagnostics.count("server",pendingChunks.contains(pos.toLong())?"native.drop_pending":"native.forget_sent");
    }
    @Inject(method="onChunkBatchReceivedByClient",at=@At("HEAD"))
    private void around_planet$ack(float desired,CallbackInfo ci){SeamDiagnostics.count("server","native.batch_ack");}
}

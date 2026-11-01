package io.github.astelev.aerospace.mixin;

import io.github.astelev.aerospace.client.ClientFlightView;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Shadow @Final private ClientLevel level;

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"), cancellable = true)
    private void aerospace$temporaryClientChunk(int x, int z, ChunkStatus status, boolean create,
                                               CallbackInfoReturnable<LevelChunk> cir) {
        var real = cir.getReturnValue();
        if (real == null || real instanceof EmptyLevelChunk) {
            var temporary = ClientFlightView.get(level, x, z);
            if (temporary != null) {
                cir.setReturnValue(temporary);
            }
        }
    }
}

package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class SeamChunkSendingMixin {
    @Shadow @Final ServerLevel level;
    @Inject(method="markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)V",at=@At("HEAD"),cancellable=true)
    private void around_planet$skipOutside(ServerPlayer player,ChunkPos pos,CallbackInfo ci) {
        if(SeamGeometry.clipped(level,pos.x,pos.z)) {io.github.astelev.around_planet.diagnostics.SeamDiagnostics.count("server","send.filtered_outside");ci.cancel();}
    }
    @Inject(method="markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V",at=@At("HEAD"),cancellable=true)
    private static void around_planet$skipOutsideLoaded(ServerPlayer player,LevelChunk chunk,CallbackInfo ci) {
        if(SeamGeometry.clipped(player.level(),chunk.getPos().x,chunk.getPos().z)) {io.github.astelev.around_planet.diagnostics.SeamDiagnostics.count("server","send.filtered_outside");ci.cancel();}
    }
}

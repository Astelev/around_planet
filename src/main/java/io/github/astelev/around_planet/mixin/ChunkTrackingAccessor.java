package io.github.astelev.around_planet.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkTrackingAccessor {
    @Invoker("updateChunkTracking") void around_planet$updateChunkTracking(ServerPlayer player);
}

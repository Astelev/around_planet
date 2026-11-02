package io.github.astelev.aerospace.mixin;

import io.github.astelev.aerospace.client.SurfaceTerrainMask;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelUnloadMixin {
    @Inject(method = "unload", at = @At("HEAD"))
    private void aerospace$forgetSurfaceColumn(LevelChunk chunk, CallbackInfo ci) {
        SurfaceTerrainMask.unloadColumn((ClientLevel)(Object)this,chunk.getPos());
    }
}

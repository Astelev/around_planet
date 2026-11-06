package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderRegionCache.class)
public abstract class SeamRenderRegionMixin {
    @WrapOperation(method="lambda$getChunkInfo$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private static LevelChunk around_planet$visualOnly(Level level,int x,int z,Operation<LevelChunk> original) {
        return SeamGeometry.clipped(level,x,z) && level instanceof ClientLevel client
                ? SeamImages.forRender(client,x,z) : original.call(level,x,z);
    }
}

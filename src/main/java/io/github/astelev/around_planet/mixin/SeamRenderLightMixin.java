package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RenderChunkRegion.class)
public abstract class SeamRenderLightMixin {
    @Shadow @Final private Level level;
    @Inject(method="getLightEngine",at=@At("HEAD"),cancellable=true)
    private void around_planet$isolatedLight(CallbackInfoReturnable<LevelLightEngine> cir) {
        if(level instanceof ClientLevel client && PlanetBorders.get(level.getWorldBorder())!=null)
            cir.setReturnValue(SeamImages.renderLight(client));
    }
}

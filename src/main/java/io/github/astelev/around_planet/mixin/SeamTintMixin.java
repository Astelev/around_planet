package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.seam.SeamGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
public abstract class SeamTintMixin {
    @Shadow @Final protected Level level;
    @Inject(method="getBlockTint",at=@At("HEAD"),cancellable=true)
    private void around_planet$tint(BlockPos pos,ColorResolver resolver,CallbackInfoReturnable<Integer> cir) {
        if(SeamGeometry.clipped(level,pos.getX()>>4,pos.getZ()>>4) && level instanceof ClientLevel client)
            cir.setReturnValue(SeamImages.tint(client,pos,resolver));
    }
}

package io.github.astelev.aerospace.mixin.dh;

import io.github.astelev.aerospace.compat.dh.DhFallbackBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.common.wrappers.worldGeneration.DhRoughSurfaceGenerator_neoforge", remap = false)
public abstract class FallbackBiomeMixin {
    @Inject(method = "getBiomeAtBlockPos", at = @At("RETURN"))
    private static void aerospace$rememberExistingBiome(@Coerce Object params, @Coerce Object level,
                                                       int x, int y, int z, CallbackInfoReturnable<Object> cir) {
        DhFallbackBridge.observeBiome(level, cir.getReturnValue(), x, z);
    }
}

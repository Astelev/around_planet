package io.github.astelev.cruising.mixin.dh;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.astelev.cruising.compat.dh.DhFlightState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil", remap = false)
public abstract class ThreadPoolUtilMixin {
    @Shadow
    private static boolean chunkBuilderOverloaded() {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "cameraMovingFast", at = @At("RETURN"))
    private static boolean cruising$allowCoarseFlightGeneration(boolean original) {
        return original && !DhFlightState.bypassMotionPause();
    }

    @ModifyReturnValue(method = "worldGenThreadsCanRun", at = @At("RETURN"))
    private static boolean cruising$observeGenerationGate(boolean original) {
        if (DhFlightState.bypassMotionPause()) {
            DhFlightState.recordGate(original, !original && chunkBuilderOverloaded());
        }
        return original;
    }
}

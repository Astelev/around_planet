package io.github.astelev.cruising.mixin.dh;

import io.github.astelev.cruising.compat.dh.DhGenerationPriority;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue", remap = false)
public abstract class WorldGenerationPriorityMixin {
    @Inject(method = "lambda$tryStartNextWorldGenTask$0", at = @At("RETURN"), cancellable = true)
    private static void cruising$nearestSurfaceFirst(@Coerce Object target, Map.Entry<?, ?> entry,
                                                      CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(DhGenerationPriority.rank(cir.getReturnValue(), target, entry));
    }
}

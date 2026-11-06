package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.compat.DhSeamBounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue",remap=false)
public abstract class SeamDhGenerationMixin {
    @Inject(method="submitRetrievalTask",at=@At("HEAD"),cancellable=true)
    private void around_planet$noOutsideGeneration(long pos,byte detail,CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if(DhSeamBounds.generationOutside(this,pos)) cir.setReturnValue(CompletableFuture.failedFuture(new CancellationException("Outside planet seam")));
    }
}

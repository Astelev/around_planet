package io.github.astelev.aerospace.mixin.dh;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.astelev.aerospace.compat.dh.DhFlightState;
import io.github.astelev.aerospace.compat.dh.DhGenerationPriority;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree", remap = false)
public abstract class LodQuadTreeMixin {
    @ModifyReturnValue(method = "calcDetailLevelFromDistance(DB)B", at = @At("RETURN"))
    private byte aerospace$coarseFlightDetail(byte original) {
        return DhFlightState.applyDetailFloor(original);
    }

    @Inject(method = {"loadQueuedSections", "startQueuedRetrievalTasks"}, at = @At("HEAD"))
    private void aerospace$snapshotTravel(CallbackInfo ci) {
        DhGenerationPriority.beginSort();
    }

    @Inject(method = {"loadQueuedSections", "startQueuedRetrievalTasks"}, at = @At("RETURN"))
    private void aerospace$finishTravelSort(CallbackInfo ci) {
        DhGenerationPriority.endSort();
    }

    @Inject(method = "lambda$startQueuedRetrievalTasks$0", at = @At("RETURN"), cancellable = true)
    private static void aerospace$forwardData(@Coerce Object target, Long a, Long b,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(DhGenerationPriority.compare(cir.getReturnValue(), target, a, b));
    }

    @Inject(method = "lambda$loadQueuedSections$0", at = @At("RETURN"), cancellable = true)
    private static void aerospace$forwardMeshes(@Coerce Object target,
            @Coerce Object a, @Coerce Object b,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(DhGenerationPriority.compareSections(cir.getReturnValue(), target, a, b));
    }
}

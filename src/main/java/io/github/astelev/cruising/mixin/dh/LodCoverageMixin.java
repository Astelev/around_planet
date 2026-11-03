package io.github.astelev.cruising.mixin.dh;

import io.github.astelev.cruising.compat.dh.DhLodCoverage;
import io.github.astelev.cruising.compat.dh.DhLodCoverageBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection",remap=false)
public abstract class LodCoverageMixin {
    @Unique private DhLodCoverage.Source cruising$coverage;
    @Inject(method="getAndBuildRenderData",at=@At("HEAD"))
    private void cruising$beginCoverage(CallbackInfoReturnable<Object> cir) { cruising$coverage=null; }
    @Inject(method="getRenderSourceForPos",at=@At("RETURN"))
    private void cruising$captureColumns(long pos, @Coerce Object direction, CallbackInfoReturnable<Object> cir) {
        if(direction==null) cruising$coverage=DhLodCoverageBridge.capture(cir.getReturnValue());
    }
    @Inject(method="uploadToGpuAsync",at=@At("RETURN"))
    private void cruising$attachCoverage(CompletableFuture<Void> parent, ArrayList<ByteBuffer> opaque,
            ArrayList<ByteBuffer> transparent, CallbackInfoReturnable<CompletableFuture<Object>> cir) {
        var snapshot=cruising$coverage;
        cir.getReturnValue().thenAccept(buffer -> DhLodCoverage.attach(buffer,snapshot));
    }
}

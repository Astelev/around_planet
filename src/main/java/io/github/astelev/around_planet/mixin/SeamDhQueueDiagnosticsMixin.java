package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import org.spongepowered.asm.mixin.*;
import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue",remap=false)
public abstract class SeamDhQueueDiagnosticsMixin implements io.github.astelev.around_planet.diagnostics.SeamQueueProbe {
    @Shadow @Final private com.seibel.distanthorizons.core.level.IDhServerLevel level;
    @Shadow public abstract int getWaitingTaskCount();
    @Shadow public abstract int getInProgressTaskCount();
    @Shadow public abstract int getQueuedChunkCount();
    @Shadow public abstract long getRetrievalEstimatedRemainingChunkCount();
    @org.spongepowered.asm.mixin.injection.Inject(method="<init>",at=@org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void around_planet$register(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){SeamDiagnostics.registerQueue(this);}
    @Override public String around_planet$queueDiagnostic(){return "id="+SeamDiagnostics.id(this)+" level="+SeamDiagnostics.id(level)+" waiting="+getWaitingTaskCount()+" inProgress="+getInProgressTaskCount()+" queuedChunks="+getQueuedChunkCount()+" remaining="+getRetrievalEstimatedRemainingChunkCount()+" progressive[pending/stages]="+(io.github.astelev.around_planet.progressive.ProgressiveOwnership.cruisingOwnsGeneration()?io.github.astelev.around_planet.progressive.ProgressiveOwnership.diagnostic(level.getFullDataProvider()):level.getFullDataProvider() instanceof io.github.astelev.around_planet.progressive.ProgressiveProviderAccess p?p.around_planet$progressiveDiagnostic():"unavailable");}
    @WrapMethod(method="submitRetrievalTask")
    private CompletableFuture<?> around_planet$request(long pos,byte detail,Operation<CompletableFuture<?>> original) {
        if(!SeamDiagnostics.enabled())return original.call(pos,detail);
        SeamDiagnostics.count("server","dh.retrieval_submit");long started=SeamDiagnostics.start();
        var future=original.call(pos,detail);
        if(future==null){SeamDiagnostics.count("server","dh.retrieval_null");return null;}
        future.whenComplete((result,error)->{
            Throwable cause=error;while(cause instanceof java.util.concurrent.CompletionException && cause.getCause()!=null)cause=cause.getCause();
            final Throwable failure=cause;
            SeamDiagnostics.count("server",failure==null?"dh.retrieval_success":failure instanceof java.util.concurrent.CancellationException?"dh.retrieval_cancelled":"dh.retrieval_failed");
            SeamDiagnostics.time("server","dh.retrieval_wall",started);
            if(failure!=null && !(failure instanceof java.util.concurrent.CancellationException))SeamDiagnostics.event("server","dh.retrieval_error",()->"pos="+pos+" detail="+detail+" error="+failure.getClass().getSimpleName()+" cause="+(failure.getCause()==null?"none":failure.getCause().getClass().getSimpleName()));
        });
        return future;
    }
}

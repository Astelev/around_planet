package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider;
import com.seibel.distanthorizons.core.generation.queues.IFullDataSourceRetrievalQueue;
import com.seibel.distanthorizons.core.generation.tasks.*;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import io.github.astelev.around_planet.progressive.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider",remap=false)
public abstract class ProgressiveProviderMixin implements ProgressiveProviderAccess {
    @Override public String around_planet$progressiveDiagnostic(){var s=around_planet$progressive;return s==null?"off":s.pending()+"/"+s.stages();}
    @Shadow @Final public AtomicReference<IFullDataSourceRetrievalQueue> worldGenQueueRef;
    @Unique private volatile ProgressiveRequests<DataSourceRetrievalResult> around_planet$progressive;
    @Unique private boolean around_planet$enabled(){return PlanetConfig.SPEC.isLoaded() && PlanetConfig.PROGRESSIVE.getAsBoolean()
            && worldGenQueueRef.get() instanceof ProgressiveQueueAccess queue && queue.around_planet$previewSupported();}
    @WrapMethod(method="queuePositionForRetrieval")
    private CompletableFuture<DataSourceRetrievalResult> around_planet$stages(Long pos,Operation<CompletableFuture<DataSourceRetrievalResult>> original) {
        int detail=DhSectionPos.getDetailLevel(pos);
        if(!PlanetConfig.SPEC.isLoaded() || !PlanetConfig.PROGRESSIVE.getAsBoolean() || detail<6
                || detail>=6+PlanetConfig.PROGRESSIVE_DETAIL.getAsInt()
                || !(worldGenQueueRef.get() instanceof ProgressiveQueueAccess queue)
                || !queue.around_planet$previewSupported()
                || !queue.around_planet$previewNear(pos,PlanetConfig.PROGRESSIVE_RADIUS.getAsInt()))return original.call(pos);
        var path=new ArrayList<Long>();int largest=6+PlanetConfig.PROGRESSIVE_DETAIL.getAsInt();
        for(int d=largest;d>detail;d--) {
            long parent=DhSectionPos.convertToDetailLevel(pos,(byte)d);
            if(queue.around_planet$previewAllowed(parent))path.add(parent);
        }
        if(path.isEmpty())return original.call(pos);
        var state=around_planet$progressive;
        if(state==null)synchronized(this) {
            state=around_planet$progressive;
            if(state==null)around_planet$progressive=state=io.github.astelev.around_planet.progressive.DhProgressiveBackend.create(
                    (GeneratedFullDataSourceProvider)(Object)this,stage->original.call(stage),PlanetConfig.PROGRESSIVE_PENDING.getAsInt());
        }
        SeamDiagnostics.count("server","progressive.logical_request");
        var result=state.request(pos,path);
        // Capacity is observable and admission is bounded; native caller retries rather than bypassing preview.
        return result==null?CompletableFuture.failedFuture(new java.util.concurrent.CancellationException("Progressive backlog capacity")):result;
    }
    @Inject(method="onWorldGenTaskComplete",at=@At("RETURN"))
    private void around_planet$published(Long pos,DataSourceRetrievalResult result,Throwable error,CallbackInfo ci) {
        var state=around_planet$progressive;if(state==null)return;
        if(error==null && result!=null && result.state==ERetrievalResultState.REQUIRES_SPLITTING)SeamDiagnostics.count("server","progressive.stage_split");
        boolean success=error==null && result!=null && result.state==ERetrievalResultState.SUCCESS;
        state.published(pos,success?null:error!=null?error:new IllegalStateException("Preview was not published"));
        if(success)SeamDiagnostics.count("server","progressive.published."+(DhSectionPos.getDetailLevel(pos)-6));
    }
    @Inject(method={"canQueueRetrievalNow()Z","canQueueRetrievalNow(Z)Z"},at=@At("RETURN"),cancellable=true)
    private void around_planet$admission(CallbackInfoReturnable<Boolean> cir){var s=around_planet$progressive;if(s!=null && around_planet$enabled() && !s.canAccept())cir.setReturnValue(false);}
    @Inject(method="removeRetrievalRequestIf",at=@At("HEAD"))
    private void around_planet$cancel(DhSectionPos.IPrimitiveLongConsumer predicate,CallbackInfo ci){var s=around_planet$progressive;if(s!=null)s.cancelIf(predicate::accept);}
    @Inject(method={"close","clearRetrievalQueue"},at=@At("HEAD"))
    private void around_planet$close(CallbackInfo ci){var s=around_planet$progressive;around_planet$progressive=null;if(s!=null)s.close();}
}

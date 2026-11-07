package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.generation.DhWorldGenerator;
import com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalTask;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import io.github.astelev.around_planet.progressive.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue",remap=false)
public abstract class ProgressiveQueueMixin implements ProgressiveQueueAccess {
    @Shadow @Final private IDhApiWorldGenerator generator;
    @Shadow @Final private IDhServerLevel level;
    @Shadow private DhBlockPos2D generationTargetPos;
    @Override public boolean around_planet$previewSupported(){return !io.github.astelev.around_planet.progressive.ProgressiveOwnership.cruisingOwnsGeneration() && generator instanceof DhWorldGenerator builtin && builtin.roughGenerator!=null && Config.Common.WorldGenerator.generatorPlan.get().surfaceGenEnabled;}
    @Unique private int around_planet$width(){var mc=level.getServerLevelWrapper().getWrappedMcObject();if(mc instanceof Level world){var t=PlanetBorders.get(world.getWorldBorder());if(t!=null)return t.width();}return 0;}
    @Override public boolean around_planet$previewAllowed(long pos) {
        var mc=level.getServerLevelWrapper().getWrappedMcObject();
        if(mc instanceof Level world){var t=PlanetBorders.get(world.getWorldBorder());return t==null || io.github.astelev.around_planet.compat.DhSeamBounds.inside(t,pos);}
        return false;
    }
    @Override public boolean around_planet$previewNear(long pos,int radius) {
        var p=generationTargetPos;return p==null || ProgressivePriority.sectionDistance(DhSectionPos.getCenterBlockPosX(pos),DhSectionPos.getCenterBlockPosZ(pos),p.x,p.z,around_planet$width(),DhSectionPos.getBlockWidth(pos))<=radius;
    }
    @WrapOperation(method="tryStartNextWorldGenTask",at=@At(value="INVOKE",target="Ljava/util/concurrent/ConcurrentHashMap;reduceEntries(JLjava/util/function/Function;Ljava/util/function/BiFunction;)Ljava/lang/Object;"))
    private Object around_planet$coarseFirst(ConcurrentHashMap<Long,DataSourceRetrievalTask> map,long threshold,Function<Map.Entry<Long,DataSourceRetrievalTask>,Object> transform,
            BiFunction<Object,Object,Object> reduce,Operation<Object> original,@Local(argsOnly=true) DhBlockPos2D target) {
        if(!PlanetConfig.SPEC.isLoaded() || !PlanetConfig.PROGRESSIVE.getAsBoolean() || !around_planet$previewSupported())return original.call(map,threshold,transform,reduce);
        int initial=PlanetConfig.PROGRESSIVE_DETAIL.getAsInt(),radius=PlanetConfig.PROGRESSIVE_RADIUS.getAsInt(),width=around_planet$width();
        Function<Map.Entry<Long,DataSourceRetrievalTask>,Object> ranked=entry->{
            Object result=transform.apply(entry);if(result instanceof ProgressiveTaskRank pair) {
                long pos=entry.getKey();int detail=DhSectionPos.getDetailLevel(pos)-6;
                boolean near=ProgressivePriority.sectionDistance(DhSectionPos.getCenterBlockPosX(pos),DhSectionPos.getCenterBlockPosZ(pos),target.x,target.z,width,DhSectionPos.getBlockWidth(pos))<=radius;
                pair.around_planet$rank(ProgressivePriority.score(pair.around_planet$rank(),detail,initial,near));
            }return result;
        };
        SeamDiagnostics.count("server","progressive.queue_rank");return original.call(map,threshold,ranked,reduce);
    }
}

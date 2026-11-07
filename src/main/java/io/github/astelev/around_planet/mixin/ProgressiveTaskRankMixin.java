package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.progressive.ProgressiveTaskRank;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue$TaskDistancePair",remap=false)
public abstract class ProgressiveTaskRankMixin implements ProgressiveTaskRank {
    @Shadow @Final @Mutable public int dist;
    @Override public int around_planet$rank(){return dist;}
    @Override public void around_planet$rank(int value){dist=value;}
}

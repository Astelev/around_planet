package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.generation.*;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.DensityFunction;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Collections;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomState.class)
public abstract class SeamNoiseStateMixin implements SeamGenerationState {
    @Shadow @Final private NoiseRouter router;
    @Shadow @Final private Climate.Sampler sampler;
    @Unique private volatile SeamNoiseBundle around_planet$state;
    @Unique private List<DensityFunction> around_planet$climateOriginals;
    @Override public SeamBlend around_planet$generationBlend(){var state=around_planet$state;return state==null?null:state.blend();}
    @Override public void around_planet$configureGeneration(SeamBlend blend) {
        if(java.util.Objects.equals(around_planet$generationBlend(),blend))return;
        if(blend==null){((SeamClimateState)(Object)sampler).around_planet$generationRouting(null);around_planet$state=null;return;}
        var visitor=SeamDensity.visitor(blend);
        if(around_planet$climateOriginals==null)around_planet$climateOriginals=List.of(sampler.temperature(),sampler.humidity(),
                sampler.continentalness(),sampler.erosion(),sampler.depth(),sampler.weirdness());
        var functions=new IdentityHashMap<DensityFunction,DensityFunction>();
        for(var input:around_planet$climateOriginals)functions.put(input,input.mapAll(visitor));
        var state=new SeamNoiseBundle(blend,router.mapAll(visitor));
        ((SeamClimateState)(Object)sampler).around_planet$generationRouting(Collections.unmodifiableMap(functions));
        around_planet$state=state;
    }
    @Inject(method="router",at=@At("HEAD"),cancellable=true)
    private void around_planet$router(CallbackInfoReturnable<NoiseRouter> cir){var state=around_planet$state;if(state!=null)cir.setReturnValue(state.router());}
}

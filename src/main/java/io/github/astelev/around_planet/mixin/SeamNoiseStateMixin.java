package io.github.astelev.around_planet.mixin;

import io.github.astelev.around_planet.generation.*;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomState.class)
public abstract class SeamNoiseStateMixin implements SeamGenerationState {
    @Shadow @Final private NoiseRouter router;
    @Shadow @Final private Climate.Sampler sampler;
    @Unique private volatile SeamNoiseBundle around_planet$state;
    @Override public SeamBlend around_planet$generationBlend(){var state=around_planet$state;return state==null?null:state.blend();}
    @Override public void around_planet$configureGeneration(SeamBlend blend) {
        if(java.util.Objects.equals(around_planet$generationBlend(),blend))return;
        if(blend==null){around_planet$state=null;return;}
        var visitor=SeamDensity.visitor(blend);
        var climate=new Climate.Sampler(sampler.temperature().mapAll(visitor),sampler.humidity().mapAll(visitor),
                sampler.continentalness().mapAll(visitor),sampler.erosion().mapAll(visitor),sampler.depth().mapAll(visitor),
                sampler.weirdness().mapAll(visitor),sampler.spawnTarget());
        around_planet$state=new SeamNoiseBundle(blend,router.mapAll(visitor),climate);
    }
    @Inject(method="router",at=@At("HEAD"),cancellable=true)
    private void around_planet$router(CallbackInfoReturnable<NoiseRouter> cir){var state=around_planet$state;if(state!=null)cir.setReturnValue(state.router());}
    @Inject(method="sampler",at=@At("HEAD"),cancellable=true)
    private void around_planet$climate(CallbackInfoReturnable<Climate.Sampler> cir){var state=around_planet$state;if(state!=null)cir.setReturnValue(state.sampler());}
}

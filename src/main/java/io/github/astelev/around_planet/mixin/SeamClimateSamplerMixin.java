package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.generation.SeamClimateState;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

/** Never replace the sampler or mutate its record fields; Fabric Biome API owns extra seed state on it. */
@Mixin(Climate.Sampler.class)
public abstract class SeamClimateSamplerMixin implements SeamClimateState {
    @Shadow @Final private DensityFunction temperature;
    @Shadow @Final private DensityFunction humidity;
    @Shadow @Final private DensityFunction continentalness;
    @Shadow @Final private DensityFunction erosion;
    @Shadow @Final private DensityFunction depth;
    @Shadow @Final private DensityFunction weirdness;
    @Unique private volatile Map<DensityFunction,DensityFunction> around_planet$routing;
    @Override public void around_planet$generationRouting(Map<DensityFunction,DensityFunction> functions){around_planet$routing=functions;}
    @Unique private DensityFunction around_planet$effective(DensityFunction input) {
        var routing=around_planet$routing;return routing==null?input:routing.getOrDefault(input,input);
    }
    @WrapOperation(method="sample",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double around_planet$sample(DensityFunction input,DensityFunction.FunctionContext context,Operation<Double> original) {
        return original.call(around_planet$effective(input),context);
    }
    @Inject(method="temperature",at=@At("HEAD"),cancellable=true)
    private void around_planet$temperature(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(temperature));}
    @Inject(method="humidity",at=@At("HEAD"),cancellable=true)
    private void around_planet$humidity(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(humidity));}
    @Inject(method="continentalness",at=@At("HEAD"),cancellable=true)
    private void around_planet$continents(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(continentalness));}
    @Inject(method="erosion",at=@At("HEAD"),cancellable=true)
    private void around_planet$erosion(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(erosion));}
    @Inject(method="depth",at=@At("HEAD"),cancellable=true)
    private void around_planet$depth(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(depth));}
    @Inject(method="weirdness",at=@At("HEAD"),cancellable=true)
    private void around_planet$weirdness(CallbackInfoReturnable<DensityFunction> cir){if(around_planet$routing!=null)cir.setReturnValue(around_planet$effective(weirdness));}
}

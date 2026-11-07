package io.github.astelev.around_planet.generation;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Blend noise leaves, leaving vanilla interpolation/cell caches and terrain splines outside them intact. */
public record SeamDensity(DensityFunction input,SeamBlend blend) implements DensityFunction {
    private static final Set<String> LEAVES=Set.of("Noise","Shift","ShiftA","ShiftB","ShiftedNoise","WeirdScaledSampler");
    public static Visitor visitor(SeamBlend blend) {
        return new Visitor() {
            private final Map<DensityFunction,DensityFunction> cache=new HashMap<>();
            @Override public DensityFunction apply(DensityFunction f) {
                return cache.computeIfAbsent(f,key->key instanceof BlendedNoise
                        || key.getClass().getEnclosingClass()==DensityFunctions.class && LEAVES.contains(key.getClass().getSimpleName())
                        ?new SeamDensity(key,blend):key);
            }
        };
    }
    @Override public double compute(FunctionContext context) {
        int x=blend.canonicalX(context.blockX());double weight=blend.weight(x);
        FunctionContext local=x==context.blockX()?context:new SinglePointContext(x,context.blockY(),context.blockZ());
        double value=input.compute(local);
        if(weight==0)return value;
        double opposite=input.compute(new SinglePointContext(blend.oppositeX(x),context.blockY(),context.blockZ()));
        return value+(opposite-value)*weight;
    }
    @Override public void fillArray(double[] values,ContextProvider provider) {provider.fillAllDirectly(values,this);}
    @Override public DensityFunction mapAll(Visitor visitor) {return visitor.apply(new SeamDensity(input.mapAll(visitor),blend));}
    @Override public double minValue(){return input.minValue();}
    @Override public double maxValue(){return input.maxValue();}
    // Runtime-only wrapper, never registered in or written into the worldgen datapack registry.
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){return KeyDispatchDataCodec.of(MapCodec.unit(this));}
}

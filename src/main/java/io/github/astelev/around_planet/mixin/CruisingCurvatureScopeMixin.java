package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.client.FallbackPlaneRenderer",remap=false)
public abstract class CruisingCurvatureScopeMixin {
    @WrapMethod(method="render") private static void surface$fallbackPass(Object params,Operation<Void> original){
        io.github.astelev.around_planet.client.SurfaceRenderState.enterFallback();
        try{original.call(params);}finally{io.github.astelev.around_planet.client.SurfaceRenderState.exitFallback();}
    }
}

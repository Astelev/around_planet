package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.compat.dh.FallbackSurfaceMesh",remap=false)
public abstract class CruisingCurvatureMeshMixin {
    @Inject(method="top",at=@At("HEAD"),cancellable=true)
    private static void surface$subdivideOuterPlane(@Coerce Object sink,float x0,float z0,float x1,float z1,CallbackInfo ci){io.github.astelev.around_planet.compat.CruisingCurvatureMesh.top(sink,x0,z0,x1,z1);ci.cancel();}
}

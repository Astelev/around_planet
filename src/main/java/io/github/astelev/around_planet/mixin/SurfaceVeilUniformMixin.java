package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="foundry.veil.impl.client.render.shader.program.ShaderProgramImpl",remap=false)
public abstract class SurfaceVeilUniformMixin {
    @Shadow public abstract int getProgram();
    @Inject(method="bind",at=@At("TAIL")) private void surface$uniforms(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.upload(getProgram());}
    @Inject(method={"freeInternal","compile","recompile"},at=@At("RETURN")) private void surface$reload(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.clear();}
}

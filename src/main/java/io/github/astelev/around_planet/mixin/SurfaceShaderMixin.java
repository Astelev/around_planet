package io.github.astelev.around_planet.mixin;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ShaderInstance.class)
public abstract class SurfaceShaderMixin {
    @Shadow public abstract int getId();
    @Inject(method="apply",at=@At("TAIL")) private void surface$uniforms(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.upload(getId());}
    @Inject(method="close",at=@At("HEAD")) private void surface$reload(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.clear();}
}

package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo
@Mixin(targets="foundry.veil.impl.client.render.shader.compiler.DirectShaderCompiler",remap=false)
public abstract class SurfaceVeilSourceMixin {
    @ModifyExpressionValue(method="compile(ILfoundry/veil/api/client/render/shader/compiler/VeilShaderSource;)Lfoundry/veil/api/client/render/shader/compiler/CompiledShader;",at=@At(value="INVOKE",target="Lfoundry/veil/api/client/render/shader/compiler/VeilShaderSource;sourceCode()Ljava/lang/String;"))
    private String surface$nativeShips(String source){return (source.contains("_sable_unpack") || source.contains("DhViewProjection") && source.contains("surfacePosition"))?io.github.astelev.around_planet.client.CurvatureShaders.patch(source):source;}
}

package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** Veil rereads vanilla resources when rebuilding dynamic-buffer variants. */
@Pseudo
@Mixin(targets="foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler",remap=false)
public abstract class SurfaceVeilVanillaSourceMixin {
    @ModifyExpressionValue(method="compileShader",at=@At(value="INVOKE",target="Lorg/apache/commons/io/IOUtils;toString(Ljava/io/Reader;)Ljava/lang/String;"))
    private String surface$variant(String source,@Local(argsOnly=true) ShaderInstance shader){
        return io.github.astelev.around_planet.client.CurvatureShaders.worldProgram(shader.getName())?io.github.astelev.around_planet.client.CurvatureShaders.patch(source):source;
    }
}

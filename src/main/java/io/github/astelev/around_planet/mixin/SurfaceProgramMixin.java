package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Program.class)
public abstract class SurfaceProgramMixin {
    @ModifyExpressionValue(method="compileShaderInternal",at=@At(value="INVOKE",target="Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;"))
    private static String surface$vertex(String source,@Local(argsOnly=true) Program.Type type,@Local(argsOnly=true,ordinal=0) String name){return type==Program.Type.VERTEX && io.github.astelev.around_planet.client.CurvatureShaders.worldProgram(name)?io.github.astelev.around_planet.client.CurvatureShaders.patch(source):source;}
}

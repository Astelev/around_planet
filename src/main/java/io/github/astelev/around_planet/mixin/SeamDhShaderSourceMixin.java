package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShader",remap=false)
public abstract class SeamDhShaderSourceMixin {
    @Inject(method="loadFile",at=@At("RETURN"),cancellable=true)
    private static void around_planet$exactSeamClip(String path,boolean absolute,CallbackInfoReturnable<String> cir) {
        if(!path.equals("assets/distanthorizons/shaders/terrain/gl/frag.frag"))return;
        String source=cir.getReturnValue();
        String replacement="uniform bool apSeamEnabled;\nuniform float apSeamMinX;\nuniform float apSeamMaxX;\n"
                +"void main() {\nif(apSeamEnabled && (vertexWorldPos.x < apSeamMinX || vertexWorldPos.x >= apSeamMaxX)) discard;\n";
        String patched=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)\\s*\\{",replacement);
        if(patched.equals(source))throw new IllegalStateException("Unsupported DH terrain fragment entry point");
        cir.setReturnValue(patched);
    }
}

package io.github.astelev.around_planet.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShader",remap=false)
public abstract class SeamDhShaderSourceMixin {
    @Inject(method="loadFile",at=@At("RETURN"),cancellable=true)
    private static void around_planet$exactSeamClip(String path,boolean absolute,CallbackInfoReturnable<String> cir) {
        if(path.equals("assets/distanthorizons/shaders/terrain/gl/vert.vert")) {
            String source=cir.getReturnValue();
            String patched=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)\\s*\\{",
                    "uniform bool apPortalNearClip;\nvoid main() {");
            if(patched.equals(source))throw new IllegalStateException("Unsupported DH terrain vertex entry point");
            int end=patched.lastIndexOf('}');
            cir.setReturnValue(patched.substring(0,end)
                    +"\ngl_ClipDistance[0] = apPortalNearClip ? gl_Position.w - 0.05 : 1.0;\n"+patched.substring(end));
            return;
        }
        if(!path.equals("assets/distanthorizons/shaders/terrain/gl/frag.frag"))return;
        String source=cir.getReturnValue();
        String replacement="uniform bool apChunkMaskEnabled;\nuniform sampler3D apChunkMask;\nuniform vec3 apChunkOffset;\n"
                +"uniform bool apSeamEnabled;\nuniform float apSeamMinX;\nuniform float apSeamMaxX;\n"
                +"void main() {\n"
                +"if(apChunkMaskEnabled) { ivec3 cell=ivec3(floor((vertexWorldPos+apChunkOffset)/16.0)); "
                +"ivec3 dims=textureSize(apChunkMask,0); if(all(greaterThanEqual(cell,ivec3(0))) "
                +"&& all(lessThan(cell,dims)) && texelFetch(apChunkMask,cell,0).r>0.5) discard; }\n"
                +"if(apSeamEnabled && (vertexWorldPos.x < apSeamMinX || vertexWorldPos.x >= apSeamMaxX)) discard;\n";
        String patched=source.replaceFirst("void\\s+main\\s*\\(\\s*\\)\\s*\\{",replacement);
        if(patched.equals(source))throw new IllegalStateException("Unsupported DH terrain fragment entry point");
        cir.setReturnValue(patched);
    }
}

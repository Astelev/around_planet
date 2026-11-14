package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface",remap=false)
public abstract class SurfaceSodiumUniformMixin {
    @Inject(method="setupState",at=@At("TAIL")) private void surface$uniforms(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.upload(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM));}
    @Inject(method="<init>",at=@At("RETURN")) private void surface$reload(CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.clear();}
}

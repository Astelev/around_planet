package io.github.astelev.around_planet.mixin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader",remap=false)
public abstract class SurfaceSodiumSourceMixin {
    @Inject(method="getShaderSource",at=@At("RETURN"),cancellable=true)
    private static void surface$source(ResourceLocation id,CallbackInfoReturnable<String> ci){if(id.getPath().endsWith("block_layer_opaque.vsh"))ci.setReturnValue(io.github.astelev.around_planet.client.CurvatureShaders.patch(ci.getReturnValue()));}
}

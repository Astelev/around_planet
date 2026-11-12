package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManager",remap=false)
public abstract class SurfaceVeilRelinkMixin {
    @Inject(method="markRecompiled",at=@At("HEAD"))
    private void surface$relinked(net.minecraft.client.renderer.ShaderInstance shader,CallbackInfo ci){io.github.astelev.around_planet.client.CurvatureShaders.clear();}
}

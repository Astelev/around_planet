package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class SeamRendererDiagnosticsMixin {
    @Inject(method="allChanged",at=@At("HEAD"))
    private void around_planet$reset(CallbackInfo ci){SeamDiagnostics.event("client","native.renderer_reset",()->"caller="+SeamDiagnostics.caller());}
}

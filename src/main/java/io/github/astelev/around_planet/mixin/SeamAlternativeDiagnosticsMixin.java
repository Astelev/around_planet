package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets={"net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer","org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer"},remap=false)
public abstract class SeamAlternativeDiagnosticsMixin {
    @Inject(method="reload",at=@At("HEAD"),require=0)
    private void around_planet$reset(CallbackInfo ci){SeamDiagnostics.event("client","alternative.renderer_reset",()->"caller="+SeamDiagnostics.caller());}
}

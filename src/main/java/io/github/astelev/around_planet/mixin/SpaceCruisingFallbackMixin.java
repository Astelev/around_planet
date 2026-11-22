package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.client.FallbackPlaneRenderer",remap=false)
public abstract class SpaceCruisingFallbackMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private static void space$noSurface(Object params,CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null && SpaceCells.contains(p))ci.cancel();}
}

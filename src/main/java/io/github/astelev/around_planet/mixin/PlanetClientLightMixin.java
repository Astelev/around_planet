package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The vanilla client's integer sky-darken field is initialized once, unlike its float lightmap API. */
@Mixin(Level.class)
public abstract class PlanetClientLightMixin {
    private boolean planet$cameraDaylight(){return (Object)this instanceof ClientLevel && PlanetDaylight.enabled((Level)(Object)this) && !io.github.astelev.around_planet.client.SpaceRenderView.active();}
    @Inject(method="getSkyDarken",at=@At("HEAD"),cancellable=true)
    private void planet$cameraDark(CallbackInfoReturnable<Integer> ci){if(planet$cameraDaylight())ci.setReturnValue(PlanetDaylight.darken((Level)(Object)this,Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x));}
    @Inject(method="isDay",at=@At("HEAD"),cancellable=true)
    private void planet$cameraDay(CallbackInfoReturnable<Boolean> ci){if(planet$cameraDaylight())ci.setReturnValue(PlanetDaylight.darken((Level)(Object)this,Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x)<4);}
}

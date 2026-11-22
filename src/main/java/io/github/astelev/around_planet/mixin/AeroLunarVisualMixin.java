package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="net.aste.aeroworks.client.WindVisualEvents",remap=false)
public abstract class AeroLunarVisualMixin {
 @Shadow private static void reset(net.minecraft.client.multiplayer.ClientLevel level){throw new AssertionError();}
 @Unique private static boolean lunar(){var l=net.minecraft.client.Minecraft.getInstance().level;return l!=null&&l.dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension());}
 @Inject(method="tick",at=@At("HEAD"),cancellable=true,require=0)
 private static void moonTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e,CallbackInfo ci){if(lunar()){reset(net.minecraft.client.Minecraft.getInstance().level);ci.cancel();}}
 @Inject(method="camera",at=@At("HEAD"),cancellable=true,require=0)
 private static void moonCamera(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e,CallbackInfo ci){if(lunar())ci.cancel();}
}

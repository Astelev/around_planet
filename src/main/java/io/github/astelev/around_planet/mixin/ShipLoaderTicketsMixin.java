package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",remap=false)
public abstract class ShipLoaderTicketsMixin {
 @Inject(method="tick",at=@At("HEAD"))
 private void around_planet$loaders(dev.ryanhcode.sable.api.sublevel.SubLevelContainer c,CallbackInfo ci){io.github.astelev.around_planet.compat.ShipLoaderBridge.before((dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer)c);}
}

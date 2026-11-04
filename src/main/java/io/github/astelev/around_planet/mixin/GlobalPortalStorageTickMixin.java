package io.github.astelev.around_planet.mixin;

import java.lang.ref.WeakReference;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

/** Aeronautics 1.1.4 registers the shutdown callback on ServerTickEvent.Post. */
@Mixin(value=GlobalPortalStorage.class,remap=false)
public abstract class GlobalPortalStorageTickMixin {
    @Shadow @Final public WeakReference<ServerLevel> world;
    @Inject(method="onServerClose",at=@At("HEAD"),cancellable=true)
    private void around_planet$onlyWhenStopping(CallbackInfo ci) {
        var level=world.get();
        if(level!=null && level.getServer().isRunning())ci.cancel();
    }
}

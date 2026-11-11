package io.github.astelev.around_planet.mixin;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import io.github.astelev.around_planet.compat.ShipTransferService;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem",remap=false)
public abstract class SeamShipTransferMixin {
    @Shadow @Final private ServerLevel level;
    @Inject(method="tick",at=@At("RETURN"))
    private void around_planet$transfer(SubLevelContainer container,CallbackInfo ci){io.github.astelev.around_planet.compat.SpaceShipTransfers.check(level,(ServerSubLevelContainer)container);ShipTransferService.check(level,(ServerSubLevelContainer)container);}
}

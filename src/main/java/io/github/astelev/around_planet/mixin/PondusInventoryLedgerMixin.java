package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="org.fractalinteractive.ci.pondus_inventory_fi.listener.InventoryChangeListener",remap=false)
public abstract class PondusInventoryLedgerMixin {
    @Inject(method="onInventoryChanged",at=@At("HEAD"))
    private static void around_planet$refreshTotal(dev.ryanhcode.sable.sublevel.ServerSubLevel ship,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.entity.BlockEntity be,CallbackInfo ci){io.github.astelev.around_planet.compat.PondusMassCompatibility.refresh(be);}
}

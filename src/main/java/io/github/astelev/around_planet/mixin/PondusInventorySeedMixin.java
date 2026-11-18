package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="org.fractalinteractive.ci.pondus_inventory_fi.mass.InventoryMassCache",remap=false)
public abstract class PondusInventorySeedMixin {
    @Inject(method="initializeCache",at=@At("HEAD"))
    private static void around_planet$seedTotal(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.entity.BlockEntity be,@Coerce Object definition,CallbackInfo ci){io.github.astelev.around_planet.compat.PondusMassCompatibility.refresh(be);}
}

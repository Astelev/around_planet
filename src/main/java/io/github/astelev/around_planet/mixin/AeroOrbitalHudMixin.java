package io.github.astelev.around_planet.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
@Pseudo
@Mixin(targets="com.cxw.aeroengineering.content.client.EngineMonitorHelmetHud",remap=false)
public abstract class AeroOrbitalHudMixin {
    /** Our circular instrument replaces only this sensor's generic text section. */
    @WrapOperation(method="createSnapshot",at=@At(value="INVOKE",target="Lcom/cxw/aeroengineering/content/item/EngineMonitorHelmetItem;shouldHideLeftHud(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean planet$noDuplicateText(ItemStack helmet,BlockPos pos,Operation<Boolean> original,@Local(argsOnly=true) Level level){return level.getBlockEntity(pos) instanceof io.github.astelev.around_planet.sensor.OrbitalSensorBlockEntity || original.call(helmet,pos);}
}

package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.client.DhFlightController",remap=false)
public abstract class SpaceCruisingDhMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;getAsBoolean()Z"))
    private static boolean space$normalDetail(ModConfigSpec.BooleanValue setting,Operation<Boolean> original){var p=Minecraft.getInstance().player;return !(p!=null && SpaceCells.contains(p)) && original.call(setting);}
}

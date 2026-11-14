package io.github.astelev.around_planet.mixin;
import io.github.astelev.around_planet.space.SpaceCells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="io.github.astelev.cruising.client.ClientFlightView",remap=false)
public abstract class SpaceCruisingClientViewMixin {
    @Inject(method="get",at=@At("HEAD"),cancellable=true)
    private static void space$noTemporaryAir(ClientLevel level,int x,int z,CallbackInfoReturnable<LevelChunk> ci){var p=Minecraft.getInstance().player;if(p!=null && SpaceCells.contains(p))ci.setReturnValue(null);}
    @Inject(method="isActive",at=@At("HEAD"),cancellable=true)
    private static void space$noHeightMode(ClientLevel level,CallbackInfoReturnable<Boolean> ci){var p=Minecraft.getInstance().player;if(p!=null && SpaceCells.contains(p))ci.setReturnValue(false);}
}

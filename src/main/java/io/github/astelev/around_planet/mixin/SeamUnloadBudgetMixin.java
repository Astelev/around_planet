package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Queue;
import java.util.function.BooleanSupplier;

/** Let shutdown pump generation completions instead of immediately retrying one unready unload forever. */
@Mixin(ChunkMap.class)
public abstract class SeamUnloadBudgetMixin {
    @Shadow @Final private ServerLevel level;
    @Unique private int around_planet$budget;
    @Unique private boolean around_planet$closing;
    @Inject(method="processUnloads",at=@At("HEAD"))
    private void around_planet$batch(BooleanSupplier time,CallbackInfo ci) {
        around_planet$closing=!level.getServer().isRunning() && PlanetBorders.get(level.getWorldBorder())!=null;
        around_planet$budget=-1;
    }
    @WrapOperation(method="processUnloads",at=@At(value="INVOKE",target="Ljava/util/Queue;poll()Ljava/lang/Object;"))
    private Object around_planet$yieldRequeuedUnload(Queue<?> queue,Operation<Object> original) {
        if(around_planet$closing) {
            if(around_planet$budget<0)around_planet$budget=Math.max(1,queue.size());
            if(around_planet$budget--==0)return null;
        }
        return original.call(queue);
    }
}

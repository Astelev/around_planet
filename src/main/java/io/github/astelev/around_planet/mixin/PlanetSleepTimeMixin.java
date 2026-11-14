package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.astelev.around_planet.space.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Sleeping advances to local dawn, retaining NeoForge's sleep-finished event. */
@Mixin(ServerLevel.class)
public abstract class PlanetSleepTimeMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/event/EventHooks;onSleepFinished(Lnet/minecraft/server/level/ServerLevel;JJ)J"))
    private long planet$localDawn(ServerLevel level,long proposed,long current,Operation<Long> original){
        if(PlanetDaylight.enabled(level)){double wait=0;for(var p:level.players())if(p.isSleeping() && !SpaceCells.contains(p))wait=Math.max(wait,PlanetSpin.ticksUntilDawn(current,PlanetDaylight.worldX(level,p.blockPosition()),io.github.astelev.around_planet.PlanetConfig.PLANET_SIZE.get()));proposed=current+(long)Math.ceil(wait);}
        return original.call(level,proposed,current);
    }
}

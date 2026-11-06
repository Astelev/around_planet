package io.github.astelev.around_planet.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.astelev.around_planet.client.SeamImages;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo
@Mixin(targets={"net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection",
        "org.embeddedt.embeddium.impl.world.cloned.ClonedChunkSection"},remap=false)
public abstract class SeamAlternativeLightMixin {
    @WrapOperation(method="copyLightArray",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getLightEngine()Lnet/minecraft/world/level/lighting/LevelLightEngine;"),require=0)
    private static LevelLightEngine around_planet$isolatedLight(Level level,Operation<LevelLightEngine> original) {
        return level instanceof ClientLevel client && PlanetBorders.get(level.getWorldBorder())!=null
                ?SeamImages.renderLight(client):original.call(level);
    }
}

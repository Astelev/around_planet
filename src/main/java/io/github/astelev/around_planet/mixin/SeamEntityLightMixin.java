package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.astelev.around_planet.client.SeamEntityImages;
import io.github.astelev.around_planet.client.SeamImages;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;

/** Keep renderer-specific emission and burning overrides; replace only the image's ambient lookup. */
@Mixin(EntityRenderer.class)
public abstract class SeamEntityLightMixin {
    @WrapOperation(method={"getSkyLightLevel","getBlockLightLevel"},at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"))
    private int around_planet$imageLight(Level level,LightLayer layer,BlockPos pos,Operation<Integer> original,
                                        @Local(argsOnly=true) Entity entity) {
        return level instanceof ClientLevel client && SeamEntityImages.get(entity.getUUID())==entity
                ?SeamImages.renderLight(client).getLayerListener(layer).getLightValue(pos)
                :original.call(level,layer,pos);
    }
}

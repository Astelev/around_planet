package io.github.astelev.around_planet.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.astelev.around_planet.client.ShipRenderScope;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public abstract class SeamShipRenderScopeMixin {
    @WrapMethod(method="renderLevel")
    private void around_planet$renderScope(DeltaTracker delta,boolean outline,Camera camera,GameRenderer renderer,
            LightTexture light,Matrix4f modelView,Matrix4f projection,Operation<Void> original) {
        ShipRenderScope.enter();io.github.astelev.around_planet.client.SurfaceRenderState.enter(modelView,projection);
        try{original.call(delta,outline,camera,renderer,light,modelView,projection);}finally{io.github.astelev.around_planet.client.SurfaceRenderState.exit();ShipRenderScope.exit();}
    }
}

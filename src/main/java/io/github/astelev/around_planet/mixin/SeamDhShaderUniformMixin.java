package io.github.astelev.around_planet.mixin;

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.render.RenderParams;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram_neoforge",remap=false)
public abstract class SeamDhShaderUniformMixin {
    @Unique private int around_planet$enabled=-1,around_planet$min=-1,around_planet$max=-1,around_planet$program=-1,around_planet$mask=-1,around_planet$sampler=-1;
    @Inject(method="fillUniformData",at=@At("RETURN"))
    private void around_planet$clipUniforms(DhApiRenderParam parameters,CallbackInfo ci) {
        var shader=(com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram_neoforge)(Object)this;
        if(around_planet$enabled<0 || around_planet$program!=shader.getId()) {
            around_planet$program=shader.getId();
            around_planet$enabled=shader.getUniformLocation("apSeamEnabled");
            around_planet$min=shader.getUniformLocation("apSeamMinX");
            around_planet$max=shader.getUniformLocation("apSeamMaxX");
            around_planet$mask=shader.getUniformLocation("apChunkMaskEnabled");
            around_planet$sampler=shader.getUniformLocation("apChunkMask");
        }
        float radius=(float)io.github.astelev.around_planet.client.SurfaceRenderState.radius();
        shader.setUniform(shader.getUniformLocation("apCurveRadius"),radius);
        if(radius>0){shader.setUniform(shader.getUniformLocation("uEarthRadius"),0f);io.github.astelev.around_planet.client.SurfaceRenderState.dhDraws++;}
        shader.setUniform(around_planet$mask,false);
        shader.setUniform(around_planet$sampler,7);
        var level=Minecraft.getInstance().level;
        var topology=level==null?null:PlanetBorders.get(level.getWorldBorder());
        boolean enabled=topology!=null && parameters instanceof RenderParams;
        shader.setUniform(around_planet$enabled,enabled);
        if(enabled) {
            double cameraX=((RenderParams)parameters).exactCameraPosition.x;
            shader.setUniform(around_planet$min,(float)(topology.west()-cameraX));
            shader.setUniform(around_planet$max,(float)(topology.east()-cameraX));
        }
    }
}

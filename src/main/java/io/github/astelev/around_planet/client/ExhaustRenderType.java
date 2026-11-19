package io.github.astelev.around_planet.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
/** Shared additive gas material; depth-tested, no depth writes, no per-nozzle render passes. */
public final class ExhaustRenderType extends RenderType {
    public static final RenderType TYPE=create("around_planet_exhaust",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,2048,false,true,CompositeState.builder()
        .setShaderState(new ShaderStateShard(()->PropulsionExhaustRenderer.shader))
        .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private ExhaustRenderType(){super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
}

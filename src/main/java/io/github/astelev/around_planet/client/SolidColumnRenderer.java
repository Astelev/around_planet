package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.solid.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
/** One shrinking propellant core per section, visible through four framed windows. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SolidColumnRenderer implements BlockEntityRenderer<SolidColumnBlockEntity> {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
    public static long frames;
    public SolidColumnRenderer(BlockEntityRendererProvider.Context c){blocks=c.getBlockRenderDispatcher();}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(SolidRegistry.TYPE.get(),SolidColumnRenderer::new);}
    @Override public void render(SolidColumnBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.fuel()<=0)return;frames++;pose.pushPose();pose.translate(.5,.5,.5);
        switch(be.getBlockState().getValue(BlockStateProperties.AXIS)){case X->pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));case Z->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));default->{}}
        pose.translate(-.5,-.5,-.5);float height=(float)(.8125*be.fraction());
        pose.translate(.15,be.burnPositive()?.09375:.90625-height,.15);pose.scale(.7f,height,.7f);
        blocks.renderSingleBlock(SolidRegistry.FUEL.get().defaultBlockState(),pose,buffers,light,overlay);pose.popPose();
    }
}

package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.sensor.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class OrbitalSensorRenderer implements BlockEntityRenderer<OrbitalSensorBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");public static long frames;
    public OrbitalSensorRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OrbitalSensorRegistry.TYPE.get(),OrbitalSensorRenderer::new);}
    @Override public void render(OrbitalSensorBlockEntity be,float partial,PoseStack p,MultiBufferSource buffers,int light,int overlay){long start=io.github.astelev.around_planet.diagnostics.SeamDiagnostics.start();var read=OrbitalSensorVisual.read(be,partial);var v=buffers.getBuffer(RenderType.entitySolid(TEXTURE));p.pushPose();p.translate(.5,.56,.5);p.mulPose(Axis.YP.rotationDegrees(180-be.getBlockState().getValue(OrbitalSensorBlock.FACING).toYRot()));p.mulPose(Axis.YP.rotationDegrees((float)-read.yaw()));
        for(int i=0;i<24;i++){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(i*15));box(p,v,-.045f,-.018f,.30f,.045f,.018f,.34f,0xFFB67B38,light,overlay);p.popPose();}
        p.mulPose(Axis.XP.rotationDegrees((float)read.pitch()));for(int i=0;i<20;i++){p.pushPose();p.mulPose(Axis.XP.rotationDegrees(i*18));box(p,v,-.016f,.215f,-.04f,.016f,.25f,.04f,0xFF48B8CE,light,overlay);p.popPose();}
        box(p,v,-.018f,-.018f,-.29f,.018f,.018f,.18f,be.active()?0xFFFFDC54:0xFF888888,light,overlay);box(p,v,-.075f,-.025f,-.25f,.075f,.025f,-.20f,0xFFFFDC54,light,overlay);box(p,v,-.035f,-.035f,-.32f,.035f,.035f,-.25f,0xFFFFFFFF,light,overlay);box(p,v,-.045f,-.025f,.13f,.045f,.025f,.20f,0xFFCE5445,light,overlay);box(p,v,-.05f,-.05f,-.05f,.05f,.05f,.05f,0xFFE5EFF2,light,overlay);p.popPose();frames++;io.github.astelev.around_planet.diagnostics.SeamDiagnostics.time("client","space.sensor_model",start);
    }
    private static final int[][] CORNERS={{0,2,3,1},{5,7,6,4},{4,6,2,0},{1,3,7,5},{2,6,7,3},{4,0,1,5}};
    private static final float[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static void box(PoseStack p,VertexConsumer v,float a,float b,float c,float d,float e,float f,int color,int light,int overlay){for(int face=0;face<6;face++)for(int i=0;i<4;i++){int corner=CORNERS[face][i];v.addVertex(p.last(),(corner&1)==0?a:d,(corner&2)==0?b:e,(corner&4)==0?c:f).setColor(color).setUv((i==2||i==3)?1:0,(i==1||i==2)?0:1).setOverlay(overlay).setLight(light).setNormal(p.last(),NORMALS[face][0],NORMALS[face][1],NORMALS[face][2]);}}
}

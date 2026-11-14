package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
/** Two batched, full-bright atlas quads per active nozzle, automatically transformed with the ship. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PropulsionExhaustRenderer implements BlockEntityRenderer<PropulsionBlockEntity> {
    public static long frames,quads;
    private static final RenderType TYPE=RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS);
    private static final ResourceLocation FIRE=ResourceLocation.withDefaultNamespace("block/fire_0"),SOUL=ResourceLocation.withDefaultNamespace("block/soul_fire_0");
    public PropulsionExhaustRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(PropulsionRegistry.TYPE.get(),PropulsionExhaustRenderer::new);}
    @Override public void render(PropulsionBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.getLevel()==null||be.kind()==PropulsionRegistry.Kind.DAMPER||be.thrust()<=0)return;
        boolean rsu=be.kind()==PropulsionRegistry.Kind.RSU;int count=rsu?4:1;boolean active=false;for(int i=0;i<count;i++)active|=be.signal(i)>0;if(!active)return;
        long start=io.github.astelev.around_planet.diagnostics.SeamDiagnostics.start();var frame=OrbitalSensorBlock.frame(be.getBlockState());var r=frame.right();var f=frame.forward();var u=frame.up();var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(rsu?SOUL:FIRE);var vertices=sprite.wrap(buffers.getBuffer(TYPE));
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f((float)r.x(),(float)r.y(),(float)r.z(),(float)f.x(),(float)f.y(),(float)f.z(),(float)u.x(),(float)u.y(),(float)u.z())));
        double time=be.getLevel().getGameTime()+partial;double sin=Math.sin(Math.PI/8),cos=Math.cos(Math.PI/8);
        for(int i=0;i<count;i++){
            var jet=ExhaustMath.jet(be.thrust(),be.signal(i),rsu,time*.8+((be.getBlockPos().asLong()+i*31)&255));if(!jet.visible())continue;
            pose.pushPose();var direction=new org.joml.Vector3f(0,-1,0);
            if(rsu){float x=0,z=0;switch(i){case 0->z=-1;case 1->x=1;case 2->z=1;default->x=-1;}pose.translate(x*(1+cos)/8,(1.75+2*sin)/16-.5,z*(1+cos)/8);direction.set((float)(x*cos),(float)sin,(float)(z*cos));}
            else pose.translate(0,-.505,0);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),direction));plane(pose,vertices,jet,false);plane(pose,vertices,jet,true);quads+=2;pose.popPose();
        }
        pose.popPose();frames++;io.github.astelev.around_planet.diagnostics.SeamDiagnostics.time("client","propulsion.exhaust",start);
    }
    private static void plane(PoseStack pose,VertexConsumer v,ExhaustMath.Jet jet,boolean cross){
        for(int i=0;i<4;i++){boolean right=i==1||i==2,tip=i>=2;float side=(right?1:-1)*jet.halfWidth()*(tip?.12f:1),y=tip?jet.length():0;
            v.addVertex(pose.last(),cross?0:side,y,cross?side:0).setColor(255,255,255,jet.alpha()).setUv(right?1:0,tip?0:1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),cross?-1:0,0,cross?0:1);
        }
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(PropulsionBlockEntity be){return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(be.kind()==PropulsionRegistry.Kind.CREATIVE?4.5:be.kind()==PropulsionRegistry.Kind.RSU?1.5:0);}
    @Override public int getViewDistance(){return 96;}
}

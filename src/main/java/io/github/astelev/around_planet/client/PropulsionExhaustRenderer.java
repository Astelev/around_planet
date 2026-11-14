package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.propulsion.*;
import io.github.astelev.around_planet.sensor.OrbitalSensorBlock;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
/** Two eight-sided, untextured emissive sections per nozzle; no fire, particles or texture uploads. */
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PropulsionExhaustRenderer implements BlockEntityRenderer<PropulsionBlockEntity> {
    public static long frames,quads;
    private static final RenderType TYPE=RenderType.lightning();
    private static final int SIDES=8;
    private static final float[] X=new float[SIDES+1],Z=new float[SIDES+1];
    static {for(int i=0;i<=SIDES;i++){X[i]=(float)Math.cos(i*2*Math.PI/SIDES);Z[i]=(float)Math.sin(i*2*Math.PI/SIDES);}}
    public PropulsionExhaustRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(PropulsionRegistry.TYPE.get(),PropulsionExhaustRenderer::new);}
    @Override public void render(PropulsionBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.getLevel()==null||be.kind()==PropulsionRegistry.Kind.DAMPER||be.thrust()<=0)return;
        boolean rsu=be.kind()==PropulsionRegistry.Kind.RSU;int count=rsu?4:1;boolean active=false;for(int i=0;i<count;i++)active|=be.signal(i)>0;if(!active)return;
        long start=io.github.astelev.around_planet.diagnostics.SeamDiagnostics.start();var frame=OrbitalSensorBlock.frame(be.getBlockState());var r=frame.right();var f=frame.forward();var u=frame.up();var vertices=buffers.getBuffer(TYPE);
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f((float)r.x(),(float)r.y(),(float)r.z(),(float)f.x(),(float)f.y(),(float)f.z(),(float)u.x(),(float)u.y(),(float)u.z())));
        double time=be.getLevel().getGameTime()+partial;double sin=Math.sin(Math.PI/8),cos=Math.cos(Math.PI/8);
        for(int i=0;i<count;i++){
            var jet=ExhaustMath.jet(be.thrust(),be.signal(i),rsu,time*.8+((be.getBlockPos().asLong()+i*31)&255));if(!jet.visible())continue;
            pose.pushPose();var direction=new org.joml.Vector3f(0,-1,0);
            if(rsu){float x=0,z=0;switch(i){case 0->z=-1;case 1->x=1;case 2->z=1;default->x=-1;}pose.translate(x*(1+cos)/8,(1.75+2*sin)/16-.5,z*(1+cos)/8);direction.set((float)(x*cos),(float)sin,(float)(z*cos));}
            else pose.translate(0,-.505,0);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),direction));plume(pose,vertices,jet,rsu);quads+=2*SIDES;pose.popPose();
        }
        pose.popPose();frames++;io.github.astelev.around_planet.diagnostics.SeamDiagnostics.time("client","propulsion.exhaust",start);
    }
    private static void plume(PoseStack pose,VertexConsumer v,ExhaustMath.Jet jet,boolean rsu){
        float neck=jet.halfWidth()*.65f;
        section(pose,v,0,jet.activeLength(),jet.halfWidth(),neck,0xE8FAFF,rsu?0x70E8FF:0x90BCFF,jet.alpha(),jet.alpha());
        section(pose,v,jet.activeLength(),jet.length(),neck,neck+jet.coneLength()*(rsu?.22f:.25f),rsu?0x50C8F0:0x6498EB,rsu?0x307CC0:0x3C58A0,jet.coneAlpha(),0);
    }
    private static void section(PoseStack pose,VertexConsumer v,float start,float end,float r0,float r1,int color0,int color1,int alpha0,int alpha1){
        for(int i=0;i<SIDES;i++){
            vertex(pose,v,X[i]*r0,start,Z[i]*r0,color0,alpha0);
            vertex(pose,v,X[i+1]*r0,start,Z[i+1]*r0,color0,alpha0);
            vertex(pose,v,X[i+1]*r1,end,Z[i+1]*r1,color1,alpha1);
            vertex(pose,v,X[i]*r1,end,Z[i]*r1,color1,alpha1);
        }
    }
    private static void vertex(PoseStack pose,VertexConsumer v,float x,float y,float z,int color,int alpha){v.addVertex(pose.last().pose(),x,y,z).setColor((color>>16)&255,(color>>8)&255,color&255,alpha);}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(PropulsionBlockEntity be){return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(be.kind()==PropulsionRegistry.Kind.CREATIVE?4.5:be.kind()==PropulsionRegistry.Kind.RSU?1.5:0);}
    @Override public int getViewDistance(){return 96;}
}

package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import java.util.Random;
/** Cached inertial stars/sun and an opaque convex sphere; no terrain or world-generation access. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class SpaceSkyRenderer {
    private static net.minecraft.client.renderer.ShaderInstance planetShader;
    @net.neoforged.bus.api.SubscribeEvent public static void shaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event)throws java.io.IOException{event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet","space_planet"),DefaultVertexFormat.POSITION_COLOR),shader->planetShader=shader);}
    static Matrix4f inverse(SpaceFrame basis){return new Matrix4f().m00((float)basis.east().x()).m01((float)basis.up().x()).m02((float)basis.south().x()).m10((float)basis.east().y()).m11((float)basis.up().y()).m12((float)basis.south().y()).m20((float)basis.east().z()).m21((float)basis.up().z()).m22((float)basis.south().z());}
    private static VertexBuffer stars,sun,planet;public static long frames,planetFrames;
    public static void render(Matrix4f view,Matrix4f projection,float partial,Camera camera){
        var p=SpaceHud.current();if(p==null || !p.active() || p.radius()<=0)return;
        ensureMeshes();var state=new SpaceState(new SpaceVector(p.x(),p.y(),p.z()),new SpaceVector(p.vx(),p.vy(),p.vz()));
        if(!p.returning())state=new SpaceTrajectory("around_planet:render",0,state,p.mu()).afterSeconds(SpaceHud.age(partial));
        var cam=camera.getPosition();var observer=SpaceEnvironment.observer(state,p.frame(),new SpaceVector(cam.x,cam.y,cam.z),p.anchor());double distance=observer.length();
        if(distance<=p.radius())return;
        var basis=p.frame();var inverse=inverse(basis);
        float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        try {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE/2);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.depthMask(false);RenderSystem.disableDepthTest();RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var sky=new Matrix4f(view).mul(inverse);draw(stars,sky,projection);draw(sun,sky,projection);
            var center=basis.toLocal(observer.scale(-100/distance));var model=new Matrix4f(view).translate((float)center.x(),(float)center.y(),(float)center.z()).mul(inverse).scale((float)(100*p.radius()/distance));
            RenderSystem.disableBlend();RenderSystem.enableCull();
            if(planetShader!=null){planetShader.safeGetUniform("PlanetSpin").set((float)PlanetDaylight.spin(net.minecraft.client.Minecraft.getInstance().level,partial).angle());var light=SpaceEnvironment.SUN_DIRECTION;planetShader.safeGetUniform("SunDirection").set((float)light.x(),(float)light.y(),(float)light.z());RenderSystem.setShader(()->planetShader);planet.bind();planet.drawWithShader(model,projection,planetShader);}
            AtmosphereRenderer.render(sky,projection,observer,p.radius(),p.atmosphereTop());RenderSystem.setShader(GameRenderer::getPositionColorShader);frames++;planetFrames++;
        } finally {VertexBuffer.unbind();RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);}
    }
    static void stars(Matrix4f view,Matrix4f projection,float brightness){ensureMeshes();RenderSystem.setShaderColor(brightness,brightness,brightness,brightness);draw(stars,view,projection);RenderSystem.setShaderColor(1,1,1,1);}
    private static void draw(VertexBuffer b,Matrix4f view,Matrix4f projection){b.bind();b.drawWithShader(view,projection,GameRenderer.getPositionColorShader());}
    private static VertexBuffer upload(BufferBuilder b){var result=new VertexBuffer(VertexBuffer.Usage.STATIC);result.bind();result.upload(b.buildOrThrow());VertexBuffer.unbind();return result;}
    private static void vertex(BufferBuilder b,SpaceVector v,int color){b.addVertex((float)v.x(),(float)v.y(),(float)v.z()).setColor(color);}
    private static void tri(BufferBuilder b,SpaceVector a,SpaceVector c,SpaceVector d,int color){vertex(b,a,color);vertex(b,c,color);vertex(b,d,color);}
    private static void ensureMeshes(){if(stars!=null)return;
        var random=new Random(0x41524F554E44L);var b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        for(int i=0;i<1400;i++){double z=random.nextDouble()*2-1,a=random.nextDouble()*2*Math.PI,r=Math.sqrt(1-z*z);var n=new SpaceVector(Math.cos(a)*r,z,Math.sin(a)*r);var tangent=n.cross(Math.abs(n.y())<.9?new SpaceVector(0,1,0):new SpaceVector(1,0,0));tangent=tangent.scale(1/tangent.length());var up=n.cross(tangent);double size=.06+random.nextDouble()*.1;var center=n.scale(100);var right=tangent.scale(size);var top=up.scale(size);int v=130+random.nextInt(126),color=0xFF000000|v<<16|v<<8|v;var q0=center.subtract(right).subtract(top);var q1=center.add(right).subtract(top);var q2=center.add(right).add(top);var q3=center.subtract(right).add(top);tri(b,q0,q1,q2,color);tri(b,q0,q2,q3,color);}
        stars=upload(b);b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        var n=SpaceEnvironment.SUN_DIRECTION;var right=n.cross(new SpaceVector(0,0,1));right=right.scale(1/right.length());var up=n.cross(right);var center=n.scale(90);
        for(int i=0;i<64;i++){double a=i*2*Math.PI/64,c=(i+1)*2*Math.PI/64;var qa=right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));var qc=right.scale(Math.cos(c)).add(up.scale(Math.sin(c)));
            tri(b,center,center.add(qa.scale(1.2)),center.add(qc.scale(1.2)),0xFFFFF4D0);
            vertex(b,center.add(qa.scale(1.2)),0x60FFD480);vertex(b,center.add(qc.scale(1.2)),0x60FFD480);vertex(b,center.add(qc.scale(4)),0x00FFD480);
            vertex(b,center.add(qa.scale(1.2)),0x60FFD480);vertex(b,center.add(qc.scale(4)),0x00FFD480);vertex(b,center.add(qa.scale(4)),0x00FFD480);
        }sun=upload(b);b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){var a=normal(x,y);var c=normal(x+1,y);var d=normal(x+1,y+1);var e=normal(x,y+1);vertex(b,a,SpaceEnvironment.planetColor(a,1));vertex(b,d,SpaceEnvironment.planetColor(d,1));vertex(b,c,SpaceEnvironment.planetColor(c,1));vertex(b,a,SpaceEnvironment.planetColor(a,1));vertex(b,e,SpaceEnvironment.planetColor(e,1));vertex(b,d,SpaceEnvironment.planetColor(d,1));}planet=upload(b);
    }
    private static SpaceVector normal(int x,int y){double lon=x*2*Math.PI/128,lat=-Math.PI/2+y*Math.PI/64;return new SpaceVector(Math.sin(lon)*Math.cos(lat),Math.cos(lon)*Math.cos(lat),Math.sin(lat));}
    private SpaceSkyRenderer(){}
}

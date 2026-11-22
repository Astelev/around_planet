package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import java.util.Random;
/** Inertial stars and ephemeris-driven Sun/planet spheres; no terrain or generation access. */
@net.neoforged.fml.common.EventBusSubscriber(modid="around_planet",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class SpaceSkyRenderer {
    private static net.minecraft.client.renderer.ShaderInstance planetShader;
    @net.neoforged.bus.api.SubscribeEvent public static void shaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event)throws java.io.IOException{event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("around_planet","space_planet"),DefaultVertexFormat.POSITION_COLOR),shader->planetShader=shader);}
    static Matrix4f inverse(SpaceFrame basis){return new Matrix4f().m00((float)basis.east().x()).m01((float)basis.up().x()).m02((float)basis.south().x()).m10((float)basis.east().y()).m11((float)basis.up().y()).m12((float)basis.south().y()).m20((float)basis.east().z()).m21((float)basis.up().z()).m22((float)basis.south().z());}
    private static VertexBuffer stars,sun,planet,moon;public static long frames,planetFrames;
    public static void render(Matrix4f view,Matrix4f projection,float partial,Camera camera){
        var p=SpaceHud.current();boolean surface=net.minecraft.client.Minecraft.getInstance().level.dimension().equals(MoonSurface.dimension());if(p==null || !p.active()&&!surface || p.radius()<=0)return;
        ensureMeshes();var state=new SpaceState(new SpaceVector(p.x(),p.y(),p.z()),new SpaceVector(p.vx(),p.vy(),p.vz()));
        if(!p.returning())state=new SpaceTrajectory("around_planet:render",0,state,p.mu()).afterSeconds(SpaceHud.age(partial));
        var mc=net.minecraft.client.Minecraft.getInstance();long tick=mc.level.getGameTime();var sys=p.system();
        if(sys!=null)state=sys.global(p.primary(),state,tick);
        var cam=camera.getPosition();var observer=SpaceEnvironment.observer(state,p.frame(),new SpaceVector(cam.x,cam.y,cam.z),p.anchor());
        var basis=p.frame();var inverse=inverse(basis);var sky=new Matrix4f(view).mul(inverse);
        float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        try {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE/2);RenderSystem.setShaderFogEnd(Float.MAX_VALUE);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.depthMask(false);RenderSystem.disableDepthTest();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);draw(stars,sky,projection);
            var bodies=new java.util.ArrayList<SkyBody>();
            var earth=observer.subtract(sys==null?SpaceVector.ZERO:sys.origin(CelestialSystem.EARTH,tick).position());bodies.add(new SkyBody(CelestialSystem.EARTH,earth,sys==null?p.radius():sys.earthRadius()));
            if(sys!=null){bodies.add(new SkyBody(CelestialSystem.SUN,observer,sys.sunRadius()));if(!surface)bodies.add(new SkyBody(CelestialSystem.MOON,observer.subtract(sys.origin(CelestialSystem.MOON,tick).position()),sys.moonRadius()));}
            else{RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();draw(sun,sky,projection);}
            bodies.sort(java.util.Comparator.comparingDouble((SkyBody b)->b.observer().length()).reversed());
            for(var body:bodies){double distance=body.observer().length();if(distance<=body.radius())continue;
                if(CelestialSystem.SUN.equals(body.primary())){drawSun(sky,projection,body.observer(),body.radius());continue;}
                var center=basis.toLocal(body.observer().scale(-100/distance));var model=new Matrix4f(view).translate((float)center.x(),(float)center.y(),(float)center.z()).mul(inverse).scale((float)(100*body.radius()/distance));
                RenderSystem.disableBlend();RenderSystem.enableCull();boolean lunar=CelestialSystem.MOON.equals(body.primary());
                if(planetShader!=null){if(lunar)MoonAtlasTexture.bind(planetShader);else PlanetAtlasTexture.bind(planetShader);planetShader.safeGetUniform("PlanetSpin").set(lunar?0f:(float)PlanetDaylight.spin(mc.level,partial).angle());var light=sys==null?SpaceEnvironment.SUN_DIRECTION:sys.sunlight(body.primary(),SpaceVector.ZERO,tick);planetShader.safeGetUniform("SunDirection").set((float)light.x(),(float)light.y(),(float)light.z());RenderSystem.setShader(()->planetShader);var mesh=lunar?moon:planet;mesh.bind();mesh.drawWithShader(model,projection,planetShader);if(!lunar)AtmosphereRenderer.render(sky,projection,body.observer(),body.radius(),p.atmosphereTop(),light);}
            }
            RenderSystem.setShader(GameRenderer::getPositionColorShader);frames++;planetFrames++;
        } finally {VertexBuffer.unbind();RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);}
    }
    private record SkyBody(String primary,SpaceVector observer,double radius){}
    private static void drawSun(Matrix4f sky,Matrix4f projection,SpaceVector observer,double radius){var direction=observer.scale(-1/observer.length());var rotation=new org.joml.Quaternionf().rotationTo(0,1,0,(float)direction.x(),(float)direction.y(),(float)direction.z());float scale=(float)(90*Math.tan(Math.asin(Math.min(.9999,radius/observer.length())))/1.2);RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();draw(sun,new Matrix4f(sky).rotate(rotation).scale(scale,1,scale),projection);}

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
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){var a=normal(x,y);var c=normal(x+1,y);var d=normal(x+1,y+1);var e=normal(x,y+1);vertex(b,a,SpaceEnvironment.planetColor(a,1));vertex(b,d,SpaceEnvironment.planetColor(d,1));vertex(b,c,SpaceEnvironment.planetColor(c,1));vertex(b,a,SpaceEnvironment.planetColor(a,1));vertex(b,e,SpaceEnvironment.planetColor(e,1));vertex(b,d,SpaceEnvironment.planetColor(d,1));}planet=upload(b);b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);for(int y=0;y<64;y++)for(int x=0;x<128;x++){var a=normal(x,y);var c=normal(x+1,y);var d=normal(x+1,y+1);var e=normal(x,y+1);for(var mn:new SpaceVector[]{a,d,c,a,e,d}){int v=(int)((155+20*Math.sin(mn.x()*43)*Math.cos(mn.z()*31)));vertex(b,mn,0xff000000|v<<16|v<<8|v);}}moon=upload(b);
    }
    private static SpaceVector normal(int x,int y){double lon=x*2*Math.PI/128,lat=-Math.PI/2+y*Math.PI/64;return new SpaceVector(Math.sin(lon)*Math.cos(lat),Math.cos(lon)*Math.cos(lat),Math.sin(lat));}
    private SpaceSkyRenderer(){}
}

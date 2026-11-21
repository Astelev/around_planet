package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
/** One bounded ray/sphere image for the complete body hierarchy, with nearest-surface depth. */
public final class OrbitSphereTexture implements AutoCloseable {
    private DynamicTexture texture;private ResourceLocation id;private Key key;
    private record Projected(String primary,double x,double y,double depth,double radius,SpaceVector light){}
    private record Key(int left,int top,int width,int height,long atlas,double angle,double yaw,double pitch,long basis,List<Projected> bodies){}
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle){render(g,camera,radius,left,top,right,bottom,angle,false);}
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle,boolean moon){render(g,camera,radius,left,top,right,bottom,angle,moon?CelestialSystem.MOON:CelestialSystem.EARTH,SpaceEnvironment.SUN_DIRECTION);}
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle,String primary,SpaceVector light){render(g,camera,List.of(new OrbitSurfaces.Body(primary,SpaceVector.ZERO,radius,light)),left,top,right,bottom,angle);}
    public void render(GuiGraphics g,OrbitCamera camera,List<OrbitSurfaces.Body> bodies,int left,int top,int right,int bottom,double angle){
        int w=right-left,h=bottom-top;if(w<1||h<1)return;double factor=Math.min(1,384.0/Math.max(w,h));int iw=Math.max(1,(int)(w*factor)),ih=Math.max(1,(int)(h*factor));double scale=camera.scale(Math.max(1,Math.min(w,h))),focusDepth=camera.direction(camera.focus).depth();
        angle=bodies.stream().anyMatch(b->CelestialSystem.EARTH.equals(b.primary()))?Math.floor(angle*2048/(2*Math.PI))*2*Math.PI/2048:0;
        var projected=new ArrayList<Projected>();for(var body:bodies){var center=camera.project(body.center(),w/2.0,h/2.0,Math.max(1,Math.min(w,h)));double r=body.radius()*scale;
            if(center.x()+r<0||center.y()+r<0||center.x()-r>w||center.y()-r>h)continue;
            var light=body.sunlight();light=new SpaceVector(Math.rint(light.x()*2048)/2048,Math.rint(light.y()*2048)/2048,Math.rint(light.z()*2048)/2048);
            projected.add(new Projected(body.primary(),Math.rint(center.x()*2)/2,Math.rint(center.y()*2)/2,Math.rint((center.depth()-focusDepth)*scale*2)/2,r,light));
        }
        var next=new Key(left,top,w,h,PlanetAtlasTexture.revision,angle,camera.yaw,camera.pitch,camera.basisVersion,List.copyOf(projected));
        if(!next.equals(key)){
            if(texture==null||texture.getPixels().getWidth()!=iw||texture.getPixels().getHeight()!=ih){close();texture=new DynamicTexture(new NativeImage(iw,ih,true));id=Minecraft.getInstance().getTextureManager().register("around_planet_orbit_sphere",texture);}
            var image=texture.getPixels();var spin=new PlanetSpin(angle,PlanetSpin.OMEGA);var vx=camera.fromView(1,0,0);var vy=camera.fromView(0,1,0);var vz=camera.fromView(0,0,1);
            for(int y=0;y<ih;y++)for(int x=0;x<iw;x++){
                double px=(x+.5)*w/iw,py=(y+.5)*h/ih,nearest=Double.NEGATIVE_INFINITY,nx=0,ny=0,nz=0;Projected hit=null;
                for(var body:projected){double xx=(px-body.x())/body.radius(),yy=-(py-body.y())/body.radius(),d=1-xx*xx-yy*yy;if(d<0)continue;double zz=Math.sqrt(d),depth=body.depth()+zz*body.radius();if(depth>nearest){nearest=depth;hit=body;nx=xx;ny=yy;nz=zz;}}
                int color=0;if(hit!=null){var inertial=new SpaceVector(nx*vx.x()+ny*vy.x()+nz*vz.x(),nx*vx.y()+ny*vy.y()+nz*vz.y(),nx*vx.z()+ny*vy.z()+nz*vz.z());color=color(hit,inertial,nz,spin);}image.setPixelRGBA(x,y,color);
            }
            texture.upload();key=next;
        }
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(id,left,top,w,h,0,0,texture.getPixels().getWidth(),texture.getPixels().getHeight(),texture.getPixels().getWidth(),texture.getPixels().getHeight());com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    private static int color(Projected body,SpaceVector inertial,double nz,PlanetSpin spin){
        if(CelestialSystem.SUN.equals(body.primary())){int green=(int)(190+45*nz),blue=(int)(80+80*nz);return 0xff000000|blue<<16|green<<8|255;}
        if(CelestialSystem.MOON.equals(body.primary())){double light=.15+.85*Math.max(0,inertial.dot(body.light()));int v=(int)((155+22*Math.sin(inertial.x()*43)*Math.cos(inertial.z()*31))*light);return 0xff000000|v<<16|v<<8|v;}
        var normal=spin.unrotate(inertial);if(!PlanetAtlasTexture.ready())return SphereGrid.color(normal.x(),normal.y(),normal.z(),inertial.dot(body.light()));
        int rgb=PlanetAtlasTexture.sample(normal.x(),normal.y(),normal.z());double light=.4+.6*Math.max(0,inertial.dot(body.light()));int red=(int)(((rgb>>>16)&255)*light),green=(int)(((rgb>>>8)&255)*light),blue=(int)((rgb&255)*light);return 0xff000000|blue<<16|green<<8|red;
    }
    @Override public void close(){if(id!=null)Minecraft.getInstance().getTextureManager().release(id);id=null;texture=null;key=null;}
}

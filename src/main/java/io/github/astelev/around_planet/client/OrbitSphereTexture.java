package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
/** Cached bounded ray/sphere image. Camera motion changes planet-fixed checks; orbit samples don't rebuild it. */
public final class OrbitSphereTexture implements AutoCloseable {
    private DynamicTexture texture;private ResourceLocation id;private double[] key;
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle){render(g,camera,radius,left,top,right,bottom,angle,false);}
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle,boolean moon){
        int w=right-left,h=bottom-top;if(w<1 || h<1)return;double factor=Math.min(1,384.0/Math.max(w,h));int iw=Math.max(1,(int)(w*factor)),ih=Math.max(1,(int)(h*factor));
        if(moon)angle=0;angle=Math.floor(angle*2048/(2*Math.PI))*2*Math.PI/2048;var spin=new PlanetSpin(angle,PlanetSpin.OMEGA);
        var center=camera.project(SpaceVector.ZERO,w/2.0,h/2.0,Math.max(1,Math.min(w,h)));double cx=Math.rint(center.x()*2)/2,cy=Math.rint(center.y()*2)/2;
        double[] next={moon?1:0,PlanetAtlasTexture.revision,angle,camera.yaw,camera.pitch,camera.zoom,cx,cy,camera.basisVersion,radius,left,top,right,bottom};
        if(!Arrays.equals(key,next)){
            if(texture==null || texture.getPixels().getWidth()!=iw || texture.getPixels().getHeight()!=ih){close();texture=new DynamicTexture(new NativeImage(iw,ih,true));id=Minecraft.getInstance().getTextureManager().register("around_planet_orbit_sphere",texture);}
            var image=texture.getPixels();double r=radius*camera.scale(Math.max(1,Math.min(w,h)));
            var vx=camera.fromView(1,0,0);var vy=camera.fromView(0,1,0);var vz=camera.fromView(0,0,1);
            for(int y=0;y<ih;y++)for(int x=0;x<iw;x++){
                double nx=((x+.5)*w/iw-cx)/r,ny=-((y+.5)*h/ih-cy)/r,d=1-nx*nx-ny*ny;int color=0;
                if(d>=0){double nz=Math.sqrt(d);var normal=spin.unrotate(new SpaceVector(nx*vx.x()+ny*vy.x()+nz*vz.x(),nx*vx.y()+ny*vy.y()+nz*vz.y(),nx*vx.z()+ny*vy.z()+nz*vz.z()));if(moon){var inertial=spin.rotate(normal);double light=.15+.85*Math.max(0,inertial.dot(SpaceEnvironment.SUN_DIRECTION));int v=(int)((155+22*Math.sin(normal.x()*43)*Math.cos(normal.z()*31))*light);color=0xff000000|v<<16|v<<8|v;}else if(PlanetAtlasTexture.ready()){int rgb=PlanetAtlasTexture.sample(normal.x(),normal.y(),normal.z());var inertial=spin.rotate(normal);double light=.4+.6*Math.max(0,inertial.dot(SpaceEnvironment.SUN_DIRECTION));int red=(int)(((rgb>>>16)&255)*light),gg=(int)(((rgb>>>8)&255)*light),b=(int)((rgb&255)*light);color=0xff000000|(b<<16)|(gg<<8)|red;}else color=SphereGrid.color(normal.x(),normal.y(),normal.z());}image.setPixelRGBA(x,y,color);
            }
            texture.upload();key=next;
        }
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(id,left,top,w,h,0,0,texture.getPixels().getWidth(),texture.getPixels().getHeight(),texture.getPixels().getWidth(),texture.getPixels().getHeight());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override public void close(){if(id!=null)Minecraft.getInstance().getTextureManager().release(id);id=null;texture=null;key=null;}
}

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
    public void render(GuiGraphics g,OrbitCamera camera,double radius,int left,int top,int right,int bottom,double angle){
        int w=right-left,h=bottom-top;if(w<1 || h<1)return;double factor=Math.min(1,384.0/Math.max(w,h));int iw=Math.max(1,(int)(w*factor)),ih=Math.max(1,(int)(h*factor));
        angle=Math.floor(angle*2048/(2*Math.PI))*2*Math.PI/2048;var spin=new PlanetSpin(angle,PlanetSpin.OMEGA);
        double[] next={angle,camera.yaw,camera.pitch,camera.zoom,camera.panX,camera.panY,camera.basisVersion,radius,left,top,right,bottom};
        if(!Arrays.equals(key,next)){
            if(texture==null || texture.getPixels().getWidth()!=iw || texture.getPixels().getHeight()!=ih){close();texture=new DynamicTexture(new NativeImage(iw,ih,true));id=Minecraft.getInstance().getTextureManager().register("around_planet_orbit_sphere",texture);}
            var image=texture.getPixels();double r=radius*camera.scale(Math.max(1,Math.min(w,h))),cx=w/2.0+camera.panX,cy=h/2.0+camera.panY;
            var vx=camera.fromView(1,0,0);var vy=camera.fromView(0,1,0);var vz=camera.fromView(0,0,1);
            for(int y=0;y<ih;y++)for(int x=0;x<iw;x++){
                double nx=((x+.5)*w/iw-cx)/r,ny=-((y+.5)*h/ih-cy)/r,d=1-nx*nx-ny*ny;int color=0;
                if(d>=0){double nz=Math.sqrt(d);var normal=spin.unrotate(new SpaceVector(nx*vx.x()+ny*vy.x()+nz*vz.x(),nx*vx.y()+ny*vy.y()+nz*vz.y(),nx*vx.z()+ny*vy.z()+nz*vz.z()));color=SphereGrid.color(normal.x(),normal.y(),normal.z());}image.setPixelRGBA(x,y,color);
            }
            texture.upload();key=next;
        }
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(id,left,top,w,h,0,0,texture.getPixels().getWidth(),texture.getPixels().getHeight(),texture.getPixels().getWidth(),texture.getPixels().getHeight());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override public void close(){if(id!=null)Minecraft.getInstance().getTextureManager().release(id);id=null;texture=null;key=null;}
}

package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.SpaceStatusPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.mojang.math.Axis;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Live orthographic navigation view; never pauses native physics or server time. */
public final class OrbitMapScreen extends Screen {
    private final OrbitCamera camera=new OrbitCamera();private SpaceStatusPayload sampled;private OrbitPlot plot;private boolean fitted;
    private int left=8,top=64,right,bottom;private static final int FUTURE=0xFF69E4DB;
    public OrbitMapScreen(){super(Component.translatable("around_planet.map.title"));}
    @Override protected void init(){right=width-8;bottom=height-45;addRenderableWidget(Button.builder(Component.translatable("around_planet.map.reset"),b->reset()).bounds(width-79,8,70,20).build());}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){if(minecraft.player==null || !SpaceCells.contains(minecraft.player))onClose();}
    private void update(){var sample=SpaceHud.current();if(sample==null || !sample.active() || sample.radius()<=0)return;
        if(sample!=sampled){sampled=sample;plot=OrbitPlot.create(state(),sample.mu(),sample.radius(),sample.returnAltitude(),sample.returning());if(!fitted){reset();fitted=true;}}
    }
    private SpaceState state(){return new SpaceState(new SpaceVector(sampled.x(),sampled.y(),sampled.z()),new SpaceVector(sampled.vx(),sampled.vy(),sampled.vz()));}
    private void reset(){if(sampled!=null && plot!=null)camera.fit(state(),plot.extent(sampled.radius())*1.12);}
    private double size(){return Math.max(1,Math.min(right-left,bottom-top));}
    private OrbitCamera.Point project(SpaceVector p){return camera.project(p,(left+right)/2.0,(top+bottom)/2.0,size());}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        update();g.fill(0,0,width,height,0xFF070D19);g.drawString(font,title,12,13,0xFFF0F5FF,true);
        g.drawWordWrap(font,Component.translatable("around_planet.map.controls"),12,height-37,width-24,0xFFBAC9DC);
        g.drawString(font,Component.translatable("around_planet.map.legend"),12,height-16,0xFF69E4DB,false);
        if(sampled==null || plot==null){g.drawCenteredString(font,Component.translatable("around_planet.map.waiting"),width/2,height/2,0xFFBAC9DC);super.render(g,mx,my,partial);return;}
        double speed=state().velocity().length();String stats=String.format(Locale.ROOT,"h %.0f m    v %.2f m/s",sampled.altitude(),speed);
        g.drawString(font,stats,12,35,0xFFD5E2F5,false);
        String peri=plot.periapsis()==null?"—":String.format(Locale.ROOT,"%.0f m",plot.periapsis().length()-sampled.radius());
        String apo=plot.apoapsis()==null?"—":String.format(Locale.ROOT,"%.0f m",plot.apoapsis().length()-sampled.radius());
        g.drawString(font,Component.translatable("around_planet.map.extrema",peri,apo),12,48,0xFFBAC9DC,false);
        g.enableScissor(left,top,right,bottom);g.fill(left,top,right,bottom,0xFF0B1423);
        double cx=(left+right)/2.0+camera.panX,cy=(top+bottom)/2.0+camera.panY,radius=sampled.radius()*camera.scale(size());
        for(int i=1;i<=4;i++)circle(g,cx,cy,radius*i,0xFF19283C,false);
        line(g,left,cy,right,cy,0xFF19283C);line(g,cx,top,cx,bottom,0xFF19283C);
        arcs(g,true);circle(g,cx,cy,radius,0xFFF5F6FA,true);arcs(g,false);
        boolean circular=plot.periapsis()!=null && plot.apoapsis()!=null && Math.abs(plot.periapsis().length()-plot.apoapsis().length())<1e-6*plot.apoapsis().length();
        if(!circular){marker(g,plot.periapsis(),0xFFFF9CD1,"Pe");marker(g,plot.apoapsis(),0xFFAAA6FF,"Ap");}
        if(plot.reentry() && plot.points().size()>1)marker(g,plot.points().getLast(),0xFFFFAE68,Component.translatable("around_planet.map.entry").getString());
        var current=project(state().position());g.fill((int)current.x()-4,(int)current.y()-4,(int)current.x()+5,(int)current.y()+5,0xFF101626);g.fill((int)current.x()-2,(int)current.y()-2,(int)current.x()+3,(int)current.y()+3,0xFFFFD85C);
        if(speed>1e-8){var arrow=project(state().position().add(state().velocity().scale(12/(camera.scale(size())*speed))));line(g,current.x(),current.y(),arrow.x(),arrow.y(),0xFFFFD85C);}
        g.disableScissor();if(sampled.returning())g.drawCenteredString(font,Component.translatable("around_planet.space.returning"),width/2,top+9,0xFFFFAE68);
        super.render(g,mx,my,partial);
    }
    private void arcs(GuiGraphics g,boolean behind){var points=plot.points();for(int i=1;i<points.size();i++){var a=project(points.get(i-1));var b=project(points.get(i));if(((a.depth()+b.depth())/2<0)==behind)line(g,a.x(),a.y(),b.x(),b.y(),behind?0xFF336A78:FUTURE);}}
    private void marker(GuiGraphics g,SpaceVector v,int color,String text){if(v==null)return;var p=project(v);if(p.x()<left || p.x()>right || p.y()<top || p.y()>bottom)return;
        double scale=camera.scale(size()),x=p.x()-(left+right)/2.0-camera.panX,y=p.y()-(top+bottom)/2.0-camera.panY;
        if(p.depth()<0 && x*x+y*y<Math.pow(sampled.radius()*scale,2))return;
        g.fill((int)p.x()-2,(int)p.y()-2,(int)p.x()+3,(int)p.y()+3,color);g.drawString(font,text,(int)p.x()+6,(int)p.y()-4,color,true);
    }
    private void circle(GuiGraphics g,double cx,double cy,double r,int color,boolean solid){if(r<.5 || !Double.isFinite(r))return;
        if(solid){for(int y=Math.max(top,(int)Math.ceil(cy-r));y<Math.min(bottom,(int)Math.floor(cy+r)+1);y++){double d=y-cy,dx=Math.sqrt(Math.max(0,r*r-d*d));g.fill(Math.max(left,(int)Math.ceil(cx-dx)),y,Math.min(right,(int)Math.floor(cx+dx)+1),y+1,color);}}
        else for(int i=0;i<128;i++){double a=i*2*Math.PI/128,b=(i+1)*2*Math.PI/128;line(g,cx+Math.cos(a)*r,cy+Math.sin(a)*r,cx+Math.cos(b)*r,cy+Math.sin(b)*r,color);}
    }
    private void line(GuiGraphics g,double ax,double ay,double bx,double by,int color){var c=OrbitCamera.clip(ax,ay,bx,by,left,top,right,bottom);if(c==null)return;double dx=c[2]-c[0],dy=c[3]-c[1],length=Math.hypot(dx,dy);if(length<.1)return;
        var pose=g.pose();pose.pushPose();pose.translate(c[0],c[1],0);pose.mulPose(Axis.ZP.rotation((float)Math.atan2(dy,dx)));g.fill(0,0,(int)Math.ceil(length),1,color);pose.popPose();}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0){camera.rotate(dx,dy);return true;}if(button==1 || button==2){camera.panX=Math.max(-width*20,Math.min(width*20,camera.panX+dx));camera.panY=Math.max(-height*20,Math.min(height*20,camera.panY+dy));return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){camera.zoom(vertical);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(OrbitMapKeys.MAP.matches(key,scan)){onClose();return true;}if(key==GLFW.GLFW_KEY_R){reset();return true;}if(key==GLFW.GLFW_KEY_EQUAL || key==GLFW.GLFW_KEY_KP_ADD){camera.zoom(1);return true;}if(key==GLFW.GLFW_KEY_MINUS || key==GLFW.GLFW_KEY_KP_SUBTRACT){camera.zoom(-1);return true;}return super.keyPressed(key,scan,modifiers);}
}

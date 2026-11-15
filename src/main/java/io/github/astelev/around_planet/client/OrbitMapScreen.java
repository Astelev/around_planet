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
    private record LabelBox(int x,int y,int width,int height){boolean intersects(LabelBox b){return x<b.x+b.width&&x+width>b.x&&y<b.y+b.height&&y+height>b.y;}}
    private final List<LabelBox> labels=new ArrayList<>();
    private record View(OrbitCamera camera,boolean follow,String primary){}
    private static final Map<net.minecraft.client.multiplayer.ClientLevel,Map<UUID,View>> VIEWS=new WeakHashMap<>();
    private final LunarMapLayer lunar=new LunarMapLayer(this);
    String viewPrimary(){return lunar.primary();}
    double viewRadius(){return lunar.enabled()?lunar.system().radius(viewPrimary()):sampled==null?1:sampled.radius();}
    double viewEntry(){return lunar.moon()?0:lunar.enabled()?lunar.system().earthEntry():sampled==null?0:sampled.returnAltitude();}
    void reframe(){fitted=false;sampled=null;}
    private final RendezvousMapLayer rendezvous=new RendezvousMapLayer(this);
    private UUID focusId(){var r=RendezvousClient.current();var p=ManeuverClient.current();return r!=null?r.focus():p==null?null:p.focus();}
    private final ManeuverMapLayer maneuvers;private double pressX,pressY;private boolean mapPressed;
    private final OrbitSphereTexture sphere=new OrbitSphereTexture();
    private record Other(io.github.astelev.around_planet.network.SpaceMapPayload.Body body,OrbitPlot plot,SpaceVector position,int color){}
    private java.util.List<Other> others=java.util.List.of();private io.github.astelev.around_planet.network.SpaceMapPayload catalog;private String catalogChart;private CelestialSystem catalogSystem;
    private final OrbitCamera camera=new OrbitCamera();private SpaceStatusPayload sampled,sampledForPlot;private OrbitPlot plot;private boolean fitted;private boolean follow;private Button followButton;
    private int left=8,top=32,right,bottom;private static final int FUTURE=0xFF69E4DB;
    public OrbitMapScreen(){this(null);}
    public OrbitMapScreen(UUID session){super(Component.translatable(session==null?"around_planet.map.title":"around_planet.maneuver.title"));maneuvers=new ManeuverMapLayer(this,session);}
    @Override protected void init(){right=width-(maneuvers.editor()?190:8);bottom=height-45;for(var widget:maneuvers.init(width-179,32,175))addRenderableWidget(widget);if(maneuvers.editor()){addRenderableWidget(rendezvous.init(width-179,height-23,171));addRenderableWidget(rendezvous.clearButton());}SpaceMapCatalog.subscribe(true);addRenderableWidget(lunar.init());followButton=addRenderableWidget(Button.builder(followLabel(),b->{follow=!follow;if(follow&&sampled!=null)camera.focus=state().position();b.setMessage(followLabel());}).bounds(width-187,8,104,20).build());addRenderableWidget(Button.builder(Component.translatable("around_planet.map.reset"),b->reset()).bounds(width-79,8,70,20).build());}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){if(minecraft.player==null || !SpaceCells.contains(minecraft.player)){onClose();return;}if(minecraft.level.getGameTime()%40==0)SpaceMapCatalog.subscribe(true);}
    @Override public void removed(){if(fitted&&minecraft.level!=null&&focusId()!=null)VIEWS.computeIfAbsent(minecraft.level,k->new HashMap<>()).put(focusId(),new View(camera.copy(),follow,viewPrimary()));sphere.close();SpaceMapCatalog.subscribe(false);super.removed();}
    private void update(){var sample=SpaceHud.current();if(sample==null || !sample.active() || sample.radius()<=0)return;
        lunar.update();sampled=sample;maneuvers.update(viewRadius(),viewEntry());if(sample!=sampledForPlot||!fitted){sampledForPlot=sample;var path=maneuvers.pathForView();plot=OrbitPlot.create(focusState(),path==null?(lunar.enabled()?lunar.system().mu(viewPrimary()):sample.mu()):path.mu(),viewRadius(),viewEntry(),sample.returning());if(!fitted){var id=focusId();var saved=id==null?null:VIEWS.getOrDefault(minecraft.level,Map.of()).get(id);if(saved==null||!saved.primary().equals(viewPrimary()))reset();else{camera.set(saved.camera());follow=saved.follow();if(followButton!=null)followButton.setMessage(followLabel());}fitted=true;}}
        if(follow)camera.focus=state().position();
        rendezvous.update(viewRadius(),viewEntry());
        var next=SpaceMapCatalog.current();if(next!=catalog||!Objects.equals(catalogChart,viewPrimary())||!Objects.equals(catalogSystem,lunar.system())){catalog=next;catalogChart=viewPrimary();catalogSystem=lunar.system();var list=new ArrayList<Other>();if(next!=null)for(var body:next.bodies()){if(CelestialSystem.body(body.id())||!OrbitChart.visible(body.primary(),viewPrimary()))continue;
            var state=new SpaceState(new SpaceVector(body.x(),body.y(),body.z()),new SpaceVector(body.vx(),body.vy(),body.vz()));
            int[] colors={0xFFB59AFF,0xFFFFB775,0xFF92D98C,0xFFFF8BAF,0xFF80BFFF};int color=colors[Math.floorMod(body.id().hashCode(),colors.length)];
            var offset=OrbitChart.offset(catalogSystem,body.primary(),viewPrimary(),next.now());double bodyRadius=catalogSystem!=null&&catalogSystem.managed(body.primary())?catalogSystem.radius(body.primary()):viewRadius();var shape=OrbitPlot.create(state,body.mu(),bodyRadius,body.returnAltitude(),body.waiting());list.add(new Other(body,OrbitChart.translate(shape,offset),state.position().add(offset),color));}others=List.copyOf(list);}
    }
    private SpaceState state(){var state=new SpaceState(new SpaceVector(sampled.x(),sampled.y(),sampled.z()),new SpaceVector(sampled.vx(),sampled.vy(),sampled.vz()));if(!sampled.returning())state=new SpaceTrajectory("map",0,state,sampled.mu()).afterSeconds(SpaceHud.age(0));if(lunar.enabled()&&!sampled.primary().equals(viewPrimary()))return lunar.system().relative(viewPrimary(),lunar.system().global(sampled.primary(),state,ManeuverClient.now()),ManeuverClient.now());return state;}
    private SpaceState focusState(){if(lunar.enabled()&&!sampled.primary().equals(viewPrimary())){var future=maneuvers.firstForView();if(future!=null)return future;}return state();}
    private Component followLabel(){return Component.translatable(follow?"around_planet.map.follow_on":"around_planet.map.follow_off");}
    private void reset(){follow=false;if(followButton!=null)followButton.setMessage(followLabel());if(sampled!=null&&plot!=null){var focusState=focusState();var focusPlot=plot;var planned=ManeuverClient.current();if(maneuvers.authorized()&&planned!=null&&planned.trajectory().primary().equals(viewPrimary())){focusState=planned.trajectory().at(ManeuverClient.now());focusPlot=OrbitPlot.create(focusState,planned.trajectory().mu(),viewRadius(),sampled.returnAltitude(),sampled.returning());}double extent=Math.min(focusPlot.extent(viewRadius()),Math.max(viewRadius()*8,focusState.position().length()*2));camera.fit(focusState,extent*1.12);if(maneuvers.editor())camera.pitch=.45;}}

    private double size(){return Math.max(1,Math.min(right-left,bottom-top));}
    boolean planAt(long tick){return maneuvers.at(tick);}
    OrbitCamera.Point project(SpaceVector p){return camera.project(p,(left+right)/2.0,(top+bottom)/2.0,size());}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        labels.clear();update();g.fill(0,0,width,height,0xFF070D19);g.drawString(font,title,12,13,0xFFF0F5FF,true);
        g.drawWordWrap(font,Component.translatable("around_planet.map.controls"),12,height-37,maneuvers.editor()?right-left:width-24,0xFFBAC9DC);
        g.drawString(font,rendezvous.active()?Component.translatable("around_planet.rendezvous.legend",Component.literal("■").withStyle(net.minecraft.ChatFormatting.YELLOW),Component.literal("■").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),Component.literal("■").withStyle(net.minecraft.ChatFormatting.GOLD)):Component.translatable(maneuvers.hasPlan()?"around_planet.maneuver.legend":"around_planet.map.legend"),12,height-16,0xFF69E4DB,false);
        if(sampled==null || plot==null){g.drawCenteredString(font,Component.translatable("around_planet.map.waiting"),width/2,height/2,0xFFBAC9DC);super.render(g,mx,my,partial);return;}
        double speed=state().velocity().length();
        g.enableScissor(left,top,right,bottom);g.fill(left,top,right,bottom,0xFF0B1423);
        var planetCenter=project(SpaceVector.ZERO);double cx=planetCenter.x(),cy=planetCenter.y(),radius=viewRadius()*camera.scale(size());
        g.drawManaged(()->{for(int i=1;i<=4;i++)circle(g,cx,cy,radius*i,0xFF19283C,false);
            globalAxes(g,true);
            for(var other:others)if(!rendezvous.isTarget(other.body.id()))arcs(g,other.plot,other.color,true,3);rendezvous.paths(g,true);if(!lunar.enabled())arcs(g,true);maneuvers.paths(g,true);
        });
        sphere.render(g,camera,viewRadius(),left,top,right,bottom,PlanetDaylight.spin(minecraft.level,partial).angle(),lunar.moon());
        g.drawManaged(()->{globalAxes(g,false);for(var other:others)if(!rendezvous.isTarget(other.body.id()))arcs(g,other.plot,other.color,false,3);rendezvous.paths(g,false);if(!lunar.enabled())arcs(g,false);maneuvers.paths(g,false);
            for(var other:others){if(rendezvous.isTarget(other.body.id()))continue;var p=other.position;var screen=project(p);boolean hover=Math.hypot(screen.x()-mx,screen.y()-my)<8;
                marker(g,p,other.color,hover?other.body.name()+(other.body.loaded()?"":" · "+Component.translatable("around_planet.map.dormant").getString()):"");}
        });
        if(plot.reentry() && plot.points().size()>1)marker(g,plot.points().getLast(),0xFFFFAE68,Component.translatable("around_planet.map.entry").getString());
        var current=project(state().position());g.fill((int)current.x()-4,(int)current.y()-4,(int)current.x()+5,(int)current.y()+5,0xFF101626);g.fill((int)current.x()-2,(int)current.y()-2,(int)current.x()+3,(int)current.y()+3,0xFFFFD85C);
        if(speed>1e-8){var arrow=project(state().position().add(state().velocity().scale(12/(camera.scale(size())*speed))));line(g,current.x(),current.y(),arrow.x(),arrow.y(),0xFFFFD85C);}
        rendezvous.markers(g);maneuvers.markers(g,mx,my);lunar.render(g,maneuvers.preview());axisCompass(g);g.disableScissor();maneuvers.panel(g);rendezvous.panel(g,left,top,right,bottom);if(sampled.returning())g.drawCenteredString(font,Component.translatable("around_planet.space.returning"),width/2,top+9,0xFFFFAE68);
        super.render(g,mx,my,partial);if(!maneuvers.tooltip(g,mx,my,true)&&!lunar.tooltip(g,mx,my,maneuvers.preview())&&!rendezvous.tooltip(g,mx,my))maneuvers.tooltip(g,mx,my);
    }
    /** Signed inertial axes start at the planet surface so the opaque globe occludes their inner halves. */
    private void globalAxes(GuiGraphics g,boolean behind){
        double length=Math.max(viewRadius()*1.45,.44*size()/camera.scale(size()));
        for(var axis:OrbitAxes.values())for(int sign:new int[]{-1,1}){
            var start=project(axis.vector.scale(sign*viewRadius()));var end=project(axis.vector.scale(sign*length));
            if((end.depth()<0)!=behind)continue;
            int color=sign>0?axis.color:axis.dim();var clipped=OrbitCamera.clip(start.x(),start.y(),end.x(),end.y(),left+5,top+6,right-18,bottom-12);if(clipped==null)continue;
            if(sign>0){line(g,clipped[0],clipped[1],clipped[2],clipped[3],color);arrowHead(g,clipped[0],clipped[1],clipped[2],clipped[3],color);}
            else dashed(g,clipped[0],clipped[1],clipped[2],clipped[3],color);
            g.drawString(font,(sign>0?"+":"−")+axis.name(),(int)clipped[2]+3,(int)clipped[3]-4,color,true);
        }
    }
    private void dashed(GuiGraphics g,double ax,double ay,double bx,double by,int color){double length=Math.hypot(bx-ax,by-ay);if(length<.1)return;for(double t=0;t<length;t+=8){double a=t/length,b=Math.min(length,t+4)/length;line(g,ax+(bx-ax)*a,ay+(by-ay)*a,ax+(bx-ax)*b,ay+(by-ay)*b,color);}}
    private void arrowHead(GuiGraphics g,double ax,double ay,double bx,double by,int color){double dx=bx-ax,dy=by-ay,length=Math.hypot(dx,dy);if(length<3)return;dx/=length;dy/=length;line(g,bx,by,bx-dx*5-dy*2.5,by-dy*5+dx*2.5,color);line(g,bx,by,bx-dx*5+dy*2.5,by-dy*5-dx*2.5,color);}
    /** Always visible when the scene origin or an axis tip has been panned offscreen. */
    private void axisCompass(GuiGraphics g){
        boolean compact=rendezvous.active()&&right-left<340;int x=left+(compact?4:8),y=bottom-(compact?53:66);g.fill(x,y,x+(compact?60:91),y+(compact?50:62),0xDD07101D);if(!compact)g.drawString(font,Component.translatable("around_planet.map.global_axes"),x+5,y+4,0xFFBAC9DC,false);
        double cx=x+(compact?23:44),cy=y+(compact?24:39),arm=compact?13:19,back=compact?10:15,label=compact?16:24;
        g.drawManaged(()->{for(var axis:OrbitAxes.values()){
            var d=camera.direction(axis.vector);double ex=cx+d.x()*arm,ey=cy+d.y()*arm;int color=d.depth()>=0?axis.color:axis.dim();
            dashed(g,cx,cy,cx-d.x()*back,cy-d.y()*back,axis.dim());line(g,cx,cy,ex,ey,color);arrowHead(g,cx,cy,ex,ey,color);
            if(Math.hypot(d.x(),d.y())<.15){g.fill((int)ex-2,(int)ey-2,(int)ex+3,(int)ey+3,color);if(d.depth()<0){g.fill((int)ex-1,(int)ey-1,(int)ex+2,(int)ey+2,0xFF07101D);}}
            g.drawString(font,"+"+axis.name(),(int)(cx+d.x()*label)+3,(int)(cy+d.y()*label)-4,color,true);
        }});
    }
    private void arcs(GuiGraphics g,boolean behind){arcs(g,plot,FUTURE,behind,1);}
    private void arcs(GuiGraphics g,OrbitPlot curve,int color,boolean behind,int stride){var points=curve.points();int dim=0xFF000000|((color&0xFEFEFE)>>>1);for(int i=0;i<points.size()-1;i+=stride)orbitLine(g,points.get(i),points.get(Math.min(points.size()-1,i+stride)),behind?dim:color,behind);}
    void orbitLine(GuiGraphics g,SpaceVector a,SpaceVector b,int color,boolean behind){var aa=project(a);var bb=project(b);for(var r:OrbitOcclusion.ranges(camera.direction(a),camera.direction(b),viewRadius(),behind))line(g,aa.x()+(bb.x()-aa.x())*r.from(),aa.y()+(bb.y()-aa.y())*r.from(),aa.x()+(bb.x()-aa.x())*r.until(),aa.y()+(bb.y()-aa.y())*r.until(),color);}
    void marker(GuiGraphics g,SpaceVector v,int color,String text){if(v==null)return;var p=project(v);if(p.x()<left || p.x()>right || p.y()<top || p.y()>bottom)return;
        var center=project(SpaceVector.ZERO);double scale=camera.scale(size()),x=p.x()-center.x(),y=p.y()-center.y();
        if(!OrbitOcclusion.visible(camera.direction(v),viewRadius()))return;
        g.fill((int)p.x()-2,(int)p.y()-2,(int)p.x()+3,(int)p.y()+3,color);if(!text.isEmpty()){var shown=font.plainSubstrByWidth(text,Math.max(20,right-left-12));int w=font.width(shown),labelX=Math.clamp((int)p.x()+6,left+3,Math.max(left+3,right-w-3)),labelY=Math.clamp((int)p.y()-4,top+2,bottom-10);var box=new LabelBox(labelX-2,labelY-1,w+4,11);if(labels.stream().noneMatch(box::intersects)){labels.add(box);g.fill(labelX-2,labelY-1,labelX+w+2,labelY+10,0xbb07101d);g.drawString(font,shown,labelX,labelY,color,true);}}
    }
    private void circle(GuiGraphics g,double cx,double cy,double r,int color,boolean solid){if(r<.5 || !Double.isFinite(r))return;
        if(solid){for(int y=Math.max(top,(int)Math.ceil(cy-r));y<Math.min(bottom,(int)Math.floor(cy+r)+1);y++){double d=y-cy,dx=Math.sqrt(Math.max(0,r*r-d*d));g.fill(Math.max(left,(int)Math.ceil(cx-dx)),y,Math.min(right,(int)Math.floor(cx+dx)+1),y+1,color);}}
        else for(int i=0;i<128;i++){double a=i*2*Math.PI/128,b=(i+1)*2*Math.PI/128;line(g,cx+Math.cos(a)*r,cy+Math.sin(a)*r,cx+Math.cos(b)*r,cy+Math.sin(b)*r,color);}
    }
    void line(GuiGraphics g,double ax,double ay,double bx,double by,int color){var c=OrbitCamera.clip(ax,ay,bx,by,left,top,right,bottom);if(c==null)return;double dx=c[2]-c[0],dy=c[3]-c[1],length=Math.hypot(dx,dy);if(length<.1)return;
        var pose=g.pose();pose.pushPose();pose.translate(c[0],c[1],0);pose.mulPose(Axis.ZP.rotation((float)Math.atan2(dy,dx)));g.fill(0,0,(int)Math.ceil(length),1,color);pose.popPose();}
    boolean visible(SpaceVector v){var p=project(v);return p.x()>=left&&p.x()<=right&&p.y()>=top&&p.y()<=bottom&&OrbitOcclusion.visible(camera.direction(v),viewRadius());}
    @Override public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(button==0&&x>=left&&x<=right&&y>=top&&y<=bottom){if(getFocused()!=null)getFocused().setFocused(false);setFocused(null);pressX=x;pressY=y;mapPressed=true;return true;}return false;}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&mapPressed){mapPressed=false;if(Math.hypot(x-pressX,y-pressY)<4){if(!maneuvers.clickPriority(x,y)&&!lunar.select(x,y,maneuvers.preview())&&!rendezvous.select(x,y))maneuvers.click(x,y);}return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&mapPressed){camera.rotate(dx,dy);return true;}if((button==1 || button==2)&&x>=left&&x<=right&&y>=top&&y<=bottom){follow=false;if(followButton!=null)followButton.setMessage(followLabel());camera.pan(dx,dy,size());return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x>=left&&x<=right&&y>=top&&y<=bottom){camera.zoom(vertical);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(getFocused() instanceof net.minecraft.client.gui.components.EditBox edit&&edit.isFocused()){return super.keyPressed(key,scan,modifiers);}if(OrbitMapKeys.MAP.matches(key,scan)){onClose();return true;}if(key==GLFW.GLFW_KEY_R){reset();return true;}if(key==GLFW.GLFW_KEY_EQUAL || key==GLFW.GLFW_KEY_KP_ADD){camera.zoom(1);return true;}if(key==GLFW.GLFW_KEY_MINUS || key==GLFW.GLFW_KEY_KP_SUBTRACT){camera.zoom(-1);return true;}return super.keyPressed(key,scan,modifiers);}
}

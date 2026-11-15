package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.*;
/** A frozen primary chart: future lunar conics stay centered on the Moon, not smeared along its orbit. */
final class LunarMapLayer {
    private final OrbitMapScreen map;private Button view;private boolean moon;private String actual;
    LunarMapLayer(OrbitMapScreen map){this.map=map;}
    Button init(){view=Button.builder(Component.translatable("around_planet.moon.view_earth"),b->{moon=!moon;map.reframe();}).bounds(map.width-300,8,108,20).build();return view;}
    CelestialSystem system(){var p=ManeuverClient.current();return p==null?null:p.system();}
    boolean enabled(){var p=ManeuverClient.current();return p!=null&&system()!=null&&system().managed(p.trajectory().primary());}
    String primary(){return enabled()&&moon?CelestialSystem.MOON:enabled()?CelestialSystem.EARTH:ManeuverClient.current()==null?"":ManeuverClient.current().trajectory().primary();}
    boolean sameChart(){var p=ManeuverClient.current();return !enabled()||p!=null&&p.trajectory().primary().equals(primary());}
    boolean moon(){return enabled()&&moon;}
    void update(){var p=ManeuverClient.current();if(enabled()&&!Objects.equals(actual,p.trajectory().primary())){boolean changed=actual!=null;actual=p.trajectory().primary();moon=CelestialSystem.MOON.equals(actual);if(changed)map.reframe();}if(view!=null){view.visible=enabled();view.active=enabled();view.setMessage(Component.translatable(moon?"around_planet.moon.view_moon":"around_planet.moon.view_earth"));}}
    void render(GuiGraphics g,ManeuverPreview preview){if(!enabled())return;var sys=system();long now=ManeuverClient.now();if(!moon){for(int i=1;i<=128;i++){var a=map.project(sys.moon(now+Math.round(sys.period()*20*(i-1)/128)).position());var b=map.project(sys.moon(now+Math.round(sys.period()*20*i/128)).position());map.line(g,a.x(),a.y(),b.x(),b.y(),0xff727b9b);}var center=sys.moon(now).position();map.marker(g,center,0xffdadde5,Component.translatable("around_planet.moon.name").getString());sphere(g,center,sys.soi(),0xff414e6c);}
        for(var boundary:boundaries(preview)){map.marker(g,boundary.position(),0xff79ffab,Component.translatable(boundary.enter()?"around_planet.moon.enter":"around_planet.moon.exit").getString());if(!moon)sphere(g,sys.moon(boundary.tick()).position(),sys.soi(),0xff456b55);}
        if(!RendezvousClient.active())g.drawString(map.getMinecraft().font,Component.translatable(moon?"around_planet.moon.chart_moon":"around_planet.moon.chart_earth"),13,37,0xffb8c6dd,false);
    }
    private record Boundary(long tick,SpaceVector position,SpaceTrajectory path,boolean enter){}
    private List<Boundary> boundaries(ManeuverPreview preview){var result=new ArrayList<Boundary>();if(!enabled()||preview==null)return result;for(var leg:preview.segments()){if(!leg.sphereBoundary()||leg.points().isEmpty())continue;long tick=leg.points().getLast().tick();var source=leg.trajectory();var path=primary().equals(source.primary())?source:PatchedConics.change(source,tick,system());if(path.primary().equals(primary()))result.add(new Boundary(tick,path.at(tick).position(),path,!CelestialSystem.MOON.equals(source.primary())));}return result;}
    private Boundary hit(double x,double y,ManeuverPreview preview){Boundary hit=null;double distance=10;for(var boundary:boundaries(preview)){var p=map.project(boundary.position());double d=Math.hypot(p.x()-x,p.y()-y);if(d<=distance&&map.visible(boundary.position())){distance=d;hit=boundary;}}return hit;}
    boolean select(double x,double y,ManeuverPreview preview){var hit=hit(x,y,preview);return hit!=null&&map.planAt(hit.tick());}
    boolean tooltip(GuiGraphics g,int x,int y,ManeuverPreview preview){var hit=hit(x,y,preview);if(hit==null)return false;var state=hit.path().at(hit.tick());var lines=new ArrayList<Component>();lines.add(Component.translatable(hit.enter()?"around_planet.moon.enter":"around_planet.moon.exit"));lines.add(Component.translatable("around_planet.map.point_time",String.format(Locale.ROOT,"%.1f",Math.max(0,(hit.tick()-ManeuverClient.now())/20.0))));lines.add(Component.translatable("around_planet.map.point_speed",String.format(Locale.ROOT,"%.2f",state.velocity().length())));lines.add(Component.translatable("around_planet.map.point_altitude",String.format(Locale.ROOT,"%.0f",hit.position().length()-system().radius(primary()))));var p=map.project(hit.position());g.fill((int)p.x()-3,(int)p.y()-3,(int)p.x()+4,(int)p.y()+4,0xffffff99);g.renderComponentTooltip(map.getMinecraft().font,lines,x,y);return true;}
    private void sphere(GuiGraphics g,SpaceVector center,double radius,int color){for(int axis=0;axis<3;axis++)for(int i=1;i<=64;i++){double a=(i-1)*2*Math.PI/64,b=i*2*Math.PI/64;var x=center.add(circle(axis,a,radius));var y=center.add(circle(axis,b,radius));var p=map.project(x);var q=map.project(y);map.line(g,p.x(),p.y(),q.x(),q.y(),color);}}
    private SpaceVector circle(int axis,double a,double r){double x=Math.cos(a)*r,y=Math.sin(a)*r;return axis==0?new SpaceVector(x,y,0):axis==1?new SpaceVector(x,0,y):new SpaceVector(0,x,y);}
}

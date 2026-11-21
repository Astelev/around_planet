package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Nested primary charts share prescribed ephemerides and retain their own orbit origins. */
final class LunarMapLayer {
    private final OrbitMapScreen map;private Button view;private String chart=CelestialSystem.EARTH,actual;
    LunarMapLayer(OrbitMapScreen map){this.map=map;}
    Button init(){view=Button.builder(Component.literal("Earth"),b->{chart=CelestialSystem.SUN.equals(chart)?CelestialSystem.EARTH:CelestialSystem.EARTH.equals(chart)?CelestialSystem.MOON:CelestialSystem.SUN;map.reframe();}).bounds(map.width-300,8,108,20).build();return view;}
    CelestialSystem system(){var p=ManeuverClient.current();return p==null?null:p.system();}
    boolean enabled(){var p=ManeuverClient.current();return p!=null&&system()!=null&&system().managed(p.trajectory().primary());}
    String primary(){return enabled()?chart:ManeuverClient.current()==null?"":ManeuverClient.current().trajectory().primary();}
    boolean sameChart(){var p=ManeuverClient.current();return !enabled()||p!=null&&p.trajectory().primary().equals(primary());}
    boolean moon(){return enabled()&&CelestialSystem.MOON.equals(chart);}
    boolean sun(){return enabled()&&CelestialSystem.SUN.equals(chart);}
    void update(){var p=ManeuverClient.current();if(enabled()&&!Objects.equals(actual,p.trajectory().primary())){boolean changed=actual!=null;actual=p.trajectory().primary();chart=actual;if(changed)map.reframe();}if(view!=null){view.visible=enabled();view.active=enabled();view.setMessage(Component.translatable("around_planet.celestial.chart",bodyName(chart)));}}
    private static Component bodyName(String primary){return Component.translatable("around_planet.celestial."+(CelestialSystem.SUN.equals(primary)?"sun":CelestialSystem.MOON.equals(primary)?"moon":"earth"));}
    void render(GuiGraphics g,ManeuverPreview preview){if(!enabled())return;var sys=system();long now=ManeuverClient.now();
        if(sun()){
            circle(g,t->sys.earth(t).position(),now,sys.solarPeriod(),0xff6c9dcb);
            map.marker(g,sys.earth(now).position(),0xff8fc9ff,bodyName(CelestialSystem.EARTH).getString());
        }
        if(!moon()){
            var offset=sun()?sys.earth(now).position():SpaceVector.ZERO;
            circle(g,t->sys.moon(t).position().add(offset),now,sys.period(),0xff727b9b);
            map.marker(g,sys.moon(now).position().add(offset),0xffdadde5,bodyName(CelestialSystem.MOON).getString());
        }
        for(var boundary:boundaries(preview))map.marker(g,boundary.position(),0xff79ffab,boundary.label().getString());
        if(!RendezvousClient.active())g.drawString(map.getMinecraft().font,Component.translatable("around_planet.celestial.chart",bodyName(chart)),13,37,0xffb8c6dd,false);
    }
    private void circle(GuiGraphics g,java.util.function.LongFunction<SpaceVector> ephemeris,long now,double seconds,int color){for(int i=1;i<=128;i++){var a=map.project(ephemeris.apply(now+Math.round(seconds*20*(i-1)/128)));var b=map.project(ephemeris.apply(now+Math.round(seconds*20*i/128)));map.line(g,a.x(),a.y(),b.x(),b.y(),color);}}
    private record Boundary(long tick,SpaceVector position,SpaceTrajectory path,boolean enter,String body){Component label(){return Component.translatable(enter?"around_planet.celestial.enter":"around_planet.celestial.exit",bodyName(body));}}
    private List<Boundary> boundaries(ManeuverPreview preview){var result=new ArrayList<Boundary>();if(!enabled()||preview==null)return result;for(var leg:preview.segments()){if(!leg.sphereBoundary()||leg.points().isEmpty())continue;long tick=leg.points().getLast().tick();var source=leg.trajectory();var next=PatchedConics.change(source,tick,system());boolean enter=Objects.equals(CelestialSystem.parent(next.primary()),source.primary());String body=enter?next.primary():source.primary();
            if(OrbitChart.visible(source.primary(),primary())||OrbitChart.visible(next.primary(),primary())){var path=primary().equals(source.primary())?source:next;var position=path.at(tick).position().add(OrbitChart.offset(system(),path.primary(),primary(),tick));result.add(new Boundary(tick,position,path,enter,body));}}
        return result;}
    private Boundary hit(double x,double y,ManeuverPreview preview){Boundary hit=null;double distance=10;for(var boundary:boundaries(preview)){var p=map.project(boundary.position());double d=Math.hypot(p.x()-x,p.y()-y);if(d<=distance&&map.visible(boundary.position())){distance=d;hit=boundary;}}return hit;}
    boolean select(double x,double y,ManeuverPreview preview){var hit=hit(x,y,preview);return hit!=null&&map.planAt(hit.tick());}
    boolean tooltip(GuiGraphics g,int x,int y,ManeuverPreview preview){var hit=hit(x,y,preview);if(hit==null)return false;var state=hit.path().at(hit.tick());var lines=new ArrayList<Component>();lines.add(hit.label());lines.add(Component.translatable("around_planet.map.point_time",String.format(Locale.ROOT,"%.1f",Math.max(0,(hit.tick()-ManeuverClient.now())/20.0))));lines.add(Component.translatable("around_planet.map.point_speed",String.format(Locale.ROOT,"%.2f",state.velocity().length())));lines.add(Component.translatable("around_planet.map.point_altitude",String.format(Locale.ROOT,"%.0f",state.position().length()-system().radius(hit.path().primary()))));var p=map.project(hit.position());g.fill((int)p.x()-3,(int)p.y()-3,(int)p.x()+4,(int)p.y()+4,0xffffff99);g.renderComponentTooltip(map.getMinecraft().font,lines,x,y);return true;}
}

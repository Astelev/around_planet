package io.github.astelev.around_planet.client;
import io.github.astelev.around_planet.space.*;
import io.github.astelev.around_planet.network.ManeuverPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
/** Shared read-only preview; only a server-granted planner session creates edit controls. */
final class ManeuverMapLayer {
    private final OrbitMapScreen map;private final UUID session;private final List<AbstractWidget> widgets=new ArrayList<>();
    private EditBox warning,timeField;private OrbitPlot result;private java.util.List<OrbitInspection.Apsis> apsides=java.util.List.of();private double radius;
    private final EditBox[] fields=new EditBox[3];private ManeuverNode draft;private UUID selected;private boolean applying;private long sequence,at=Long.MIN_VALUE;private int x,y,width,step=10;
    private ManeuverPayload packet;private ManeuverPreview preview;private boolean dirty=true;private String error="";
    ManeuverMapLayer(OrbitMapScreen map,UUID session){this.map=map;this.session=session;}
    boolean hasPlan(){return !nodes().isEmpty();}
    boolean editor(){return session!=null;}
    boolean authorized(){var p=ManeuverClient.current();return editor()&&p!=null&&session.equals(p.session());}
    List<AbstractWidget> init(int x,int y,int width){this.x=x;this.y=y;this.width=width;widgets.clear();if(!editor())return List.of();
        for(int i=0;i<3;i++){final int axis=i;var field=new EditBox(map.getMinecraft().font,x+71,y+29+i*22,63,18,Component.translatable("around_planet.maneuver.axis."+i));field.setTooltip(Tooltip.create(Component.translatable("around_planet.maneuver.help."+i)));field.setMaxLength(12);field.setFilter(v->v.matches("-?\\d*(\\.\\d*)?"));field.setResponder(v->changed());fields[i]=field;widgets.add(field);
            widgets.add(Button.builder(Component.literal("−"),b->increment(axis,-step)).bounds(x+137,y+29+i*22,16,18).build());widgets.add(Button.builder(Component.literal("+"),b->increment(axis,step)).bounds(x+155,y+29+i*22,16,18).build());}
        widgets.add(Button.builder(Component.literal("± "+step),b->{step=step==1?10:step==10?100:1;b.setMessage(Component.literal("± "+step));}).bounds(x,y+98,57,18).build());
        widgets.add(Button.builder(Component.translatable("around_planet.maneuver.delete"),b->delete()).bounds(x+59,y+98,64,18).build());
        widgets.add(Button.builder(Component.literal("<"),b->cycle(-1)).bounds(x+125,y+98,22,18).build());widgets.add(Button.builder(Component.literal(">"),b->cycle(1)).bounds(x+149,y+98,22,18).build());
        warning=new EditBox(map.getMinecraft().font,x+109,y+158,62,18,Component.translatable("around_planet.maneuver.warning",0));warning.setMaxLength(16);warning.setFilter(v->v.matches("\\d*([.,]\\d*)?"));warning.setTooltip(Tooltip.create(Component.translatable("around_planet.maneuver.warning_help")));warning.setResponder(v->{if(applying||draft==null||!authorized()||v.isEmpty())return;try{draft=new ManeuverNode(draft.id(),draft.tick(),draft.prograde(),draft.normal(),draft.radial(),Double.parseDouble(v.replace(',','.')));dirty=true;error="";submit(false);}catch(IllegalArgumentException ignored){error="around_planet.maneuver.warning_range";}});widgets.add(warning);
        timeField=new EditBox(map.getMinecraft().font,x+71,y+11,63,16,Component.translatable("around_planet.maneuver.time"));timeField.setMaxLength(12);timeField.setFilter(v->v.matches("\\d*(\\.\\d*)?"));timeField.setTooltip(Tooltip.create(Component.translatable("around_planet.maneuver.time_help")));timeField.setResponder(v->{if(applying||draft==null||!authorized()||v.isEmpty())return;try{double seconds=Double.parseDouble(v);if(!Double.isFinite(seconds)||seconds<1||seconds>1e12)throw new IllegalArgumentException();retime(Math.addExact(ManeuverClient.now(),Math.round(seconds*20)));}catch(IllegalArgumentException|ArithmeticException ignored){error="around_planet.maneuver.number";}});widgets.add(timeField);
        widgets.add(Button.builder(Component.literal("−"),b->{if(draft!=null)retime(Math.max(ManeuverClient.now()+20,draft.tick()-step*20L));setFields();}).bounds(x+137,y+11,16,16).build());widgets.add(Button.builder(Component.literal("+"),b->{if(draft!=null)retime(Math.addExact(draft.tick(),step*20L));setFields();}).bounds(x+155,y+11,16,16).build());
        setFields();return List.copyOf(widgets);
    }
    void update(double radius,double entry){this.radius=radius;var p=ManeuverClient.current();if(p==null)return;
        if(p!=packet){packet=p;dirty=true;if(draft!=null&&p.ack()>=sequence&&p.error().isEmpty())draft=p.nodes().stream().filter(n->n.id().equals(selected)).findFirst().orElse(null);if(!p.error().isEmpty()&&p.ack()>=sequence){error=p.error();draft=p.nodes().stream().filter(n->n.id().equals(selected)).findFirst().orElse(null);setFields();}}
        for(var widget:widgets)widget.active=authorized()&&(draft!=null||widget instanceof Button);
        long now=ManeuverClient.now();if(warning!=null&&!warning.isFocused()&&draft!=null){applying=true;warning.setValue(String.format(Locale.ROOT,"%.2f",draft.warningSeconds(packet.warningSeconds())));applying=false;}if(timeField!=null&&!timeField.isFocused()&&draft!=null){applying=true;timeField.setValue(String.format(Locale.ROOT,"%.2f",Math.max(0,(draft.tick()-now)/20.0)));applying=false;}if(!dirty&&now-at<5)return;at=now;dirty=false;
        try{preview=ManeuverPreview.create(p.trajectory(),now,nodes(),radius,entry);var extrema=new ArrayList<>(OrbitInspection.apsides(p.trajectory(),now,radius,entry,false));var finalSegment=preview.segments().getLast();if(finalSegment.planned()&&!finalSegment.points().isEmpty())extrema.addAll(OrbitInspection.apsides(finalSegment.trajectory(),finalSegment.points().getFirst().tick(),radius,entry,true));apsides=List.copyOf(extrema);result=null;if(selected!=null)for(var mark:preview.nodes())if(mark.node().id().equals(selected)){result=OrbitPlot.create(mark.after().initial(),mark.after().mu(),radius,entry,false);break;}}catch(RuntimeException failure){preview=null;error="around_planet.maneuver.preview_failed";}
    }
    private List<ManeuverNode> nodes(){var result=new ArrayList<ManeuverNode>(packet==null?List.of():packet.nodes());if(draft!=null){result.removeIf(n->n.id().equals(draft.id()));result.add(draft);}result.sort(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id));return result;}
    private void select(ManeuverNode node){selected=node.id();draft=node;error="";dirty=true;setFields();}
    private void setFields(){applying=true;for(int i=0;i<3;i++)if(fields[i]!=null){double value=draft==null?0:i==0?draft.prograde():i==1?draft.normal():draft.radial();fields[i].setValue(String.format(Locale.ROOT,"%.2f",value));}if(warning!=null)warning.setValue(String.format(Locale.ROOT,"%.2f",draft==null?(packet==null?30:packet.warningSeconds()):draft.warningSeconds(packet==null?30:packet.warningSeconds())));if(timeField!=null)timeField.setValue(draft==null?"":String.format(Locale.ROOT,"%.2f",Math.max(0,(draft.tick()-ManeuverClient.now())/20.0)));applying=false;}
    private void increment(int axis,double amount){if(draft==null||!authorized())return;double value=axis==0?draft.prograde():axis==1?draft.normal():draft.radial();fields[axis].setValue(String.format(Locale.ROOT,"%.2f",Math.clamp(value+amount,-ManeuverNode.MAX_DV,ManeuverNode.MAX_DV)));}
    private void changed(){if(applying||draft==null||!authorized())return;try{draft=new ManeuverNode(draft.id(),draft.tick(),Double.parseDouble(fields[0].getValue()),Double.parseDouble(fields[1].getValue()),Double.parseDouble(fields[2].getValue()),draft.warningSeconds());error="";dirty=true;submit(false);}catch(IllegalArgumentException e){error="around_planet.maneuver.number";}}
    private void retime(long tick){if(draft==null||!authorized())return;draft=new ManeuverNode(draft.id(),tick,draft.prograde(),draft.normal(),draft.radial(),draft.warningSeconds());dirty=true;error="";submit(false);}
    private void submit(boolean delete){if(draft!=null&&authorized())PacketDistributor.sendToServer(new ManeuverPayload.Edit(session,++sequence,delete,draft));}
    private void delete(){if(draft==null)return;submit(true);selected=null;draft=null;dirty=true;setFields();}
    private void cycle(int direction){var nodes=nodes();if(nodes.isEmpty())return;int index=0;for(int i=0;i<nodes.size();i++)if(nodes.get(i).id().equals(selected))index=i;select(nodes.get(Math.floorMod(index+direction,nodes.size())));}
    void paths(GuiGraphics g,boolean behind){if(preview==null)return;for(var segment:preview.segments())if(segment.planned())for(int i=1;i<segment.points().size();i++){var a=map.project(segment.points().get(i-1).position());var b=map.project(segment.points().get(i).position());if(((a.depth()+b.depth())/2<0)==behind)map.line(g,a.x(),a.y(),b.x(),b.y(),behind?0xff795427:0xffffac4d);}}
    void markers(GuiGraphics g,int mx,int my){if(preview==null)return;for(var a:apsides)map.marker(g,a.position(),a.planned()?0xffffac4d:a.apo()?0xffaaa6ff:0xffff9cd1,(a.planned()?"Δv ":"")+Component.translatable(a.apo()?"around_planet.map.apoapsis":"around_planet.map.periapsis").getString());for(var mark:preview.nodes()){var p=map.project(mark.position());boolean hover=Math.hypot(p.x()-mx,p.y()-my)<9;String text=hover?String.format(Locale.ROOT,"%s · Δv %.1f m/s",time(mark.node().tick()),mark.node().magnitude()):mark.node().id().equals(selected)?"Δv":"";map.marker(g,mark.position(),0xffffac4d,text);}}
    boolean clickPriority(double mx,double my){if(!authorized()||preview==null)return false;for(var mark:preview.nodes()){var p=map.project(mark.position());if(Math.hypot(p.x()-mx,p.y()-my)<=10&&map.visible(mark.position())){select(mark.node());return true;}}var hit=pick(mx,my,true);return hit!=null&&hit.apsis()!=null&&hit.planned()&&at(hit.tick());}
    boolean click(double mx,double my){if(!authorized()||preview==null)return false;
        for(var mark:preview.nodes()){var p=map.project(mark.position());if(Math.hypot(p.x()-mx,p.y()-my)<=9&&map.visible(mark.position())){select(mark.node());return true;}}
        var hit=pick(mx,my,true);if(hit==null)return false;long tick=hit.tick();
        return at(tick);
    }
    boolean at(long tick){if(!authorized())return false;tick=Math.max(tick,ManeuverClient.now()+20);if(nodes().size()>=ManeuverNode.MAX_NODES){error="around_planet.maneuver.limit";return true;}select(new ManeuverNode(UUID.randomUUID(),tick,0,0,0,packet==null?30:packet.warningSeconds()));submit(false);return true;}

    private record Hit(SpaceTrajectory path,long tick,SpaceVector position,boolean planned,OrbitInspection.Apsis apsis){}
    private Hit pick(double mx,double my,boolean editing){
        OrbitInspection.Apsis snapped=null;double snapDistance=10;for(var a:apsides){if(editing&&!a.planned()&&preview!=null&&!preview.nodes().isEmpty()&&a.tick()>=preview.nodes().getFirst().node().tick())continue;var point=map.project(a.position());double distance=Math.hypot(point.x()-mx,point.y()-my);if(distance<=10&&map.visible(a.position())&&(snapped==null||a.planned()&&!snapped.planned()||a.planned()==snapped.planned()&&distance<=snapDistance)){snapped=a;snapDistance=distance;}}if(snapped!=null)return new Hit(snapped.path(),snapped.tick(),snapped.position(),snapped.planned(),snapped);
        if(preview==null)return null;
        double best=8;long tick=-1,low=0,high=0;ManeuverPreview.Segment picked=null;for(var segment:preview.segments())for(int i=1;i<segment.points().size();i++){
            var a=segment.points().get(i-1);var b=segment.points().get(i);var aa=map.project(a.position());var bb=map.project(b.position());double dx=bb.x()-aa.x(),dy=bb.y()-aa.y(),length=dx*dx+dy*dy;if(length<1e-8)continue;
            double t=Math.clamp(((mx-aa.x())*dx+(my-aa.y())*dy)/length,0,1),distance=Math.hypot(aa.x()+t*dx-mx,aa.y()+t*dy-my);var point=a.position().add(b.position().subtract(a.position()).scale(t));
            if(distance<=8&&(distance<=best||picked!=null&&segment.planned()&&!picked.planned()&&distance<=best+1)&&map.visible(point)){best=distance;tick=a.tick()+Math.round((b.tick()-a.tick())*t);low=a.tick();high=b.tick();picked=segment;}
        }
        if(tick<0)return null;
        if(picked!=null&&high>low){var path=picked.trajectory();for(int i=0;i<28&&high-low>3;i++){long a=low+(high-low)/3,b=high-(high-low)/3;if(score(path,a,mx,my)<score(path,b,mx,my))high=b;else low=a;}tick=low;for(long candidate:new long[]{low,low+(high-low)/2,high})if(score(path,candidate,mx,my)<score(path,tick,mx,my))tick=candidate;}
        if(picked==null)return null;return new Hit(picked.trajectory(),tick,picked.trajectory().at(tick).position(),picked.planned(),null);
    }
    boolean tooltip(GuiGraphics g,int mx,int my){return tooltip(g,mx,my,false);}
    boolean tooltip(GuiGraphics g,int mx,int my,boolean plannedOnly){
        if(map.getMinecraft().screen!=map||preview==null)return false;for(var mark:preview.nodes()){var p=map.project(mark.position());if(Math.hypot(p.x()-mx,p.y()-my)<=10&&map.visible(mark.position())){var lines=new ArrayList<Component>();lines.add(Component.translatable("around_planet.map.maneuver_node"));lines.add(Component.literal(String.format(Locale.ROOT,"Δv %.2f m/s",mark.node().magnitude())));lines.add(Component.translatable("around_planet.map.point_time",String.format(Locale.ROOT,"%.1f",Math.max(0,(mark.node().tick()-ManeuverClient.now())/20.0))));lines.add(Component.translatable("around_planet.map.point_altitude",String.format(Locale.ROOT,"%.0f",mark.position().length()-radius)));lines.add(Component.translatable("around_planet.map.point_speed",String.format(Locale.ROOT,"%.2f",mark.after().initial().velocity().length())));g.fill((int)p.x()-3,(int)p.y()-3,(int)p.x()+4,(int)p.y()+4,0xffffff99);g.renderComponentTooltip(map.getMinecraft().font,lines,mx,my);return true;}}var hit=pick(mx,my,false);if(hit==null||plannedOnly&&(hit.apsis()==null||!hit.planned()))return false;var state=hit.path().at(hit.tick());var lines=new ArrayList<Component>();
        if(hit.apsis()!=null){var a=hit.apsis();lines.add(Component.translatable(a.planned()?"around_planet.map.planned_apsis":"around_planet.map.apsis",Component.translatable(a.apo()?"around_planet.map.apoapsis":"around_planet.map.periapsis")));var p=map.project(a.position());g.fill((int)p.x()-3,(int)p.y()-3,(int)p.x()+4,(int)p.y()+4,0xffffff99);}
        lines.add(Component.translatable("around_planet.map.point_time",String.format(Locale.ROOT,"%.1f",Math.max(0,(hit.tick()-ManeuverClient.now())/20.0))));
        lines.add(Component.translatable("around_planet.map.point_speed",String.format(Locale.ROOT,"%.2f",state.velocity().length())));lines.add(Component.translatable("around_planet.map.point_altitude",String.format(Locale.ROOT,"%.0f",hit.position().length()-radius)));g.renderComponentTooltip(map.getMinecraft().font,lines,mx,my);return true;
    }
    private double score(SpaceTrajectory path,long tick,double x,double y){var p=map.project(path.at(tick).position());return Math.hypot(p.x()-x,p.y()-y);}
    private static String time(long tick){double seconds=(tick-ManeuverClient.now())/20.0;return String.format(Locale.ROOT,"T%s%.1f s",seconds>=0?"−":"+",Math.abs(seconds));}
    private String altitude(SpaceVector p){return p==null?"—":String.format(Locale.ROOT,"%.0f m",p.length()-radius);}
    void panel(GuiGraphics g){if(!editor())return;g.fill(x-4,y-3,x+width,y+182,0xed101d30);var font=map.getMinecraft().font;
        g.drawString(font,Component.translatable("around_planet.maneuver.node",nodes().size()),x,y,0xffffb761,false);
        for(int i=0;i<3;i++)g.drawString(font,Component.translatable("around_planet.maneuver.axis."+i),x,y+34+i*22,0xffbcd2e3,false);
        var p=ManeuverClient.current();Component status=!authorized()?Component.translatable("around_planet.maneuver.readonly"):!error.isEmpty()?Component.translatable(error):Component.literal(draft==null?"Δv: m/s":String.format(Locale.ROOT,"Δv %.2f m/s %s",draft.magnitude(),p!=null&&p.ack()>=sequence?"✓":"…"));g.drawString(font,status,x,y+120,error.isEmpty()?0xffacd9cb:0xffff7777,false);
        g.drawString(font,"Pe: "+altitude(result==null?null:result.periapsis()),x,y+134,0xffffb761,false);g.drawString(font,"Ap: "+(result!=null&&result.open()?"∞":altitude(result==null?null:result.apoapsis())),x,y+146,0xffffb761,false);g.drawString(font,Component.translatable("around_planet.maneuver.warning_short"),x,y+162,0xffbcd2e3,false);g.drawString(font,Component.translatable("around_planet.maneuver.time"),x,y+16,0xffbcd2e3,false);
    }
}

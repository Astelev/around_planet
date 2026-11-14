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
        setFields();return List.copyOf(widgets);
    }
    void update(double radius,double entry){var p=ManeuverClient.current();if(p==null)return;
        if(p!=packet){packet=p;dirty=true;if(!p.error().isEmpty()&&p.ack()>=sequence){error=p.error();draft=p.nodes().stream().filter(n->n.id().equals(selected)).findFirst().orElse(null);setFields();}}
        for(var widget:widgets)widget.active=authorized()&&(draft!=null||widget instanceof Button);
        long now=ManeuverClient.now();if(!dirty&&now-at<5)return;at=now;dirty=false;
        try{preview=ManeuverPreview.create(p.trajectory(),now,nodes(),radius,entry);}catch(RuntimeException failure){preview=null;error="around_planet.maneuver.preview_failed";}
    }
    private List<ManeuverNode> nodes(){var result=new ArrayList<ManeuverNode>(packet==null?List.of():packet.nodes());if(draft!=null){result.removeIf(n->n.id().equals(draft.id()));result.add(draft);}result.sort(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id));return result;}
    private void select(ManeuverNode node){selected=node.id();draft=node;error="";dirty=true;setFields();}
    private void setFields(){applying=true;for(int i=0;i<3;i++)if(fields[i]!=null){double value=draft==null?0:i==0?draft.prograde():i==1?draft.normal():draft.radial();fields[i].setValue(String.format(Locale.ROOT,"%.2f",value));}applying=false;}
    private void increment(int axis,double amount){if(draft==null||!authorized())return;double value=axis==0?draft.prograde():axis==1?draft.normal():draft.radial();fields[axis].setValue(String.format(Locale.ROOT,"%.2f",Math.clamp(value+amount,-ManeuverNode.MAX_DV,ManeuverNode.MAX_DV)));}
    private void changed(){if(applying||draft==null||!authorized())return;try{draft=new ManeuverNode(draft.id(),draft.tick(),Double.parseDouble(fields[0].getValue()),Double.parseDouble(fields[1].getValue()),Double.parseDouble(fields[2].getValue()));error="";dirty=true;submit(false);}catch(IllegalArgumentException e){error="around_planet.maneuver.number";}}
    private void submit(boolean delete){if(draft!=null&&authorized())PacketDistributor.sendToServer(new ManeuverPayload.Edit(session,++sequence,delete,draft));}
    private void delete(){if(draft==null)return;submit(true);selected=null;draft=null;dirty=true;setFields();}
    private void cycle(int direction){var nodes=nodes();if(nodes.isEmpty())return;int index=0;for(int i=0;i<nodes.size();i++)if(nodes.get(i).id().equals(selected))index=i;select(nodes.get(Math.floorMod(index+direction,nodes.size())));}
    void paths(GuiGraphics g,boolean behind){if(preview==null)return;for(var segment:preview.segments())if(segment.planned())for(int i=1;i<segment.points().size();i++){var a=map.project(segment.points().get(i-1).position());var b=map.project(segment.points().get(i).position());if(((a.depth()+b.depth())/2<0)==behind)map.line(g,a.x(),a.y(),b.x(),b.y(),behind?0xff795427:0xffffac4d);}}
    void markers(GuiGraphics g,int mx,int my){if(preview==null)return;for(var mark:preview.nodes()){var p=map.project(mark.position());boolean hover=Math.hypot(p.x()-mx,p.y()-my)<9;String text=hover?String.format(Locale.ROOT,"%s · Δv %.1f m/s",time(mark.node().tick()),mark.node().magnitude()):mark.node().id().equals(selected)?"Δv":"";map.marker(g,mark.position(),0xffffac4d,text);}}
    boolean click(double mx,double my){if(!authorized()||preview==null)return false;
        for(var mark:preview.nodes()){var p=map.project(mark.position());if(Math.hypot(p.x()-mx,p.y()-my)<=9&&map.visible(mark.position())){select(mark.node());return true;}}
        double best=8;long tick=-1;for(var segment:preview.segments())for(int i=1;i<segment.points().size();i++){
            var a=segment.points().get(i-1);var b=segment.points().get(i);var aa=map.project(a.position());var bb=map.project(b.position());double dx=bb.x()-aa.x(),dy=bb.y()-aa.y(),length=dx*dx+dy*dy;if(length<1e-8)continue;
            double t=Math.clamp(((mx-aa.x())*dx+(my-aa.y())*dy)/length,0,1),distance=Math.hypot(aa.x()+t*dx-mx,aa.y()+t*dy-my);var point=a.position().add(b.position().subtract(a.position()).scale(t));
            if(distance<best&&map.visible(point)){best=distance;tick=a.tick()+Math.round((b.tick()-a.tick())*t);}
        }
        if(tick<0)return false;tick=Math.max(tick,ManeuverClient.now()+20);if(tick-ManeuverClient.now()>ManeuverNode.HORIZON||nodes().size()>=ManeuverNode.MAX_NODES){error="around_planet.maneuver.limit";return true;}
        select(new ManeuverNode(UUID.randomUUID(),tick,0,0,0));submit(false);return true;
    }
    private static String time(long tick){double seconds=(tick-ManeuverClient.now())/20.0;return String.format(Locale.ROOT,"T%s%.1f s",seconds>=0?"−":"+",Math.abs(seconds));}
    void panel(GuiGraphics g){if(!editor())return;g.fill(x-4,y-3,x+width,y+132,0xed101d30);var font=map.getMinecraft().font;
        g.drawString(font,Component.translatable("around_planet.maneuver.node",nodes().size()),x,y,0xffffb761,false);
        if(draft!=null){double seconds=(draft.tick()-ManeuverClient.now())/20.0;g.drawString(font,time(draft.tick()),x,y+14,seconds>0?0xffc8d7e5:0xffff7777,false);}
        else g.drawString(font,Component.translatable("around_planet.maneuver.choose"),x,y+14,0xffc8d7e5,false);
        for(int i=0;i<3;i++)g.drawString(font,Component.translatable("around_planet.maneuver.axis."+i),x,y+34+i*22,0xffbcd2e3,false);
        var p=ManeuverClient.current();Component status=!authorized()?Component.translatable("around_planet.maneuver.readonly"):!error.isEmpty()?Component.translatable(error):Component.literal(draft==null?"Δv: m/s":String.format(Locale.ROOT,"Δv %.2f m/s %s",draft.magnitude(),p!=null&&p.ack()>=sequence?"✓":"…"));g.drawString(font,status,x,y+120,error.isEmpty()?0xffacd9cb:0xffff7777,false);
    }
}

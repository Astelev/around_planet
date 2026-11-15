package io.github.astelev.around_planet.space;
import java.util.*;
/** Pure on-demand prediction. Never mutates an actual orbit or executes a burn. */
public record ManeuverPreview(List<Segment> segments,List<Mark> nodes) {
    public record Point(long tick,SpaceVector position){}
    public record Segment(List<Point> points,boolean planned,SpaceTrajectory trajectory,boolean sphereBoundary){public Segment(List<Point> points,boolean planned,SpaceTrajectory trajectory){this(points,planned,trajectory,false);}public Segment{points=List.copyOf(points);}}
    public record Mark(ManeuverNode node,SpaceVector position,SpaceTrajectory after){}
    public ManeuverPreview {segments=List.copyOf(segments);nodes=List.copyOf(nodes);}
    public static ManeuverPreview create(SpaceTrajectory base,long now,List<ManeuverNode> plan,double radius,double entry){
        var segments=new ArrayList<Segment>();var marks=new ArrayList<Mark>();var trajectory=base;long from=now;boolean planned=false;
        for(var node:plan.stream().filter(n->n.tick()>now).sorted(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id)).toList()){
            var points=sample(trajectory,from,node.tick(),radius+entry,48);segments.add(new Segment(points,planned,trajectory));
            if(points.isEmpty()||points.getLast().tick()!=node.tick())return new ManeuverPreview(segments,marks);
            var state=trajectory.at(node.tick());trajectory=trajectory.impulse(node.tick(),node.delta(state));marks.add(new Mark(node,state.position(),trajectory));from=node.tick();planned=true;
        }
        var state=trajectory.at(from);var ellipse=EllipticArc.of(state,trajectory.mu());
        if(ellipse!=null)segments.add(new Segment(timed(ellipse.full(radius+entry,384),from),planned,trajectory));
        else {double seconds=Math.max(120,Math.min(3600,Math.max(radius*20,state.position().length()*5)/Math.max(1,state.velocity().length())));segments.add(new Segment(sample(trajectory,from,tickAt(from,seconds),radius+entry,256),planned,trajectory));}
        return new ManeuverPreview(segments,marks);
    }
    public static ManeuverPreview create(SpaceTrajectory base,long now,List<ManeuverNode> plan,double radius,double entry,CelestialSystem system){return create(base,now,plan,radius,entry,system,6);}
    public static ManeuverPreview create(SpaceTrajectory base,long now,List<ManeuverNode> plan,double radius,double entry,CelestialSystem system,int patchLimit){
        if(system==null||!system.managed(base.primary()))return create(base,now,plan,radius,entry);
        var segments=new ArrayList<Segment>();var marks=new ArrayList<Mark>();var path=base;long from=now;boolean planned=false;
        for(var node:plan.stream().filter(n->n.tick()>now).sorted(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id)).toList()){
            var flight=PatchedConics.trace(path,from,node.tick(),system);if(!append(segments,flight,planned,system,entry,false))return limited(segments,marks,patchLimit);path=flight.finalPath();var state=path.at(node.tick());path=path.impulse(node.tick(),node.delta(state));marks.add(new Mark(node,state.position(),path));from=node.tick();planned=true;
        }
        int limit=Math.clamp(patchLimit,1,16),patches=1;for(int i=1;i<segments.size();i++)if(!segments.get(i-1).trajectory().primary().equals(segments.get(i).trajectory().primary()))patches++;
        for(int i=0;i<limit;i++){var ellipse=EllipticArc.of(path.at(from),path.mu());double horizon=Math.max(system.period()*8,ellipse==null?3600:ellipse.period());long until=tickAt(from,horizon),event=PatchedConics.transition(path,from,until,system);boolean transition=event>=0;long end=transition?event:until;
            var flight=new PatchedConics.Flight(List.of(new PatchedConics.Leg(path,from,end,transition)),path);if(!append(segments,flight,planned,system,entry,!transition))break;if(!transition||patches>=limit)break;path=PatchedConics.change(path,event,system);from=event;patches++;
        }return limited(segments,marks,patchLimit);
    }
    private static ManeuverPreview limited(List<Segment> segments,List<Mark> marks,int maximum){var visible=new ArrayList<Segment>();String primary=null;int patches=0;for(var segment:segments){if(!segment.trajectory().primary().equals(primary)){if(++patches>Math.clamp(maximum,1,16))break;primary=segment.trajectory().primary();}visible.add(segment);}var nodes=marks.stream().filter(mark->visible.stream().anyMatch(seg->seg.trajectory().equals(mark.after()))).toList();return new ManeuverPreview(visible,nodes);}
    private static boolean append(List<Segment> segments,PatchedConics.Flight flight,boolean planned,CelestialSystem system,double entry,boolean finalCoast){for(var leg:flight.legs()){double shell=system.radius(leg.path().primary())+(CelestialSystem.MOON.equals(leg.path().primary())?0:system.earthEntry());long until=leg.until();var ellipse=EllipticArc.of(leg.path().at(leg.from()),leg.path().mu());if(finalCoast&&!leg.transition()&&ellipse!=null)until=Math.min(until,tickAt(leg.from(),ellipse.period()));var points=finalCoast&&!leg.transition()&&ellipse!=null?timed(ellipse.full(CelestialSystem.MOON.equals(leg.path().primary())?0:shell,384),leg.from()):sample(leg.path(),leg.from(),until,CelestialSystem.MOON.equals(leg.path().primary())?0:shell,256);segments.add(new Segment(points,planned,leg.path(),leg.transition()&&!points.isEmpty()&&points.getLast().tick()==leg.until()));if(points.isEmpty()||points.getLast().tick()!=until)return false;}return true;}
    public static long tickAt(long from,double seconds){double ticks=Math.max(0,seconds*20);return ticks>=Long.MAX_VALUE-from?Long.MAX_VALUE:from+Math.round(ticks);}
    private static List<Point> timed(EllipticArc.Arc arc,long from){var result=new ArrayList<Point>();for(var v:arc.vertices())result.add(new Point(tickAt(from,v.seconds()),v.position()));return result;}

    private static List<Point> sample(SpaceTrajectory path,long from,long until,double shell,int count){
        var ellipse=EllipticArc.of(path.at(from),path.mu());
        if(ellipse!=null){long start=from;if((until-from)/20.0>ellipse.period()){start=Math.max(from,until-Math.min(until-from,(long)Math.ceil(ellipse.period()*20)));ellipse=EllipticArc.of(path.at(start),path.mu());}var arc=ellipse.until((until-start)/20.0,shell,384);var points=new ArrayList<>(timed(arc,start));if(!arc.reentry()&&!points.isEmpty())points.set(points.size()-1,new Point(until,path.at(until).position()));return points;}
        var points=new ArrayList<Point>();long last=Long.MIN_VALUE;
        for(int i=0;i<=count;i++){long tick=from+(long)((until-from)*(i/(double)count));if(tick==last)continue;var state=path.at(tick);if(state.position().length()<shell-1e-5&&i>0)break;points.add(new Point(tick,state.position()));last=tick;}
        return points;
    }
}

package io.github.astelev.around_planet.space;
import java.util.*;
/** Pure on-demand prediction. Never mutates an actual orbit or executes a burn. */
public record ManeuverPreview(List<Segment> segments,List<Mark> nodes) {
    public record Point(long tick,SpaceVector position){}
    public record Segment(List<Point> points,boolean planned){public Segment{points=List.copyOf(points);}}
    public record Mark(ManeuverNode node,SpaceVector position){}
    public ManeuverPreview {segments=List.copyOf(segments);nodes=List.copyOf(nodes);}
    public static ManeuverPreview create(SpaceTrajectory base,long now,List<ManeuverNode> plan,double radius,double entry){
        var segments=new ArrayList<Segment>();var marks=new ArrayList<Mark>();var trajectory=base;long from=now;boolean planned=false;
        for(var node:plan.stream().filter(n->n.tick()>now).sorted(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id)).toList()){
            var points=sample(trajectory,from,node.tick(),radius+entry,48);segments.add(new Segment(points,planned));
            if(points.isEmpty()||points.getLast().tick()!=node.tick())return new ManeuverPreview(segments,marks);
            var state=trajectory.at(node.tick());marks.add(new Mark(node,state.position()));trajectory=trajectory.impulse(node.tick(),node.delta(state));from=node.tick();planned=true;
        }
        var state=trajectory.at(from);var curve=OrbitPlot.create(state,trajectory.mu(),radius,entry,false);
        double seconds=curve.period()>0?curve.period():Math.max(120,Math.min(3600,Math.max(radius*20,state.position().length()*5)/Math.max(1,state.velocity().length())));
        long end=from+Math.min(ManeuverNode.HORIZON,Math.max(20,(long)Math.ceil(seconds*20)));
        segments.add(new Segment(sample(trajectory,from,end,radius+entry,256),planned));return new ManeuverPreview(segments,marks);
    }
    private static List<Point> sample(SpaceTrajectory path,long from,long until,double shell,int count){
        var points=new ArrayList<Point>();long last=Long.MIN_VALUE;
        for(int i=0;i<=count;i++){long tick=from+(until-from)*i/count;if(tick==last)continue;var state=path.at(tick);if(state.position().length()<shell-1e-5&&i>0)break;points.add(new Point(tick,state.position()));last=tick;}
        return points;
    }
}

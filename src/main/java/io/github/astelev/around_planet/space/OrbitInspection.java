package io.github.astelev.around_planet.space;
import java.util.*;
/** Exact extrema dates and point telemetry, shared by map hover and maneuver snapping. */
public final class OrbitInspection {
    public record Apsis(SpaceTrajectory path,long tick,SpaceVector position,boolean apo,boolean planned){}
    public static List<Apsis> apsides(SpaceTrajectory path,long from,double radius,double entry,boolean planned){var state=path.at(from);var plot=OrbitPlot.create(state,path.mu(),radius,entry,false);var ellipse=EllipticArc.of(state,path.mu());var result=new ArrayList<Apsis>();if(ellipse==null){if(plot.periapsis()!=null&&plot.periapsis().length()>=radius+entry&&state.position().dot(state.velocity())<0){var future=new SpaceTrajectory("inspect",0,state,path.mu());double lo=0,hi=Math.max(1,2*state.position().length()/Math.max(1,state.velocity().length()));for(int i=0;i<64;i++){var at=future.afterSeconds(hi);if(at.position().dot(at.velocity())>=0)break;hi*=2;}for(int i=0;i<48;i++){double mid=(lo+hi)/2;var at=future.afterSeconds(mid);if(at.position().dot(at.velocity())<0)lo=mid;else hi=mid;}result.add(new Apsis(path,ManeuverPreview.tickAt(from,(lo+hi)/2),plot.periapsis(),false,planned));}return List.copyOf(result);}
        // Circular trajectories have no distinct extrema and should not magnetize arbitrary points.
        if(plot.periapsis()!=null&&plot.apoapsis()!=null&&Math.abs(plot.periapsis().length()-plot.apoapsis().length())<1e-6*plot.apoapsis().length())return result;
        for(boolean apo:new boolean[]{false,true}){var position=apo?plot.apoapsis():plot.periapsis();if(position==null||position.length()<radius+entry-1e-5)continue;double seconds=ellipse.secondsTo(position);if(seconds<1)seconds+=ellipse.period();result.add(new Apsis(path,ManeuverPreview.tickAt(from,seconds),position,apo,planned));}return List.copyOf(result);
    }
    private OrbitInspection(){}
}

package io.github.astelev.around_planet.space;
import java.util.*;
/** Shared definition of the nearest upcoming burn for sensor, planner and helmet. */
public final class ManeuverGuidance {
    public static Optional<ManeuverNode> next(List<ManeuverNode> nodes,long now){return nodes.stream().filter(n->n.tick()>=now&&n.magnitude()>.005).min(Comparator.comparingLong(ManeuverNode::tick).thenComparing(ManeuverNode::id));}
    public static Optional<SpaceVector> direction(SpaceTrajectory orbit,ManeuverNode node){var delta=node.delta(orbit.at(node.tick()));double length=delta.length();return length<1e-8?Optional.empty():Optional.of(delta.scale(1/length));}
    public static boolean warning(long now,long tick,double seconds){return warningTicks(now,tick,WarningDuration.ticks(seconds));}
    public static boolean warningTicks(long now,long tick,long ticks){return ticks>0&&tick>now&&tick-now<=ticks;}
    private ManeuverGuidance(){}
}

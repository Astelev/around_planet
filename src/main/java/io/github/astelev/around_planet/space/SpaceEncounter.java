package io.github.astelev.around_planet.space;
/** Shared inertial encounter test. Distance never replaces the strict relative-speed gate. */
public final class SpaceEncounter {
    public static boolean eligible(SpaceState a,SpaceState b,double distance,double maxSpeed){return distance>0&&a.position().subtract(b.position()).length()<=distance&&a.velocity().subtract(b.velocity()).length()<maxSpeed;}
    public static SpaceState relative(SpaceState source,SpaceState host,SpaceFrame hostFrame){return new SpaceState(hostFrame.toLocal(source.position().subtract(host.position())),hostFrame.toLocal(source.velocity().subtract(host.velocity())));}
    private SpaceEncounter(){}
}

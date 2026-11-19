package io.github.astelev.around_planet.space;
import java.util.UUID;
/** Prescribed equatorial ephemeris. The Moon is never an independently integrated vessel. */
public record CelestialSystem(double earthRadius,double earthMu,long clockTicks,double earthEntry) {
    public CelestialSystem(double radius,double mu,long clock){this(radius,mu,clock,2000);}
    public static final String EARTH="around_planet:overworld",MOON="around_planet:moon";
    public static final UUID MOON_ID=UUID.fromString("00000000-0000-0000-0000-00000000a001"),EARTH_ID=UUID.fromString("00000000-0000-0000-0000-00000000a002");
    public CelestialSystem{if(!Double.isFinite(earthEntry)||earthEntry<0)throw new IllegalArgumentException("Earth entry");if(!(earthRadius>0)||!(earthMu>0)||!Double.isFinite(earthRadius+earthMu))throw new IllegalArgumentException("Celestial scale");}
    public boolean managed(String primary){return EARTH.equals(primary)||MOON.equals(primary);}
    public static boolean body(UUID id){return MOON_ID.equals(id)||EARTH_ID.equals(id);}
    public double moonRadius(){return earthRadius*.2724;}
    public double distance(){return earthRadius*12;}
    public double moonMu(){return earthMu*.0123;}
    public double soi(){return distance()*Math.pow(moonMu()/earthMu,.4);}
    public double motion(){return Math.sqrt(earthMu/Math.pow(distance(),3));}
    public double period(){return 2*Math.PI/motion();}
    public SpaceState moon(long tick){return moonSeconds(tick/20.0);}
    public SpaceState moonSeconds(double seconds){double a=Math.IEEEremainder(-(seconds+clockTicks/20.0)*motion(),2*Math.PI),c=Math.cos(a),s=Math.sin(a),v=distance()*motion();return new SpaceState(new SpaceVector(distance()*c,distance()*s,0),new SpaceVector(v*s,-v*c,0));}
    public double radius(String primary){return MOON.equals(primary)?moonRadius():earthRadius;}
    public double mu(String primary){return MOON.equals(primary)?moonMu():earthMu;}
    public SpaceState global(String primary,SpaceState state,long tick){if(!MOON.equals(primary))return state;var moon=moon(tick);return new SpaceState(state.position().add(moon.position()),state.velocity().add(moon.velocity()));}
    public SpaceState relative(String primary,SpaceState global,long tick){if(!MOON.equals(primary))return global;var moon=moon(tick);return new SpaceState(global.position().subtract(moon.position()),global.velocity().subtract(moon.velocity()));}
    public SpaceState target(UUID body,String primary,long tick){var absolute=MOON_ID.equals(body)?moon(tick):new SpaceState(SpaceVector.ZERO,SpaceVector.ZERO);return relative(primary,absolute,tick);}
    public CelestialSystem warp(long ticks){return new CelestialSystem(earthRadius,earthMu,Math.addExact(clockTicks,ticks),earthEntry);}
}

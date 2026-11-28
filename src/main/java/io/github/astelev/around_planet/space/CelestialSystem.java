package io.github.astelev.around_planet.space;
import java.util.UUID;
/** Prescribed circular ephemerides; every vessel remains relative to exactly one attracting body. */
public record CelestialSystem(double earthRadius,double earthMu,long clockTicks,double earthEntry,double yearSeconds) {
    public CelestialSystem(double radius,double mu,long clock,double entry){this(radius,mu,clock,entry,0);}
    public CelestialSystem(double radius,double mu,long clock){this(radius,mu,clock,2000);}
    public static final String SUN="around_planet:sun",EARTH="around_planet:overworld",MOON="around_planet:moon";
    public static final UUID MOON_ID=UUID.fromString("00000000-0000-0000-0000-00000000a001"),EARTH_ID=UUID.fromString("00000000-0000-0000-0000-00000000a002"),SUN_ID=UUID.fromString("00000000-0000-0000-0000-00000000a003");
    public CelestialSystem{if(!Double.isFinite(yearSeconds)||yearSeconds<0)throw new IllegalArgumentException("Earth year");if(!Double.isFinite(earthEntry)||earthEntry<0)throw new IllegalArgumentException("Earth entry");if(!(earthRadius>0)||!(earthMu>0)||!Double.isFinite(earthRadius+earthMu))throw new IllegalArgumentException("Celestial scale");}
    public boolean managed(String primary){return SUN.equals(primary)||EARTH.equals(primary)||MOON.equals(primary);}
    public static boolean body(UUID id){return MOON_ID.equals(id)||EARTH_ID.equals(id)||SUN_ID.equals(id);}
    public static String primary(UUID id){if(MOON_ID.equals(id))return MOON;if(EARTH_ID.equals(id))return EARTH;if(SUN_ID.equals(id))return SUN;throw new IllegalArgumentException("Unknown celestial body");}
    public static String name(UUID id){return MOON_ID.equals(id)?"Moon":SUN_ID.equals(id)?"Sun":"Earth";}
    public static String parent(String primary){return MOON.equals(primary)?EARTH:EARTH.equals(primary)?SUN:null;}
    public double sunRadius(){return earthDistance()*12/512;}
    public double sunMu(){return earthMu*1000;}
    public double earthDistance(){return yearSeconds==0?earthRadius*512:Math.cbrt(sunMu()*Math.pow(effectiveYearSeconds()/(2*Math.PI),2));}
    /** Smallest year that leaves the entire Moon sphere safely inside the Earth sphere. */
    public double minimumYearSeconds(){double d=(distance()+soi())*1.05/Math.pow(earthMu/sunMu(),.4);return 2*Math.PI*Math.sqrt(d*d*d/sunMu());}
    public double effectiveYearSeconds(){return yearSeconds==0?2*Math.PI*Math.sqrt(Math.pow(earthRadius*512,3)/sunMu()):Math.max(yearSeconds,minimumYearSeconds());}
    public double earthSoi(){return earthDistance()*Math.pow(earthMu/sunMu(),.4);}
    public double solarMotion(){return Math.sqrt(sunMu()/Math.pow(earthDistance(),3));}
    public double solarPeriod(){return 2*Math.PI/solarMotion();}
    public double solarAngle(double gameTicks){return Math.IEEEremainder((gameTicks+clockTicks)/20.0*solarMotion(),2*Math.PI);}
    public double solarDestructionRadius(){return sunRadius()*1.05;}
    public SpaceState earth(long tick){return earthSeconds(tick/20.0);}
    public SpaceState earthSeconds(double seconds){double a=Math.IEEEremainder((seconds+clockTicks/20.0)*solarMotion(),2*Math.PI),c=Math.cos(a),s=Math.sin(a),d=earthDistance(),v=d*solarMotion();return new SpaceState(new SpaceVector(-d*s,-d*c,0),new SpaceVector(-v*c,v*s,0));}
    public double moonRadius(){return MoonScale.fromEarthRadius(earthRadius).radius();}
    public double distance(){return earthRadius*12;}
    public double moonMu(){return earthMu*.0123;}
    public double soi(){return distance()*Math.pow(moonMu()/earthMu,.4);}
    public double sphere(String body){return MOON.equals(body)?soi():EARTH.equals(body)?earthSoi():Double.POSITIVE_INFINITY;}
    public double orbitRadius(String child){return MOON.equals(child)?distance():earthDistance();}
    public double motion(){return Math.sqrt(earthMu/Math.pow(distance(),3));}
    public double period(){return 2*Math.PI/motion();}
    /** Moon ephemeris relative to Earth, retained for old saves and lunar surface coordinates. */
    public SpaceState moon(long tick){return moonSeconds(tick/20.0);}
    public SpaceState moonSeconds(double seconds){double a=Math.IEEEremainder(-(seconds+clockTicks/20.0)*motion(),2*Math.PI),c=Math.cos(a),s=Math.sin(a),v=distance()*motion();return new SpaceState(new SpaceVector(distance()*c,distance()*s,0),new SpaceVector(v*s,-v*c,0));}
    public SpaceState childSeconds(String child,double seconds){return MOON.equals(child)?moonSeconds(seconds):earthSeconds(seconds);}
    public double radius(String primary){return SUN.equals(primary)?sunRadius():MOON.equals(primary)?moonRadius():earthRadius;}
    public double mu(String primary){return SUN.equals(primary)?sunMu():MOON.equals(primary)?moonMu():earthMu;}
    public SpaceState origin(String primary,long tick){if(SUN.equals(primary))return new SpaceState(SpaceVector.ZERO,SpaceVector.ZERO);var e=earth(tick);if(!MOON.equals(primary))return e;var m=moon(tick);return new SpaceState(e.position().add(m.position()),e.velocity().add(m.velocity()));}
    public SpaceState global(String primary,SpaceState state,long tick){if(!managed(primary))return state;var o=origin(primary,tick);return new SpaceState(state.position().add(o.position()),state.velocity().add(o.velocity()));}
    public SpaceState relative(String primary,SpaceState global,long tick){if(!managed(primary))return global;var o=origin(primary,tick);return new SpaceState(global.position().subtract(o.position()),global.velocity().subtract(o.velocity()));}
    public double targetMu(UUID body,String chart){return SUN_ID.equals(body)||SUN.equals(chart)?sunMu():earthMu;}
    public SpaceState target(UUID body,String primary,long tick){return relative(primary,origin(primary(body),tick),tick);}
    public SpaceVector sunlight(String primary,SpaceVector position,long tick){var p=global(primary,new SpaceState(position,SpaceVector.ZERO),tick).position().scale(-1);return p.length()>1e-12?p.scale(1/p.length()):SpaceEnvironment.SUN_DIRECTION;}
    public CelestialSystem warp(long ticks){return new CelestialSystem(earthRadius,earthMu,Math.addExact(clockTicks,ticks),earthEntry,yearSeconds);}
}

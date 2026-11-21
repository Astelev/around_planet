package io.github.astelev.around_planet.space;
/** Native conics are displayed around their primary's position at the catalog date. */
public final class OrbitChart {
    public static boolean visible(String source,String chart){return source.equals(chart)||CelestialSystem.SUN.equals(chart)||CelestialSystem.EARTH.equals(chart)&&CelestialSystem.MOON.equals(source);}
    public static boolean shared(CelestialSystem system,String a,String b){return a.equals(b)||system!=null&&system.managed(a)&&system.managed(b);}
    public static SpaceVector offset(CelestialSystem system,String source,String chart,long tick){if(system==null||source.equals(chart))return SpaceVector.ZERO;return system.origin(source,tick).position().subtract(system.origin(chart,tick).position());}
    public static OrbitPlot translate(OrbitPlot plot,SpaceVector offset){if(offset.length()==0)return plot;return new OrbitPlot(plot.points().stream().map(p->p.add(offset)).toList(),plot.periapsis()==null?null:plot.periapsis().add(offset),plot.apoapsis()==null?null:plot.apoapsis().add(offset),plot.period(),plot.reentry(),plot.open());}
    private OrbitChart(){}
}

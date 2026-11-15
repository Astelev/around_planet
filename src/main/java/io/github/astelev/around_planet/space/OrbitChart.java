package io.github.astelev.around_planet.space;
/** Native conics are displayed around their primary's position at the catalog date. */
public final class OrbitChart {
    public static boolean visible(String source,String chart){return source.equals(chart);}
    public static boolean shared(CelestialSystem system,String a,String b){return a.equals(b)||system!=null&&system.managed(a)&&system.managed(b);}
    public static SpaceVector offset(CelestialSystem system,String source,String chart,long tick){if(system==null||source.equals(chart))return SpaceVector.ZERO;var moon=system.moon(tick).position();return (CelestialSystem.MOON.equals(source)?moon:SpaceVector.ZERO).subtract(CelestialSystem.MOON.equals(chart)?moon:SpaceVector.ZERO);}
    public static OrbitPlot translate(OrbitPlot plot,SpaceVector offset){if(offset.length()==0)return plot;return new OrbitPlot(plot.points().stream().map(p->p.add(offset)).toList(),plot.periapsis()==null?null:plot.periapsis().add(offset),plot.apoapsis()==null?null:plot.apoapsis().add(offset),plot.period(),plot.reentry(),plot.open());}
    private OrbitChart(){}
}

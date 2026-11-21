package io.github.astelev.around_planet.space;
import java.util.*;
/** Present-date body surfaces in a selected primary chart; ephemerides are shared with flight. */
public final class OrbitSurfaces {
 public record Body(String primary,SpaceVector center,double radius,SpaceVector sunlight){}
 public static List<Body> scene(CelestialSystem system,String chart,long tick){
  if(system==null||!system.managed(chart))return List.of();
  var bodies=new ArrayList<Body>();
  for(var primary:List.of(CelestialSystem.SUN,CelestialSystem.EARTH,CelestialSystem.MOON))if(OrbitChart.visible(primary,chart))
   bodies.add(new Body(primary,OrbitChart.offset(system,primary,chart,tick),system.radius(primary),system.sunlight(primary,SpaceVector.ZERO,tick)));
  return List.copyOf(bodies);
 }
 public static List<OrbitOcclusion.Sphere> occluders(List<Body> bodies,OrbitCamera camera){return bodies.stream().map(b->new OrbitOcclusion.Sphere(camera.direction(b.center()),b.radius())).toList();}
 /** A celestial label sits on its own visible hemisphere, rather than being hidden by its center. */
 public static boolean markerVisible(SpaceVector point,List<Body> bodies,OrbitCamera camera){var p=camera.direction(point);for(var body:bodies){if(point.subtract(body.center()).length()<Math.max(1e-6,body.radius()*1e-9))continue;var center=camera.direction(body.center());if(!OrbitOcclusion.visible(new OrbitCamera.Point(p.x()-center.x(),p.y()-center.y(),p.depth()-center.depth()),body.radius()))return false;}return true;}
 private OrbitSurfaces(){}
}

package io.github.astelev.around_planet.space;
import java.util.*;
/** Analytic orthographic visibility against the opaque near surface, including subterranean conics. */
public final class OrbitOcclusion {
 public record Range(double from,double until){}
 public static boolean visible(OrbitCamera.Point p,double radius){double xy=p.x()*p.x()+p.y()*p.y();return xy>=radius*radius||p.depth()>=Math.sqrt(Math.max(0,radius*radius-xy))-1e-7;}
 public static List<Range> ranges(OrbitCamera.Point a,OrbitCamera.Point b,double radius,boolean behind){var cuts=new ArrayList<Double>(List.of(0.0,1.0));double x=a.x(),y=a.y(),z=a.depth(),dx=b.x()-x,dy=b.y()-y,dz=b.depth()-z;roots(cuts,dx*dx+dy*dy,2*(x*dx+y*dy),x*x+y*y-radius*radius);roots(cuts,dx*dx+dy*dy+dz*dz,2*(x*dx+y*dy+z*dz),x*x+y*y+z*z-radius*radius);if(Math.abs(dz)>1e-15)add(cuts,-z/dz);cuts.sort(Double::compare);var result=new ArrayList<Range>();for(int i=1;i<cuts.size();i++){double lo=cuts.get(i-1),hi=cuts.get(i),t=(lo+hi)/2;if(hi-lo<1e-12)continue;var p=new OrbitCamera.Point(x+t*dx,y+t*dy,z+t*dz);if((p.depth()<0)==behind&&visible(p,radius)){if(!result.isEmpty()&&Math.abs(result.getLast().until()-lo)<1e-12){var previous=result.removeLast();result.add(new Range(previous.from(),hi));}else result.add(new Range(lo,hi));}}return result;}
 public record Sphere(OrbitCamera.Point center,double radius){public Sphere{if(!(radius>0)||!Double.isFinite(radius))throw new IllegalArgumentException("Occluder radius");}}
 public static boolean visible(OrbitCamera.Point point,List<Sphere> spheres){for(var sphere:spheres){var c=sphere.center();if(!visible(new OrbitCamera.Point(point.x()-c.x(),point.y()-c.y(),point.depth()-c.depth()),sphere.radius()))return false;}return true;}
 /** Split against every projected circle and near surface before classifying each subsegment. */
 public static List<Range> ranges(OrbitCamera.Point a,OrbitCamera.Point b,List<Sphere> spheres,boolean behind){
  var cuts=new ArrayList<Double>(List.of(0.0,1.0));double dx=b.x()-a.x(),dy=b.y()-a.y(),dz=b.depth()-a.depth();
  for(var sphere:spheres){var c=sphere.center();double x=a.x()-c.x(),y=a.y()-c.y(),z=a.depth()-c.depth(),r=sphere.radius();roots(cuts,dx*dx+dy*dy,2*(x*dx+y*dy),x*x+y*y-r*r);roots(cuts,dx*dx+dy*dy+dz*dz,2*(x*dx+y*dy+z*dz),x*x+y*y+z*z-r*r);}
  if(Math.abs(dz)>1e-15)add(cuts,-a.depth()/dz);cuts.sort(Double::compare);var result=new ArrayList<Range>();
  for(int i=1;i<cuts.size();i++){double lo=cuts.get(i-1),hi=cuts.get(i),t=(lo+hi)/2;if(hi-lo<1e-12)continue;var p=new OrbitCamera.Point(a.x()+t*dx,a.y()+t*dy,a.depth()+t*dz);if((p.depth()<0)==behind&&visible(p,spheres)){if(!result.isEmpty()&&Math.abs(result.getLast().until()-lo)<1e-12){var previous=result.removeLast();result.add(new Range(previous.from(),hi));}else result.add(new Range(lo,hi));}}
  return result;
 }
 private static void roots(List<Double> cuts,double a,double b,double c){if(a<1e-25)return;double d=b*b-4*a*c;if(d<0)return;double q=Math.sqrt(d);add(cuts,(-b-q)/(2*a));add(cuts,(-b+q)/(2*a));}
 private static void add(List<Double> cuts,double t){if(t>0&&t<1&&Double.isFinite(t))cuts.add(t);}
 private OrbitOcclusion(){}
}

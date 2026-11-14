package io.github.astelev.around_planet.space;
/** Render-only curvature and altitude/sky policy, independent of physics and world generation. */
public final class SurfaceVisualMath {
    public static double drop(double x,double z,double radius){if(!(radius>0)||!Double.isFinite(radius))throw new IllegalArgumentException("Radius");return (x*x+z*z)/(2*radius);}
    public static double darkness(double altitude,double start,double end){double t=Math.clamp((altitude-start)/Math.max(1,end-start),0,1);return t*t*(3-2*t);}
    public static SpaceVector sun(double angle){return new SpaceVector(-Math.sin(angle),Math.cos(angle),0);}
    public static double horizon(double height,double radius){return -Math.sin(Math.atan(Math.sqrt(Math.max(0,2*(height-64)/radius))));}
    private SurfaceVisualMath(){}
}

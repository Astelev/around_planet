package io.github.astelev.around_planet.space;

/** Real seconds per solar rotation. Auto mode rounds the 3:1 orbital/spin condition upward to 5 minutes. */
public final class PlanetDayLength {
    public static double seconds(int circumference,double gravity,double atmosphereTop,double manualMinutes){
        if(manualMinutes>0)return Math.max(.05,manualMinutes*60);
        if(gravity<=0)return 1200;
        double radius=circumference/(2*Math.PI),distance=radius+Math.max(0,atmosphereTop);
        double circular=Math.sqrt(gravity*radius*radius/distance);
        double required=3*2*Math.PI*distance/circular;
        return Math.max(300,Math.ceil((required-1e-8)/300)*300);
    }
    private PlanetDayLength(){}
}

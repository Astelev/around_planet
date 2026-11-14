package io.github.astelev.around_planet.propulsion;
/** Bounded geometry for two crossed animated atlas sprites; no particle simulation. */
public final class ExhaustMath {
    public record Jet(float length,float halfWidth,int alpha){public boolean visible(){return length>0;}}
    private static final Jet OFF=new Jet(0,0,0);
    public static Jet jet(double thrust,int signal,boolean rsu,double phase){
        if(!Double.isFinite(thrust)||thrust<=0||signal<=0||!Double.isFinite(phase))return OFF;
        double throttle=Math.clamp(signal,0,15)/15.0;
        double intensity=Math.min(3,Math.sqrt(Math.min(thrust,10_000_000)*throttle/(rsu?1000:10000)));
        double pulse=.94+.06*Math.sin(phase);
        return new Jet((float)((rsu?.12+.32*intensity:.35+1.15*intensity)*pulse),(float)((rsu?.055:.14)*(.6+.4*Math.min(1.5,intensity))), (int)(255*(.25+.7*Math.min(1,intensity))));
    }
    private ExhaustMath(){}
}

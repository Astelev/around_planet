package io.github.astelev.around_planet.propulsion;
/** Compact active core followed by expanding gas. Positive thrust keeps a minimum gas length. */
public final class ExhaustMath {
    public record Jet(float activeLength,float coneLength,float halfWidth,int alpha,int coneAlpha){
        public float length(){return activeLength+coneLength;}
        public boolean visible(){return length()>0;}
    }
    private static final Jet OFF=new Jet(0,0,0,0,0);
    public static float minimumCone(boolean rsu){return rsu?.24f:.8f;}
    public static Jet jet(double thrust,int signal,boolean rsu,double phase){
        if(!Double.isFinite(thrust)||thrust<=0||signal<=0||!Double.isFinite(phase))return OFF;
        double throttle=Math.clamp(signal,0,15)/15.0;
        double intensity=Math.min(2,Math.sqrt(Math.min(thrust,10_000_000)*throttle/(rsu?1000:10000)));
        double pulse=.985+.015*Math.sin(phase),brightness=Math.min(1,intensity);
        return new Jet((float)((rsu?.18:.65)*intensity*pulse),(float)Math.max(minimumCone(rsu),(rsu?.22:.85)*intensity),rsu?.065f:.14f,(int)(120+120*brightness),(int)((28+70*brightness)*(.97+.03*Math.sin(phase+2))));
    }
    private ExhaustMath(){}
}

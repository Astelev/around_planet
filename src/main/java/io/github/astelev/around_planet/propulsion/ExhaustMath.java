package io.github.astelev.around_planet.propulsion;
/** Compact active core followed by expanding gas. Positive thrust keeps a minimum gas length. */
public final class ExhaustMath {
    public record Jet(float activeLength,float coneLength,float halfWidth,int alpha,int coneAlpha){
        public float length(){return activeLength+coneLength;}
        public boolean visible(){return length()>0;}
    }
    private static final Jet OFF=new Jet(0,0,0,0,0);
    public static float minimumCone(boolean rsu){return rsu?.72f:3.2f;}
    public static Jet jet(double thrust,int signal,boolean rsu,double phase){
        if(!Double.isFinite(thrust)||thrust<=0||signal<=0||!Double.isFinite(phase))return OFF;
        double throttle=Math.clamp(signal,0,15)/15.0;
        double intensity=Math.min(2,(rsu?2:1)*Math.sqrt(Math.min(thrust,rsu?PropulsionMath.RSU_MAX_THRUST:10_000_000)*throttle/(rsu?PropulsionMath.RSU_MAX_THRUST:10000)));
        double pulse=.985+.015*Math.sin(phase),brightness=Math.min(1,intensity);
        return new Jet((float)((rsu?.54:2.6)*intensity*pulse),(float)Math.max(minimumCone(rsu),(rsu?.66:3.4)*intensity),rsu?.072f:.16f,(int)(60+90*brightness),(int)((28+70*brightness)*(.97+.03*Math.sin(phase+2))));
    }
    /** Smooth pressure expansion: the active length is a transition centre, never a mesh seam. */
    public static float engineRadius(Jet jet,float distance){
        double y=Math.clamp(distance,0,jet.length()),length=jet.length();if(length<=0)return 0;
        double blend=Math.max(.15,length*.12),d=y-jet.activeLength(),start=-jet.activeLength();
        double expansion=.5*(d+Math.sqrt(d*d+blend*blend)-start-Math.sqrt(start*start+blend*blend));
        double t=y/length,tail=Math.clamp((t-.72)/.28,0,1);tail=tail*tail*(3-2*tail);
        return (float)(1.5*(jet.halfWidth()+.14*expansion)*(1-.55*tail));
    }
    private ExhaustMath(){}
}

package io.github.astelev.around_planet.propulsion;
/** Newtons, seconds and joules. FE cost is proportional to removed rotational energy. */
public final class PropulsionMath {
    public static final int CAPACITY=1_000_000;
    public static final double FE_PER_JOULE=.1;
    public record Brake(double factor,int cost){}
    public static double impulse(double thrust,int signal,double dt){return Double.isFinite(thrust)&&Double.isFinite(dt)&&thrust>0&&dt>0?Math.min(thrust,10_000_000)*Math.clamp(signal,0,15)/15.0*dt:0;}
    public static Brake brake(double energy,double strength,int signal,double dt,int available){
        if(!Double.isFinite(energy)||energy<1e-8||!Double.isFinite(dt)||dt<=0||!Double.isFinite(strength)||strength<=0||signal<=0||available<=0)return new Brake(1,0);
        double factor=Math.exp(-Math.min(strength,20)*Math.clamp(signal,0,15)/15.0*dt),removed=energy*(1-factor*factor);
        int requested=(int)Math.min(Integer.MAX_VALUE,Math.ceil(removed*FE_PER_JOULE));
        int cost=Math.min(available,Math.max(1,requested));
        if(cost<requested)factor=Math.sqrt(Math.max(0,1-cost/FE_PER_JOULE/energy));
        return new Brake(factor,cost);
    }
    private PropulsionMath(){}
}

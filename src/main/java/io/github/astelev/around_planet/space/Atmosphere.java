package io.github.astelev.around_planet.space;

/** Gameplay atmosphere in the nonrotating planet frame. Closed-form quadratic drag over one short step. */
public record Atmosphere(double top,double bottom,double drag,double handoffSpeed) {
    public Atmosphere {
        if(!Double.isFinite(top) || !Double.isFinite(bottom) || top<=bottom || bottom<0 || !Double.isFinite(drag) || drag<0 || !Double.isFinite(handoffSpeed) || handoffSpeed<=0)throw new IllegalArgumentException("Atmosphere settings");
    }
    /** Weak upper haze, then a dominant quadratic ramp over the final 500 blocks. */
    public double density(double altitude) {
        double upper=Math.clamp((top-altitude)/(top-bottom),0,1);
        double lower=Math.clamp((bottom+Math.min(500,top-bottom)-altitude)/Math.min(500,top-bottom),0,1);
        return .01*upper*upper+.99*lower*lower;
    }
    public SpaceVector damp(SpaceState state,double radius,double seconds) {
        if(!Double.isFinite(seconds) || seconds<0 || !(radius>0))throw new IllegalArgumentException("Atmosphere step");
        double altitude=state.position().length()-radius,speed=state.velocity().length();
        if(altitude>=top || speed<=handoffSpeed || state.position().dot(state.velocity())>=0 || seconds==0)return state.velocity();
        double next=speed/(1+drag*density(altitude)*speed*seconds);
        // Drag never creates acceleration and never turns the long approach into a slow settling phase.
        return state.velocity().scale(Math.max(handoffSpeed,next)/speed);
    }
    /** Final safety at the native-physics handoff, including skipped-shell steps/manual return. */
    public SpaceVector handoff(SpaceVector velocity) {
        double speed=velocity.length();return speed>handoffSpeed?velocity.scale(handoffSpeed/speed):velocity;
    }
}

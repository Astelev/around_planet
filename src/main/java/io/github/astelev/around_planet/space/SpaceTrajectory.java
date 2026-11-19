package io.github.astelev.around_planet.space;

import java.util.Objects;

/** Two-body/free-flight trajectory evaluated on demand. No tick callback and no mutable propagation state.
 * The attracting body is the origin of this trajectory's inertial chart. Surface crossings must be
 * handled by the future event scheduler; this point-mass model is not terrain collision physics.
 */
public record SpaceTrajectory(String primary,long epochTick,SpaceState initial,double mu) {
    public SpaceTrajectory {
        if(primary==null || primary.isBlank())throw new IllegalArgumentException("Missing primary");
        Objects.requireNonNull(initial);
        if(!Double.isFinite(mu) || mu<0)throw new IllegalArgumentException("Invalid gravitational parameter");
        if(mu>0 && !(initial.position().length()>0))throw new IllegalArgumentException("Orbit starts at attracting center");
    }
    public SpaceState at(long tick) {
        double dt=Math.subtractExact(tick,epochTick)/20.0;
        return afterSeconds(dt);
    }
    public SpaceState afterSeconds(double dt) {
        if(!Double.isFinite(dt))throw new IllegalArgumentException("Nonfinite elapsed time");
        if(dt==0)return initial;
        if(mu==0)return new SpaceState(initial.position().add(initial.velocity().scale(dt)),initial.velocity());
        return KeplerMotion.at(initial,mu,dt);
    }
    public SpaceTrajectory impulse(long tick,SpaceVector deltaVelocity) {
        var before=at(tick);
        return new SpaceTrajectory(primary,tick,new SpaceState(before.position(),before.velocity().add(deltaVelocity)),mu);
    }
}

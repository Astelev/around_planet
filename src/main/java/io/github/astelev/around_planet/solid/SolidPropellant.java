package io.github.astelev.around_planet.solid;

/** One charge consists of 64 mass-accounted portions. The burn itself remains continuous. */
public final class SolidPropellant {
    public static final double CAPACITY=64, FLOW=20;
    public static double exhaustSpeed(){return Math.sqrt(2*1_280_000*(io.github.astelev.around_planet.PlanetConfig.SPEC.isLoaded()?io.github.astelev.around_planet.PlanetConfig.FUEL_EFFICIENCY_MULTIPLIER.get():.25));}
    public static double valid(double n){return Double.isFinite(n)?Math.clamp(n,0,CAPACITY):0;}
    public static int portions(double remaining){return remaining<=1e-9?0:(int)Math.ceil(remaining-1e-9);}
    private SolidPropellant(){}
}

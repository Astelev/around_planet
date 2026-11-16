package io.github.astelev.around_planet.fuel;

import io.github.astelev.around_planet.PlanetConfig;

/** Server-synchronized mixture parameters, shared by combustion and the engine display. */
public final class RocketFuelParameters {
    private static volatile Bipropellant hydrolox=Bipropellant.HYDROLOX;
    public static Bipropellant hydrolox(){
        if(!PlanetConfig.SPEC.isLoaded())return Bipropellant.HYDROLOX;
        double energy=PlanetConfig.HYDROLOX_ENERGY.get()*PlanetConfig.FUEL_EFFICIENCY_MULTIPLIER.get(),efficiency=PlanetConfig.HYDROLOX_EFFICIENCY.get();
        var mixture=hydrolox;
        if(mixture.energyPerKg()!=energy||mixture.efficiency()!=efficiency){
            var base=Bipropellant.HYDROLOX;
            hydrolox=mixture=new Bipropellant(base.fuelKgPerMb(),base.oxidizerKgPerMb(),base.oxidizerToFuel(),energy,efficiency);
        }
        return mixture;
    }
    private RocketFuelParameters(){}
}

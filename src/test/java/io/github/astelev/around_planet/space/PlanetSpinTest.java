package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanetSpinTest {
    private final PlanetGeometry planet=new PlanetScale(131072).geometry();
    private static void close(SpaceVector a,SpaceVector b){assertTrue(a.subtract(b).length()<1e-8,a+" != "+b);}
    @Test void dawnNoonDuskAndTerminatorTravelWest(){
        for(int t:new int[]{0,6000,12000,18000,24000}){double noonX=planet.wrapX((6000-t)*131072d/24000);var spin=PlanetSpin.at(t,true);close(SpaceEnvironment.SUN_DIRECTION,spin.frame(planet,noonX,0).up());assertEquals(0,PlanetSpin.localTime(t,noonX,131072),1e-10);}
        var dawn=PlanetSpin.at(0,true).frame(planet,0,0).toLocal(SpaceEnvironment.SUN_DIRECTION);close(new SpaceVector(1,0,0),dawn);
    }
    @Test void oppositeLongitudesHaveOppositeDaylightAndPeriodicSeam(){assertEquals(0,PlanetSpin.skyDarken(PlanetSpin.localTime(6000,0,131072),0,0));assertEquals(11,PlanetSpin.skyDarken(PlanetSpin.localTime(6000,65536,131072),0,0));assertEquals(PlanetSpin.localTime(555,-65536,131072),PlanetSpin.localTime(555,65536,131072),1e-12);}
    @Test void chartRoundTripsAcrossPhasesLatitudesAndSeams(){for(int t:new int[]{0,6000,10000,18000})for(double x:new double[]{-65536,-40000,0,65535.9})for(double z:new double[]{-60000,0,45000}){var spin=PlanetSpin.at(t,true);var w=spin.surface(planet,spin.position(planet,x,2400,z));close(new SpaceVector(x,2400,z),w);}}
    @Test void spinVelocityIsDerivativeOfMovingGround(){double t=1234;var spin=PlanetSpin.at(t,true);var a=spin.position(planet,10000,3000,20000);var b=PlanetSpin.at(t+.00002,true).position(planet,10000,3000,20000);assertTrue(b.subtract(a).scale(1e6).subtract(spin.velocity(a)).length()<.001);}
    @Test void launchAndLandingDoNotInventRelativeVelocity(){for(int t:new int[]{0,6000,17000}){var spin=PlanetSpin.at(t,true);var w=new SpaceVector(60000,2208,-25000);var v=new SpaceVector(40,75,-15);var state=spin.launch(planet,w,v);close(v,spin.landingVelocity(planet,state.position(),state.velocity()));close(spin.velocity(state.position()),spin.launch(planet,w,SpaceVector.ZERO).velocity());}}
    @Test void sleepingAdvancesToLocalDawn(){assertEquals(6000,PlanetSpin.ticksUntilDawn(18000,0,131072));assertEquals(6000,PlanetSpin.ticksUntilDawn(6000,65536,131072));}
    @Test void stationaryClockHasNoSpinBoost(){var spin=PlanetSpin.at(6000,false);close(SpaceVector.ZERO,spin.velocity(new SpaceVector(100,200,300)));}
    @Test void defaultCircularSpeedExceedsRotationByThreeEvenAtAtmosphereTop(){assertTrue(PlanetSpin.circularToSpinRatio(planet.radius(),0,9.81)>4);assertTrue(PlanetSpin.circularToSpinRatio(planet.radius(),4000,9.81)>3);}
    @Test void atmosphereBrakesRelativeToMovingAirAndHandoffRetainsWind(){var spin=PlanetSpin.at(6000,true);var p=spin.position(planet,1000,2300,0);var wind=spin.velocity(p);var atmosphere=new Atmosphere(4000,2000,.012,50);var fast=new SpaceState(p,wind.add(p.scale(-200/p.length())));var next=spin.damp(atmosphere,fast,planet.radius(),.05);assertTrue(next.subtract(wind).length()<200);close(wind.add(p.scale(-50/p.length())),spin.handoff(atmosphere,fast));var coRotating=new SpaceState(p,wind);close(wind,spin.handoff(atmosphere,coRotating));close(wind,spin.damp(atmosphere,coRotating,planet.radius(),1));}
}

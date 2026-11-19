package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanetDayLengthTest {
    @Test void automaticRoundingMatchesWorldScaleExamples(){
        assertEquals(1200,PlanetDayLength.seconds(131072,9.81,4000,0));
        assertEquals(1500,PlanetDayLength.seconds(262144,9.81,4000,0));
        assertEquals(1800,PlanetDayLength.seconds(393216,9.81,4000,0));
        assertEquals(2100,PlanetDayLength.seconds(524288,9.81,4000,0));
    }
    @Test void automaticPeriodRetainsThreeToOneMarginAtAtmosphereTop(){
        for(int factor=1;factor<=16;factor++){
            double seconds=PlanetDayLength.seconds(131072*factor,9.81,4000,0);
            assertEquals(0,seconds%300);
            assertTrue(PlanetSpin.circularToSpinRatio(new PlanetScale(131072*factor).radius(),4000,9.81,seconds)>=3);
        }
    }
    @Test void manualOverrideDoesNotDependOnWorldSize(){
        for(int factor:new int[]{1,2,8})assertEquals(2250,PlanetDayLength.seconds(131072*factor,9.81,4000,37.5));
        assertEquals(1200,PlanetDayLength.seconds(131072,0,4000,0));
    }
    @Test void skySpinVelocitySleepAndWarpShareCustomPeriod(){
        double seconds=2400;var planet=new PlanetScale(131072).geometry();
        var a=PlanetSpin.at(1234,true,seconds);var b=PlanetSpin.at(1234+.00002,true,seconds);
        var p=a.position(planet,10000,3000,20000);
        assertTrue(b.position(planet,10000,3000,20000).subtract(p).scale(1e6).subtract(a.velocity(p)).length()<.001);
        assertEquals(PlanetSpin.at(1234+seconds*20,true,seconds).angle(),a.angle(),1e-12);
        assertEquals(12000,PlanetSpin.ticksUntilDawn(36000,0,131072,seconds));
        assertEquals(0,PlanetSpin.localTime(12000,0,131072,seconds),1e-12);
        assertEquals(Math.PI/2,PlanetSpin.at(12000,true,seconds).angle()-PlanetSpin.at(0,true,seconds).angle(),1e-12);
        var local=new SpaceVector(30,100,-5);var world=new SpaceVector(6000,2208,1000);
        var state=a.launch(planet,world,local);
        assertTrue(local.subtract(a.landingVelocity(planet,state.position(),state.velocity())).length()<1e-8);
    }
}

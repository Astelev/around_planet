package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AtmosphereTest {
    private final Atmosphere atmosphere=new Atmosphere(6000,2968,.02,20);
    private SpaceState state(double altitude,double vy){return new SpaceState(new SpaceVector(0,20000+altitude,0),new SpaceVector(30,vy,4));}
    @Test void vacuumAndOutboundCoastRemainUnchanged(){for(var s:new SpaceState[]{state(6500,-200),state(4000,200)})assertEquals(s.velocity(),atmosphere.damp(s,20000,1));}
    @Test void densityIncreasesGraduallyAndDragNeverReversesVelocity(){var high=state(5900,-500);var low=state(3100,-500);var vh=atmosphere.damp(high,20000,.05);var vl=atmosphere.damp(low,20000,.05);assertTrue(vl.length()<vh.length());assertTrue(vh.length()<high.velocity().length());assertTrue(vl.dot(low.velocity())>0);assertTrue(vl.cross(low.velocity()).length()<1e-8);}
    @Test void quadraticDecayIsStableAndComposesAtConstantDensity(){var s=state(4000,-1000);var a=atmosphere.damp(s,20000,1);var b=atmosphere.damp(s,20000,.5);b=atmosphere.damp(new SpaceState(s.position(),b),20000,.5);assertTrue(a.subtract(b).length()<1e-10);}
    @Test void skippedShellAndManualReturnHaveBoundedNativeHandoff(){var v=new SpaceVector(300,-1000000,50);var capped=atmosphere.handoff(v);assertEquals(20,capped.length(),1e-9);assertTrue(capped.cross(v).length()<1e-6);assertEquals(SpaceVector.ZERO,atmosphere.handoff(SpaceVector.ZERO));assertEquals(new SpaceVector(1,-2,3),atmosphere.handoff(new SpaceVector(1,-2,3)));}
    @Test void inboundKeplerFlightSlowsBeforeReachingNativeWorld(){var model=new Atmosphere(4000,2000,.012,50);double radius=20000,mu=9.81*radius*radius;var s=state(4000,-1200);int ticks=0;while(s.position().length()-radius>2000 && ticks<20000){s=KeplerMotion.at(s,mu,.05);s=new SpaceState(s.position(),model.damp(s,radius,.05));ticks++;}assertTrue(ticks<20000,"Entry never reached the lower shell");assertTrue(s.velocity().length()<250 && s.velocity().length()>=50,"Atmosphere failed to brake before handoff: "+s.velocity().length());assertTrue(ticks<800,"Atmosphere made the approach too slow");}
    @Test void newProfileRetainsSpeedAbove2500AndBrakesMostlyInFinal500() {
        var a=new Atmosphere(4000,2000,.012,50);var s=new SpaceState(new SpaceVector(0,24000,0),new SpaceVector(0,-500,0));double mid=0;int ticks=0;
        while(s.position().y()>22000 && ticks++<2000){s=new SpaceTrajectory("test",0,s,0).afterSeconds(.01);s=new SpaceState(s.position(),a.damp(s,20000,.01));if(mid==0 && s.position().y()<=22500)mid=s.velocity().length();}
        assertTrue(mid>450,"Upper layer removed orbital speed too early: "+mid);
        assertTrue(mid-s.velocity().length()>500-mid,"Most braking happened before the last 500 blocks");
        assertTrue(s.velocity().length()>=50 && ticks<1500,"Approach became viscous");assertEquals(50,a.handoff(s.velocity()).length(),1e-8);
    }
    @Test void dragDoesNotForceSlowObjectsToSpeedUpOrFastObjectsBelowHandoffSpeed(){var a=new Atmosphere(4000,2000,.012,50);var slow=new SpaceState(new SpaceVector(0,22001,0),new SpaceVector(0,-10,0));assertEquals(slow.velocity(),a.damp(slow,20000,1));var fast=new SpaceState(slow.position(),new SpaceVector(0,-1000,0));assertEquals(50,a.damp(fast,20000,1000).length(),1e-8);}
    @Test void invalidSettingsAndStepsAreRejected(){assertThrows(IllegalArgumentException.class,()->new Atmosphere(1000,2000,.02,20));assertThrows(IllegalArgumentException.class,()->new Atmosphere(6000,2968,-1,20));assertThrows(IllegalArgumentException.class,()->atmosphere.damp(state(4000,-100),20000,-1));}
}

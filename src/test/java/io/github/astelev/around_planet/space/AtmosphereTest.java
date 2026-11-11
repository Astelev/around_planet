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
    @Test void inboundKeplerFlightSlowsBeforeReachingNativeWorld(){double radius=20000,mu=9.81*radius*radius;var s=state(6000,-1200);int ticks=0;while(s.position().length()-radius>2968 && ticks<20000){s=KeplerMotion.at(s,mu,.05);s=new SpaceState(s.position(),atmosphere.damp(s,radius,.05));ticks++;}assertTrue(ticks<20000,"Entry never reached the lower shell");assertTrue(s.velocity().length()<30,"Atmosphere failed to brake before handoff: "+s.velocity().length());assertTrue(ticks>100,"Atmosphere did not extend the approach");}
    @Test void invalidSettingsAndStepsAreRejected(){assertThrows(IllegalArgumentException.class,()->new Atmosphere(1000,2000,.02,20));assertThrows(IllegalArgumentException.class,()->new Atmosphere(6000,2968,-1,20));assertThrows(IllegalArgumentException.class,()->atmosphere.damp(state(4000,-100),20000,-1));}
}

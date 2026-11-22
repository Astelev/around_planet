package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ManeuverGuidanceTest {
    @Test void residualBurnIncludesCrossTrackErrorAndUsesUpdatedOrbitalFrame(){
        var before=new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,300,0));
        var node=new ManeuverNode(UUID.randomUUID(),1000,20,0,0,10);
        var impulse=new SpaceVector(0,8,3);
        var after=new SpaceState(new SpaceVector(29000,1000,0),before.velocity().add(impulse));
        var remaining=node.afterImpulse(before,after,impulse);
        var expected=node.delta(before).subtract(impulse);
        assertEquals(Math.hypot(12,3),remaining.magnitude(),1e-10);
        assertTrue(remaining.delta(after).subtract(expected).length()<1e-10);
        assertEquals(node.id(),remaining.id());assertEquals(10,remaining.warningSeconds());
    }
    @Test void completedBurnStopsBeingTheNextGuidanceTarget(){
        var state=new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,300,0));
        var node=new ManeuverNode(UUID.randomUUID(),1000,20,0,0,10);
        var finished=node.afterImpulse(state,state,node.delta(state));
        assertEquals(0,finished.magnitude());assertTrue(ManeuverGuidance.next(List.of(finished),900).isEmpty());
    }
    @Test void nearestUpcomingIgnoresPassedNodesAndUsesPlannedDeltaAtBurn(){var past=new ManeuverNode(UUID.randomUUID(),5,1,0,0);var first=new ManeuverNode(UUID.randomUUID(),1000,3,4,0);var later=new ManeuverNode(UUID.randomUUID(),2000,0,0,5);var orbit=new SpaceTrajectory("test",0,new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,300,0)),4e9);assertEquals(first,ManeuverGuidance.next(List.of(later,past,first),10).orElseThrow());var expected=first.delta(orbit.at(first.tick()));assertTrue(expected.scale(1/expected.length()).subtract(ManeuverGuidance.direction(orbit,first).orElseThrow()).length()<1e-12);assertTrue(ManeuverGuidance.next(List.of(past),10).isEmpty());}
    @Test void warningStartsAtBoundaryStopsAfterNodeAndSupportsLongIntervals(){assertFalse(ManeuverGuidance.warning(100,701,30));assertTrue(ManeuverGuidance.warning(101,701,30));assertFalse(ManeuverGuidance.warning(701,701,30));assertFalse(ManeuverGuidance.warning(702,701,30));assertFalse(ManeuverGuidance.warning(700,701,0));assertTrue(ManeuverGuidance.warning(0,200000,Integer.MAX_VALUE));}
    @Test void apsisDatesAndVelocitiesMatchTheirGeometry(){double mu=4e9,r=30000,a=200000;var path=new SpaceTrajectory("test",0,new SpaceState(new SpaceVector(r,0,0),new SpaceVector(0,Math.sqrt(mu*(2/r-1/a)),0)),mu);var extrema=OrbitInspection.apsides(path,100,20000,2200,true);assertEquals(2,extrema.size());for(var apsis:extrema){assertTrue(apsis.tick()>100);var state=path.at(apsis.tick());assertTrue(state.position().subtract(apsis.position()).length()<state.velocity().length()*.06+.1);assertEquals(mu*(2/apsis.position().length()-1/a),state.velocity().dot(state.velocity()),.05);}}
    @Test void incomingEscapeOrbitHasOneFuturePeriapsis(){var path=new SpaceTrajectory("escape",0,new SpaceState(new SpaceVector(100000,0,0),new SpaceVector(-300,350,0)),4e9);var extrema=OrbitInspection.apsides(path,0,20000,2200,true);assertEquals(1,extrema.size());var apsis=extrema.getFirst();assertFalse(apsis.apo());assertTrue(apsis.tick()>0);var state=path.at(apsis.tick());assertTrue(state.position().subtract(apsis.position()).length()<state.velocity().length()*.06+.1);}
    @Test void individualWarningsOverrideLegacyPlannerDefault(){var old=new ManeuverNode(UUID.randomUUID(),1000,1,0,0);var shortBurn=new ManeuverNode(UUID.randomUUID(),1000,1,0,0,5);var longBurn=new ManeuverNode(UUID.randomUUID(),1000,1,0,0,45);assertEquals(30,old.warningSeconds(30));assertFalse(ManeuverGuidance.warning(800,shortBurn.tick(),shortBurn.warningSeconds(30)));assertTrue(ManeuverGuidance.warning(800,longBurn.tick(),longBurn.warningSeconds(30)));assertThrows(IllegalArgumentException.class,()->new ManeuverNode(UUID.randomUUID(),1000,0,0,0,-2));}
    @Test void fractionalWarningsProduceExactlyTheRequestedNumberOfPoweredTicks(){for(double seconds:new double[]{0,.05,.1,.15,.35,1,1.25}){int powered=0;for(long now=0;now<=101;now++)if(ManeuverGuidance.warning(now,100,seconds))powered++;assertEquals(Math.round(seconds*20),powered,"Pulse length for "+seconds);}}
    @Test void warningDurationsRoundToTicksAndRejectInvalidNumbers(){assertEquals(1,WarningDuration.ticks(.05));assertEquals(7,WarningDuration.ticks(.35));assertEquals(.35,new ManeuverNode(UUID.randomUUID(),100,0,0,0,.36).warningSeconds());assertEquals(0,WarningDuration.ticks(.01));for(double v:new double[]{-.01,Double.NaN,Double.POSITIVE_INFINITY,1e15})assertThrows(IllegalArgumentException.class,()->WarningDuration.ticks(v));assertEquals(-1,WarningDuration.seconds(-1));assertThrows(IllegalArgumentException.class,()->WarningDuration.seconds(-2));}
}

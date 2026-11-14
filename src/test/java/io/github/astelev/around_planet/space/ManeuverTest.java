package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ManeuverTest {
    private final SpaceState circular=new SpaceState(new SpaceVector(100,0,0),new SpaceVector(0,10,0));
    private ManeuverNode node(long tick,double p,double n,double r){return new ManeuverNode(UUID.randomUUID(),tick,p,n,r);}
    @Test void signedAxesAreOrthogonalAndFollowTheOrbit(){assertEquals(new SpaceVector(0,2,0),node(20,2,0,0).delta(circular));assertEquals(new SpaceVector(0,0,-3),node(20,0,-3,0).delta(circular));assertEquals(new SpaceVector(-4,0,0),node(20,0,0,-4).delta(circular));assertEquals(5,node(20,3,4,0).magnitude());}
    @Test void progradeRaisesTheApoapsisAndNormalTiltsThePlane(){var orbit=new SpaceTrajectory("test",0,circular,10000);var boosted=orbit.impulse(0,node(0,1,0,0).delta(circular));var plot=OrbitPlot.create(boosted.initial(),10000,50,0,false);assertTrue(plot.apoapsis().length()>100);var tilted=orbit.impulse(0,node(0,0,1,0).delta(circular));assertTrue(Math.abs(tilted.initial().position().cross(tilted.initial().velocity()).y())>0);assertEquals(circular,orbit.initial());}
    @Test void laterNodesUseTheTrajectoryAfterEarlierBurns(){var orbit=new SpaceTrajectory("test",0,new SpaceState(new SpaceVector(1000,0,0),new SpaceVector(0,10,0)),0);var a=node(200,10,0,0);var b=node(400,10,0,0);var preview=ManeuverPreview.create(orbit,0,List.of(b,a),100,0);assertEquals(2,preview.nodes().size());assertEquals(new SpaceVector(1000,100,0),preview.nodes().get(0).position());assertEquals(new SpaceVector(1000,300,0),preview.nodes().get(1).position());assertEquals(new SpaceVector(0,10,0),orbit.initial().velocity());}
    @Test void pastNodesNeverBecomeRealOrPredictedBurns(){var orbit=new SpaceTrajectory("test",0,circular,10000);assertTrue(ManeuverPreview.create(orbit,200,List.of(node(100,100,0,0)),50,0).nodes().isEmpty());assertEquals(circular,orbit.at(0));}
    @Test void invalidDeltaVAndTimeAreRejected(){assertThrows(IllegalArgumentException.class,()->node(20,Double.NaN,0,0));assertThrows(IllegalArgumentException.class,()->node(20,10001,0,0));assertThrows(IllegalArgumentException.class,()->node(-1,0,0,0));}
    @Test void radialAndStoppedFramesStayFinite(){for(var state:List.of(new SpaceState(new SpaceVector(1000,0,0),SpaceVector.ZERO),new SpaceState(new SpaceVector(0,1000,0),new SpaceVector(0,20,0))))assertEquals(Math.sqrt(14),node(100,1,2,3).delta(state).length(),1e-9);}
    @Test void boundPostBurnForecastClosesBeyondTheOldTwoHourWindow(){
        double mu=9.81*20000*20000,r=30000,apo=2000000,a=(r+apo)/2;var state=new SpaceState(new SpaceVector(r,0,0),new SpaceVector(0,Math.sqrt(mu/r),0));var base=new SpaceTrajectory("planet",0,state,mu);
        double burn=Math.sqrt(mu*(2/r-1/a))-state.velocity().length();var preview=ManeuverPreview.create(base,0,List.of(node(400,burn,0,0)),20000,2200);var curve=preview.segments().getLast();assertTrue(curve.planned());assertTrue(curve.points().getLast().tick()-curve.points().getFirst().tick()>168000);assertTrue(curve.points().getFirst().position().subtract(curve.points().getLast().position()).length()<1e-6);assertTrue(curve.points().stream().mapToDouble(p->p.position().length()).max().orElseThrow()>apo*.9999);
    }
    @Test void distantEllipsePointsHaveAccurateDatesAndAcceptLaterBurns(){
        double mu=9.81*20000*20000,r=30000,a=800000;var base=new SpaceTrajectory("planet",0,new SpaceState(new SpaceVector(r,0,0),new SpaceVector(0,Math.sqrt(mu*(2/r-1/a)),0)),mu);
        var initial=ManeuverPreview.create(base,0,List.of(),20000,2200);var points=initial.segments().getFirst().points();var far=points.get(points.size()/2);assertTrue(far.tick()>168000);assertTrue(far.position().subtract(base.at(far.tick()).position()).length()<base.at(far.tick()).velocity().length()*.06+.1);
        var preview=ManeuverPreview.create(base,0,List.of(node(far.tick(),1,0,0)),20000,2200);assertEquals(1,preview.nodes().size());assertTrue(preview.nodes().getFirst().position().subtract(base.at(far.tick()).position()).length()<1e-6);
    }

}

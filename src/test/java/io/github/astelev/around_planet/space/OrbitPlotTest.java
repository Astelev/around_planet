package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrbitPlotTest {
    private final double radius=20000,mu=9.81*radius*radius;
    private SpaceState circular(double r){return new SpaceState(new SpaceVector(r,0,0),new SpaceVector(0,Math.sqrt(mu/r),0));}
    @Test void circularPlotClosesAndMatchesKeplerAtQuarterOrbit(){
        var state=circular(25000);var plot=OrbitPlot.create(state,mu,radius,736,false);
        assertFalse(plot.open());assertFalse(plot.reentry());assertEquals(385,plot.points().size());
        assertTrue(plot.points().getFirst().subtract(plot.points().getLast()).length()<1e-6);
        for(var p:plot.points())assertEquals(25000,p.length(),1e-6);
        var atQuarter=new SpaceTrajectory("planet",0,state,mu).at(Math.round(plot.period()*5));
        assertTrue(atQuarter.position().subtract(plot.points().get(96)).length()<10);
    }
    @Test void suborbitalFutureStopsAtReturnShellNotPlanetCenter(){
        double apo=30000,peri=19500,a=(apo+peri)/2;
        var state=new SpaceState(new SpaceVector(apo,0,0),new SpaceVector(0,Math.sqrt(mu*(2/apo-1/a)),0));
        var plot=OrbitPlot.create(state,mu,radius,736,false);
        assertTrue(plot.reentry());assertEquals(radius+736,plot.points().getLast().length(),1e-7);
        for(var p:plot.points())assertTrue(p.length()>=radius+736-1e-7);
        assertEquals(peri,plot.periapsis().length(),1e-7);assertEquals(apo,plot.apoapsis().length(),1e-7);
    }
    @Test void hyperbolicTrajectoryHasNoFakeApoapsisOrClosedLoop(){
        var state=new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,1.5*Math.sqrt(2*mu/30000),0));
        var plot=OrbitPlot.create(state,mu,radius,736,false);assertTrue(plot.open());assertNull(plot.apoapsis());assertFalse(plot.reentry());
        assertTrue(plot.points().getLast().length()>state.position().length());assertTrue(plot.points().size()<=385);
    }
    @Test void radialMotionAndZeroGravityProduceFiniteUsefulPaths(){
        var inward=new SpaceState(new SpaceVector(0,22000,0),new SpaceVector(0,-10,0));
        for(double gravity:new double[]{0,mu}){var plot=OrbitPlot.create(inward,gravity,radius,736,false);assertTrue(plot.reentry());assertEquals(radius+736,plot.points().getLast().length(),1e-7);}
        var outward=OrbitPlot.create(new SpaceState(inward.position(),new SpaceVector(0,10,0)),mu,radius,736,false);
        assertTrue(outward.reentry());assertNotNull(outward.apoapsis());assertTrue(outward.apoapsis().length()>22000);
        var line=OrbitPlot.create(new SpaceState(inward.position(),new SpaceVector(10,0,0)),0,radius,736,false);assertTrue(line.open());assertFalse(line.reentry());
    }
    @Test void nearParabolicAndWaitingCasesDoNotCreateNonfiniteVertices(){
        for(double k:new double[]{1-1e-14,1,1+1e-14}){var plot=OrbitPlot.create(new SpaceState(new SpaceVector(30000,0,0),new SpaceVector(0,k*Math.sqrt(2*mu/30000),0)),mu,radius,736,false);for(var p:plot.points())assertTrue(Double.isFinite(p.length()));}
        assertEquals(1,OrbitPlot.create(circular(25000),mu,radius,736,true).points().size());
    }
    @Test void ellipticalVerticesLieOnTheOriginalOrbitalPlaneAndConic(){
        var state=new SpaceState(new SpaceVector(28000,100,500),new SpaceVector(20,300,60));var h=state.position().cross(state.velocity());
        var e=state.velocity().cross(h).scale(1/mu).subtract(state.position().scale(1/state.position().length()));double p=h.dot(h)/mu;
        for(var point:OrbitPlot.create(state,mu,radius,736,false).points()){assertEquals(0,point.dot(h)/h.length(),1e-6);assertEquals(p,point.length()+point.dot(e),1e-6);}
    }
}

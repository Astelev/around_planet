package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PlanetGeometryTest {
    private final PlanetGeometry p=new PlanetGeometry(65536,65536);
    private static void near(SpaceVector a,SpaceVector b,double eps){assertTrue(a.subtract(b).length()<eps,a+" != "+b);}
    @Test void longitudeWrapsAndWorldChartRoundTrips(){
        for(double x:new double[]{-65535,-30000,0,45000,65535})for(double z:new double[]{-65000,-10000,0,65000}){
            var local=new SpaceVector(x,768,z);near(local,p.surface(p.position(x,768,z)),1e-6);
            near(p.position(x,768,z),p.position(x+131072,768,z),1e-8);
        }
    }
    @Test void seamCoordinatesDescribeTheSamePoint(){near(p.position(-65536,768,55),p.position(65536,768,55),1e-8);}
    @Test void entryBasisIsProperAndPreservesVelocityAndHandedness(){
        for(int x=-60000;x<65536;x+=20000){var f=p.frame(x,12345);var v=new SpaceVector(31,-7,17);near(v,f.toLocal(f.toCosmic(v)),1e-10);near(f.south(),f.east().cross(f.up()),1e-10);}
    }
    @Test void entirePlanetCanBeCrossedInOneStepWithoutMissingEntry(){
        var a=p.position(0,2000,0);var b=a.scale(-1);var hit=p.inwardCrossing(a,b,736).orElseThrow();assertEquals(p.radius()+736,hit.length(),1e-8);assertTrue(hit.y()>0);
    }
    @Test void outboundAndGrazingPathsDoNotReenter(){
        assertTrue(p.inwardCrossing(p.position(0,736,0),p.position(0,900,0),736).isEmpty());
        double r=p.radius()+736;assertTrue(p.inwardCrossing(new SpaceVector(-100,r+1,0),new SpaceVector(100,r+1,0),736).isEmpty());
    }
    @Test void gravityBringsASuborbitalLaunchBackThroughTheReturnShell(){
        var f=p.frame(1000,500);var orbit=new SpaceTrajectory("planet",0,new SpaceState(p.position(1000,768,500),f.toCosmic(new SpaceVector(10,20,0))),9.81*p.radius()*p.radius());
        SpaceVector previous=orbit.initial().position();boolean entered=false;
        for(int tick=1;tick<300;tick++){var state=orbit.at(tick);if(p.inwardCrossing(previous,state.position(),736).isPresent()){assertTrue(state.position().dot(state.velocity())<0);entered=true;break;}previous=state.position();}
        assertTrue(entered,"Suborbital coast never returned");
    }
    @Test void poleAdjacentBasisRemainsFinite(){for(double z:new double[]{-65536,65536}){var f=p.frame(8,z);assertEquals(1,f.up().length(),1e-12);assertTrue(Double.isFinite(p.surface(p.position(8,768,z)).z()));}}
}

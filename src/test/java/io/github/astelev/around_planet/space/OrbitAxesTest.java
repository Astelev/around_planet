package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrbitAxesTest {
    @Test void inertialAxesKeepTheirSignedDirectionsInDefaultOrbitalView(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,0,0),new SpaceVector(0,10,0)),100);assertTrue(c.direction(OrbitAxes.X.vector).x()>.99);assertTrue(c.direction(OrbitAxes.Y.vector).y()<-.99);assertTrue(c.direction(OrbitAxes.Z.vector).depth()>.99);}
    @Test void compassTracksCameraRotationButNotPanOrZoom(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,20,0),new SpaceVector(-2,10,3)),100);c.rotate(70,40);var before=c.direction(OrbitAxes.Z.vector);c.panX=1000;c.panY=-300;c.zoom(10);assertEquals(before,c.direction(OrbitAxes.Z.vector));var a=c.project(OrbitAxes.Z.vector,0,0,400);assertEquals(before.x(),(a.x()-c.panX)/c.scale(400),1e-10);assertEquals(before.y(),(a.y()-c.panY)/c.scale(400),1e-10);}
    @Test void globalAxesRemainOrthonormalUnderArbitraryViewAndReset(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(5,100,40),new SpaceVector(3,8,20)),200);c.rotate(180,80);for(var axis:OrbitAxes.values()){var d=c.direction(axis.vector);assertEquals(1,d.x()*d.x()+d.y()*d.y()+d.depth()*d.depth(),1e-12);var minus=c.direction(axis.vector.scale(-1));assertEquals(-d.x(),minus.x(),1e-12);assertEquals(-d.y(),minus.y(),1e-12);assertEquals(-d.depth(),minus.depth(),1e-12);}}
}

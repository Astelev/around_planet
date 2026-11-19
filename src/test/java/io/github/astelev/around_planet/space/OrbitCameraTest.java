package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrbitCameraTest {
    @Test void horizontalDragRotatesAroundGlobalZForAnyFittedOrbit(){
        for(var state:new SpaceState[]{new SpaceState(new SpaceVector(100,20,30),new SpaceVector(0,10,5)),new SpaceState(new SpaceVector(0,0,100),new SpaceVector(5,3,0))}){
            var camera=new OrbitCamera();camera.fit(state,200);var pole=new SpaceVector(0,0,100);
            var before=camera.direction(pole);camera.rotate(170,0);var after=camera.direction(pole);
            assertEquals(before.x(),after.x(),1e-10);assertEquals(before.y(),after.y(),1e-10);assertEquals(before.depth(),after.depth(),1e-10);
            var v=new SpaceVector(11,22,33);var screen=camera.direction(v);
            assertTrue(camera.fromView(screen.x(),-screen.y(),screen.depth()).subtract(v).length()<1e-10);
        }
    }
    @Test void cameraUsesPlanetAxisAndRotationPreservesProjectedLength(){
        var c=new OrbitCamera();var p=new SpaceVector(100,20,30);c.fit(new SpaceState(p,new SpaceVector(0,10,0)),200);
        var a=c.project(p,0,0,400);assertTrue(Double.isFinite(a.depth()));
        c.rotate(70,30);var b=c.project(p,0,0,400);double s=c.scale(400);assertEquals(p.length()*p.length(),(b.x()*b.x()+b.y()*b.y())/(s*s)+b.depth()*b.depth(),1e-6);
    }
    @Test void zoomIsBoundedAndResetRestoresUsefulView(){var c=new OrbitCamera();for(int i=0;i<100;i++)c.zoom(100);assertEquals(100000000,c.zoom);for(int i=0;i<100;i++)c.zoom(-100);assertEquals(.000001,c.zoom);c.fit(new SpaceState(new SpaceVector(0,100,0),SpaceVector.ZERO),100);assertEquals(1,c.zoom);assertTrue(Double.isFinite(c.project(new SpaceVector(10,20,30),0,0,200).x()));}
    @Test void clippingHandlesHugeLinesAndFullyHiddenSegments(){assertArrayEquals(new double[]{0,50,100,50},OrbitCamera.clip(-1e9,50,1e9,50,0,0,100,100),1e-6);assertNull(OrbitCamera.clip(-20,-20,-10,-10,0,0,100,100));assertArrayEquals(new double[]{30,20,30,80},OrbitCamera.clip(30,20,30,80,0,0,100,100));}
    @Test void draggingMovesTheWorldPivotForBothRotationAndZoom(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,0,0),new SpaceVector(0,10,0)),200);var before=c.project(SpaceVector.ZERO,300,200,400);c.pan(80,-45,400);var after=c.project(SpaceVector.ZERO,300,200,400);assertEquals(before.x()+80,after.x(),1e-10);assertEquals(before.y()-45,after.y(),1e-10);var pivot=c.focus;c.rotate(70,40);c.zoom(5);var centered=c.project(pivot,300,200,400);assertEquals(300,centered.x(),1e-10);assertEquals(200,centered.y(),1e-10);}
    @Test void followingAMovingSpaceKeepsItsPositionCenteredAndResetRestoresPlanet(){var c=new OrbitCamera();c.fit(new SpaceState(new SpaceVector(100,20,0),new SpaceVector(0,10,0)),200);c.focus=new SpaceVector(100,20,0);c.rotate(200,-50);c.zoom(3);var p=c.project(c.focus,200,150,400);assertEquals(200,p.x(),1e-10);assertEquals(150,p.y(),1e-10);c.focus=new SpaceVector(103,25,1);p=c.project(c.focus,200,150,400);assertEquals(200,p.x(),1e-10);assertEquals(150,p.y(),1e-10);c.fit(new SpaceState(c.focus,new SpaceVector(0,10,0)),200);assertEquals(SpaceVector.ZERO,c.focus);}
}

package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NavballMathTest {
    @Test void frontRightAndUpMapToInstrumentAxes(){var f=new SpaceVector(0,0,-1);assertEquals(new SpaceVector(0,0,1),NavballMath.instrument(f,f));assertEquals(new SpaceVector(1,0,0),NavballMath.instrument(new SpaceVector(1,0,0),f));assertEquals(new SpaceVector(0,1,0),NavballMath.instrument(new SpaceVector(0,1,0),f));}
    @Test void targetProjectionDistinguishesRearAndRespectsAngles(){assertEquals(1,NavballMath.fromAngles(0,0).z(),1e-12);assertTrue(NavballMath.fromAngles(180,0).z()<0);assertEquals(1,NavballMath.fromAngles(90,0).x(),1e-12);assertEquals(1,NavballMath.fromAngles(0,90).y(),1e-12);}
    @Test void sphereHasTransparentExteriorAndRollMovesTheHorizon(){assertEquals(0,NavballMath.color(1,1,new SpaceVector(0,1,0)));assertNotEquals(NavballMath.color(0,.5,new SpaceVector(0,1,0)),NavballMath.color(0,-.5,new SpaceVector(0,1,0)));assertEquals(NavballMath.color(0,.5,new SpaceVector(0,1,0)),NavballMath.color(.5,0,new SpaceVector(1,0,0)));}
    @Test void mirroredHorizonReversesRollButPreservesPitch(){var up=new SpaceVector(.6,.7,.3);var mirrored=NavballMath.horizonUp(up);assertEquals(-up.x(),mirrored.x());assertEquals(up.y(),mirrored.y());assertEquals(up.z(),mirrored.z());for(double x:new double[]{-.6,-.2,.2,.6})for(double y:new double[]{-.4,0,.4})assertEquals(NavballMath.color(-x,y,up),NavballMath.color(x,y,mirrored));assertNotEquals(NavballMath.color(.6,-.3,up),NavballMath.color(.6,-.3,mirrored));}

}

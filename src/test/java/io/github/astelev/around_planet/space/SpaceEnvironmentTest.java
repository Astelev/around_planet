package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceEnvironmentTest {
    @Test void fixedSunLightsOneHemisphereAndLeavesOppositeDark(){assertEquals(1,SpaceEnvironment.SUN_DIRECTION.length(),1e-9);assertEquals(1,SpaceEnvironment.illumination(SpaceEnvironment.SUN_DIRECTION),1e-9);assertEquals(.045,SpaceEnvironment.illumination(SpaceEnvironment.SUN_DIRECTION.scale(-1)),1e-12);}
    @Test void sunlightInDifferentLocalCellsStillHasSameCosmicDirection(){var p=new PlanetGeometry(65536,65536);for(int x:new int[]{-60000,0,45000}){var f=p.frame(x,22000);var local=f.toLocal(SpaceEnvironment.SUN_DIRECTION);assertTrue(f.toCosmic(local).subtract(SpaceEnvironment.SUN_DIRECTION).length()<1e-9);}}
    @Test void planetAngularSizeFollowsAltitudeRatherThanPhysicalCellAddress(){double r=20000;assertEquals(Math.PI/6,SpaceEnvironment.angularRadius(r,2*r),1e-12);assertTrue(SpaceEnvironment.angularRadius(r,r+768)>SpaceEnvironment.angularRadius(r,r+10000));}
    @Test void cameraParallaxUsesOnlyOffsetFromItsCellAnchor(){var f=new SpaceFrame(new SpaceVector(0,0,-1),new SpaceVector(0,1,0),new SpaceVector(1,0,0));var s=new SpaceState(new SpaceVector(0,30000,0),SpaceVector.ZERO);var a=new SpaceVector(1024,1536,-1024);assertEquals(new SpaceVector(4,30003,-2),SpaceEnvironment.observer(s,f,a.add(new SpaceVector(2,3,4)),a));}
    @Test void fractionalCoastForRenderingMatchesAnalyticalCircularArc(){var s=new SpaceState(new SpaceVector(100,0,0),new SpaceVector(0,10,0));var t=new SpaceTrajectory("planet",0,s,10000);var p=t.afterSeconds(.025).position();assertEquals(100*Math.cos(.0025),p.x(),1e-9);assertEquals(100*Math.sin(.0025),p.y(),1e-9);assertEquals(t.at(1),t.afterSeconds(.05));}
}

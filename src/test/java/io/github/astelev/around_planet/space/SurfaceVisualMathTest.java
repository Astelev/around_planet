package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SurfaceVisualMathTest {
    @Test void planetBackdropRayHitsActualNearSurfaceAndRejectsSky(){
        double radius=new PlanetScale(131072).radius(),height=2100;
        var down=SurfaceVisualMath.planetNormal(new SpaceVector(0,-1,0),height,radius).orElseThrow();
        assertTrue(down.subtract(new SpaceVector(0,1,0)).length()<1e-10);
        assertTrue(SurfaceVisualMath.planetNormal(new SpaceVector(0,1,0),height,radius).isEmpty());
        assertTrue(SurfaceVisualMath.planetNormal(new SpaceVector(1,0,0),height,radius).isEmpty());
        double edge=-Math.sqrt(1-Math.pow(radius/(radius+height),2));
        assertTrue(SurfaceVisualMath.planetNormal(new SpaceVector(Math.sqrt(1-Math.pow(edge+.01,2)),edge+.01,0),height,radius).isEmpty());
        var normal=SurfaceVisualMath.planetNormal(new SpaceVector(Math.sqrt(1-Math.pow(edge-.01,2)),edge-.01,0),height,radius).orElseThrow();
        assertEquals(1,normal.length(),1e-10);assertTrue(normal.x()>0&&normal.y()>0);
    }
    @Test void nadirBackdropUsesSamePlanetFixedCoordinatesAsOrbitAtlas(){
        var planet=new PlanetScale(131072).geometry();
        for(double x:new double[]{-60000,0,50000})for(double z:new double[]{-30000,0,30000}){
            var frame=planet.frame(x,z);
            var local=SurfaceVisualMath.planetNormal(new SpaceVector(0,-1,0),2200,planet.radius()).orElseThrow();
            assertTrue(frame.toCosmic(local).subtract(frame.up()).length()<1e-10);
        }
    }
    @Test void curvatureDependsOnDistanceNotCompassOrCameraRotation(){double r=new PlanetScale(131072).radius();assertEquals(SurfaceVisualMath.drop(3000,4000,r),SurfaceVisualMath.drop(-4000,3000,r),1e-9);assertEquals(0,SurfaceVisualMath.drop(0,0,r));assertEquals(5000*5000/(2*r),SurfaceVisualMath.drop(3000,4000,r),1e-9);}
    @Test void planetSizeControlsCurvature(){assertEquals(SurfaceVisualMath.drop(500,400,1000)/2,SurfaceVisualMath.drop(500,400,2000),1e-9);assertThrows(IllegalArgumentException.class,()->SurfaceVisualMath.drop(1,1,0));}
    @Test void skyDarkeningStartsAt1600AndReachesSpaceSmoothly(){assertEquals(0,SurfaceVisualMath.darkness(1599,1600,2208));assertEquals(0,SurfaceVisualMath.darkness(1600,1600,2208));assertEquals(.5,SurfaceVisualMath.darkness(1904,1600,2208),1e-9);assertEquals(1,SurfaceVisualMath.darkness(2208,1600,2208));assertEquals(1,SurfaceVisualMath.darkness(3000,1600,2208));}
    @Test void sunAndMoonFollowTheOrdinaryClock(){assertEquals(1,SurfaceVisualMath.sun(0).y(),1e-9);assertEquals(-1,SurfaceVisualMath.sun(Math.PI).y(),1e-9);assertEquals(1,SurfaceVisualMath.sun(1.3).length(),1e-9);assertTrue(SurfaceVisualMath.sun(Math.PI/2).x()<-.99);}
    @Test void higherObserversSeeADepressedHorizon(){assertEquals(0,SurfaceVisualMath.horizon(64,20000),1e-9);assertTrue(SurfaceVisualMath.horizon(1700,20000)<SurfaceVisualMath.horizon(100,20000));}
}

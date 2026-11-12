package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SurfaceVisualMathTest {
    @Test void curvatureDependsOnDistanceNotCompassOrCameraRotation(){double r=new PlanetScale(131072).radius();assertEquals(SurfaceVisualMath.drop(3000,4000,r),SurfaceVisualMath.drop(-4000,3000,r),1e-9);assertEquals(0,SurfaceVisualMath.drop(0,0,r));assertEquals(5000*5000/(2*r),SurfaceVisualMath.drop(3000,4000,r),1e-9);}
    @Test void planetSizeControlsCurvature(){assertEquals(SurfaceVisualMath.drop(500,400,1000)/2,SurfaceVisualMath.drop(500,400,2000),1e-9);assertThrows(IllegalArgumentException.class,()->SurfaceVisualMath.drop(1,1,0));}
    @Test void skyDarkeningStartsAt1600AndReachesSpaceSmoothly(){assertEquals(0,SurfaceVisualMath.darkness(1599,1600,2208));assertEquals(0,SurfaceVisualMath.darkness(1600,1600,2208));assertEquals(.5,SurfaceVisualMath.darkness(1904,1600,2208),1e-9);assertEquals(1,SurfaceVisualMath.darkness(2208,1600,2208));assertEquals(1,SurfaceVisualMath.darkness(3000,1600,2208));}
    @Test void sunAndMoonFollowTheOrdinaryClock(){assertEquals(1,SurfaceVisualMath.sun(0).y(),1e-9);assertEquals(-1,SurfaceVisualMath.sun(Math.PI).y(),1e-9);assertEquals(1,SurfaceVisualMath.sun(1.3).length(),1e-9);assertTrue(SurfaceVisualMath.sun(Math.PI/2).x()<-.99);}
    @Test void higherObserversSeeADepressedHorizon(){assertEquals(0,SurfaceVisualMath.horizon(64,20000),1e-9);assertTrue(SurfaceVisualMath.horizon(1700,20000)<SurfaceVisualMath.horizon(100,20000));}
}

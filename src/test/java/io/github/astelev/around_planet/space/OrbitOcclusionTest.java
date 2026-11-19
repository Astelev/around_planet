package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrbitOcclusionTest {
 @Test void hideInteriorEvenOnNearHemisphere(){assertFalse(OrbitOcclusion.visible(new OrbitCamera.Point(0,0,.5),1));assertFalse(OrbitOcclusion.visible(new OrbitCamera.Point(0,0,-2),1));assertTrue(OrbitOcclusion.visible(new OrbitCamera.Point(0,0,2),1));}
 @Test void clipCrossingAtActualNearSurface(){var a=new OrbitCamera.Point(-2,0,.5);var b=new OrbitCamera.Point(2,0,.5);var visible=OrbitOcclusion.ranges(a,b,1,false);assertEquals(2,visible.size());assertEquals((2-Math.sqrt(.75))/4,visible.getFirst().until(),1e-9);assertEquals((2+Math.sqrt(.75))/4,visible.getLast().from(),1e-9);}
 @Test void retainFarOrbitOutsideSilhouette(){assertEquals(2,OrbitOcclusion.ranges(new OrbitCamera.Point(-2,0,-2),new OrbitCamera.Point(2,0,-2),1,true).size());assertTrue(OrbitOcclusion.ranges(new OrbitCamera.Point(0,0,.1),new OrbitCamera.Point(0,0,.5),1,false).isEmpty());}
}

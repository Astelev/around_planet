package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CurvatureGridTest {
    @Test void outerQuadHasInteriorVerticesAndPreservesSamplesAndWinding(){var points=new ArrayList<float[]>();CurvatureGrid.top(-4096,-2048,4096,0,(x,y,z,u,v)->{assertEquals(x,u);assertEquals(z,v);assertEquals(0,y);points.add(new float[]{x,z});});assertEquals(16*4*4,points.size());assertTrue(points.stream().anyMatch(p->p[0]==0 && p[1]==-1024));double area=0;for(int i=0;i<points.size();i+=4){var a=points.get(i);var b=points.get(i+1);var c=points.get(i+2);assertEquals(a[0],b[0]);assertTrue(b[1]>a[1] && c[0]>b[0]);area+=(b[1]-a[1])*(c[0]-b[0]);}assertEquals(8192d*2048,area);}
    @Test void extremelyFarCoverageHasBoundedVertexCount(){int[] n={0};CurvatureGrid.top(-1000000,-1000000,1000000,1000000,(x,y,z,u,v)->n[0]++);assertEquals(64*64*4,n[0]);}
    @Test void zeroAreaEmitsNoFacesAndInvalidBoundsFail(){CurvatureGrid.top(1,0,1,10,(x,y,z,u,v)->fail("Degenerate face"));assertThrows(IllegalArgumentException.class,()->CurvatureGrid.top(1,0,-1,10,(x,y,z,u,v)->{}));}
}

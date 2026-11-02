package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class FallbackSurfaceMeshTest {
    @Test void raisedCellHasTopAndFourClosedSides() {
        var quads = new ArrayList<float[]>();
        FallbackSurfaceMesh.build(new int[][]{{32}},128,1024,
                (a,b,c,d,e,f,g,h,i,j,k,l) -> quads.add(new float[]{a,b,c,d,e,f,g,h,i,j,k,l}));
        assertEquals(9,quads.size()); // top, four walls, four outer sea strips
        assertEquals(32,quads.getFirst()[1]);
        assertEquals(4,quads.stream().filter(q -> q[1] != q[4]).count());
    }
    @Test void wallsFaceOutwardRatherThanInsideRaisedTile() {
        var walls = new ArrayList<float[]>();
        FallbackSurfaceMesh.build(new int[][]{{32}},64,1024,
                (a,b,c,d,e,f,g,h,i,j,k,l) -> { if (b != e) walls.add(new float[]{a,b,c,d,e,f,g,h,i,j,k,l}); });
        assertEquals(4,walls.size());
        for (var q : walls) {
            float ax=q[3]-q[0], ay=q[4]-q[1], az=q[5]-q[2];
            float bx=q[6]-q[0], by=q[7]-q[1], bz=q[8]-q[2];
            float nx=ay*bz-az*by, nz=ax*by-ay*bx;
            float cx=(q[0]+q[3]+q[6]+q[9])/4-32;
            float cz=(q[2]+q[5]+q[8]+q[11])/4-32;
            assertTrue(nx*cx+nz*cz > 0, "Wall must face away from the tile interior");
        }
    }

    @Test void equalAdjacentTilesHaveNoInternalWall() {
        int[] count={0};
        FallbackSurfaceMesh.build(new int[][]{{0,0},{0,0}},128,1024,
                (a,b,c,d,e,f,g,h,i,j,k,l) -> count[0]++);
        assertEquals(8,count[0]);
    }
}

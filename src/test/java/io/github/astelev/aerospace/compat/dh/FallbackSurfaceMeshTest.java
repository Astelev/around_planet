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
    @Test void equalAdjacentTilesHaveNoInternalWall() {
        int[] count={0};
        FallbackSurfaceMesh.build(new int[][]{{0,0},{0,0}},128,1024,
                (a,b,c,d,e,f,g,h,i,j,k,l) -> count[0]++);
        assertEquals(8,count[0]);
    }
}

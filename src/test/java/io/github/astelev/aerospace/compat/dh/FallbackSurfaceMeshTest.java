package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class FallbackSurfaceMeshTest {
    @Test void raisedCellHasTopAndFourClosedSides() {
        var quads = new ArrayList<float[]>();
        FallbackSurfaceMesh.build(new int[][]{{32}},128,1024,
                (a,b,c,d,e,f,g,h,i,j,k,l) -> quads.add(new float[]{a,b,c,d,e,f,g,h,i,j,k,l}));
        assertEquals(10,quads.size()); // two top quads, four skirts, four outer sea strips
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
        assertEquals(20,count[0]); // eight top quads, eight outer skirt segments, four outer base strips
    }
    @Test void neighboringTilesShareRampCornersInsteadOfVerticalSteps() {
        var nodes=FallbackSurfaceMesh.corners(new int[][]{{0,0},{64,64}},null);
        assertEquals(0,nodes[0][1]); assertEquals(32,nodes[1][1]); assertEquals(64,nodes[2][1]);
        assertEquals(32,FallbackSurfaceMesh.sampleHeight(nodes,new int[][]{{0,0},{64,64}},64,64,32));
        assertEquals(0,FallbackSurfaceMesh.sampleHeight(nodes,new int[][]{{0,0},{64,64}},64,32,32));
        assertEquals(64,FallbackSurfaceMesh.sampleHeight(nodes,new int[][]{{0,0},{64,64}},64,96,32));
    }
    @Test void waterRemainsLevelAndLandRampsMeetTheSameEdge() {
        var nodes=FallbackSurfaceMesh.corners(new int[][]{{3,3},{64,64}},new boolean[][]{{true,true},{false,false}});
        assertEquals(3,nodes[0][1]); assertEquals(3,nodes[1][1]); assertEquals(64,nodes[2][1]);
        assertEquals(3,FallbackSurfaceMesh.sampleHeight(nodes,new int[][]{{3,3},{64,64}},64,32,32));
    }
    @Test void ownershipSkirtsReachBelowTerrainAndSampleTheUnknownSide() {
        var covered=new boolean[64];
        for(int x=4;x<8;x++) for(int z=0;z<8;z++) covered[x*8+z]=true;
        var vertices=new ArrayList<float[]>();
        FallbackSurfaceMesh.build(new int[][]{{32,32},{32,32}},null,64,1024,covered,8,-256,
                (x,y,z,u,v) -> vertices.add(new float[]{x,y,z,u,v}));
        int seams=0;
        for(int i=0;i<vertices.size();i+=4) {
            var a=vertices.get(i); var b=vertices.get(i+1); var c=vertices.get(i+2); var d=vertices.get(i+3);
            if(a[0]==64 && b[0]==64 && c[0]==64 && d[0]==64 && a[1]==-256) {
                seams++;
                assertEquals(32,b[1]);
                assertTrue(a[3]<64 && b[3]<64 && c[3]<64 && d[3]<64);
            }
        }
        assertEquals(8,seams);
    }
}

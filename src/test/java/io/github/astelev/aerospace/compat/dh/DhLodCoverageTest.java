package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import java.util.BitSet;
import static org.junit.jupiter.api.Assertions.*;
class DhLodCoverageTest {
    @Test void readySectionOwnsItsFootprintIndependentOfCameraPixels() {
        var data=new BitSet(4096); data.set(0,4096);
        var source=new DhLodCoverage.Source(-64,-64,1,data);
        var mask=new boolean[9]; source.mark(mask,-2,-2,3);
        assertTrue(mask[4]);
        assertEquals(1,java.util.stream.IntStream.range(0,9).filter(i->mask[i]).count());
    }
    @Test void partialColumnsDoNotHideFallbackInUnfinishedTiles() {
        var data=new BitSet(4096); data.set(0,4096); data.clear(0);
        var mask=new boolean[4];
        new DhLodCoverage.Source(0,0,2,data).mark(mask,0,0,2);
        assertFalse(mask[0]); assertTrue(mask[1]); assertTrue(mask[2]); assertTrue(mask[3]);
    }
    @Test void chunkResolutionPreservesKnownPartsOfAPartialBiomeTile() {
        var data=new BitSet(4096); data.set(0,4096); data.clear(0);
        var mask=new boolean[16];
        new DhLodCoverage.Source(0,0,1,data).mark(mask,0,0,4,16);
        assertFalse(mask[0]); assertTrue(mask[1]); assertTrue(mask[4]); assertTrue(mask[15]);
    }
    @Test void coarseDataCoversOnlyKnownColumnsAndMetadataFollowsBufferIdentity() {
        var data=new BitSet(4096); data.set(0);
        var source=new DhLodCoverage.Source(0,0,128,data);
        var mask=new boolean[16]; source.mark(mask,0,0,4);
        assertTrue(mask[0]); assertTrue(mask[1]); assertTrue(mask[4]); assertTrue(mask[5]); assertFalse(mask[2]);
        var oldBuffer=new Object(); var nextBuffer=new Object();
        DhLodCoverage.attach(oldBuffer,source);
        assertSame(source,DhLodCoverage.get(oldBuffer)); assertNull(DhLodCoverage.get(nextBuffer));
        data.clear();
        var copy=new boolean[1]; source.mark(copy,0,0,1); assertTrue(copy[0]);
    }
}

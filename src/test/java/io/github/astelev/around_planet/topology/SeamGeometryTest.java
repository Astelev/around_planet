package io.github.astelev.around_planet.topology;

import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.compat.DhSeamBounds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeamGeometryTest {
    @Test void imagesUseOppositeCanonicalColumnsWithoutChangingLocalBlockCoordinates() {
        var t=EastWestTopology.atBoundary(65536);
        assertEquals(-4096,SeamGeometry.sourceX(t,4096));
        assertEquals(4095,SeamGeometry.sourceX(t,-4097));
        assertEquals(-4094,SeamGeometry.sourceX(t,4098));
        assertTrue(SeamGeometry.canonical(t,-4096));assertTrue(SeamGeometry.canonical(t,4095));
        assertFalse(SeamGeometry.canonical(t,-4097));assertFalse(SeamGeometry.canonical(t,4096));
    }
    @Test void dhSectionsOutsideAreRejectedWhileCanonicalSectionsRemainInside() {
        var t=EastWestTopology.atBoundary(65536);
        long east=6L | (1024L<<8), west=6L | ((-1025L&0xfffffff)<<8);
        assertTrue(DhSeamBounds.outside(t,east));assertTrue(DhSeamBounds.outside(t,west));
        assertTrue(DhSeamBounds.inside(t,6L | (1023L<<8)));
        assertFalse(DhSeamBounds.inside(t,east));
    }
    @Test void partialSectionsRemainAvailableForFragmentClippingAtAnUnalignedSeam() {
        var t=EastWestTopology.atBoundary(144);
        long east=6L | (2L<<8),west=6L | ((-3L&0xfffffff)<<8);
        assertFalse(DhSeamBounds.inside(t,east));assertFalse(DhSeamBounds.inside(t,west));
        assertFalse(DhSeamBounds.outside(t,east));assertFalse(DhSeamBounds.outside(t,west));
        assertEquals(128,DhSeamBounds.minX(east));assertEquals(192,DhSeamBounds.maxX(east));
        assertEquals(-192,DhSeamBounds.minX(west));assertEquals(-128,DhSeamBounds.maxX(west));
        assertTrue(DhSeamBounds.outside(t,6L | (3L<<8)));
    }
}

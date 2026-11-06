package io.github.astelev.around_planet.generation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeamBlendTest {
    @Test void supportStripUsesTheOppositeChartAndPrivateFarCoordinatesStayUntouched() {
        var b=SeamBlend.of(65536,256);
        assertEquals(-65536,b.canonicalX(65536));assertEquals(65535,b.canonicalX(-65537));
        assertEquals(-65500,b.canonicalX(-65500));assertEquals(20_480_000,b.canonicalX(20_480_000));
        assertEquals(0,b.weight(0));assertEquals(0,b.weight(65536-256));assertEquals(.5,b.weight(-65536));
        assertEquals(64,SeamBlend.of(128,2048).band());
    }
    @Test void blendingHasNoValueOrSlopeJumpAtEitherEnd() {
        var b=SeamBlend.of(65536,256);
        // Sample an intentionally mismatched pair of analytic hills using a continuous extension of the chart.
        double h=.001;
        assertEquals(sample(65536,b),sample(-65536,b),1e-12);
        double before=(sample(65536,b)-sample(65536-h,b))/h;
        double after=(sample(-65536+h,b)-sample(-65536,b))/h;
        assertEquals(before,after,1e-5);
        double inner=65536-256;
        assertEquals(raw(inner),sample(inner,b),0);
        assertEquals((raw(inner)-raw(inner-h))/h,(sample(inner+h,b)-sample(inner,b))/h,1e-5);
    }
    @Test void weightIsMonotonicAndBoundedAndBothShoreBudgetsMatch() {
        var b=SeamBlend.of(65536,256);double previous=0;
        for(int d=256;d>=0;d--) {
            double weight=b.weight(65536-d);assertTrue(weight>=previous && weight<=.5);
            assertEquals(weight,b.weight(-65536+d),1e-15);previous=weight;
        }
        assertEquals(0,b.weight(Integer.MIN_VALUE));assertThrows(IllegalArgumentException.class,()->new SeamBlend(129,32));
    }
    private static double raw(double x){return Math.sin(x/91)*40+Math.cos(x/313)*90;}
    private static double sample(double x,SeamBlend b) {
        double cx=x>=b.boundary()?x-2*b.boundary():x< -b.boundary()?x+2*b.boundary():x;
        double t=Math.max(0,(b.band()-(b.boundary()-Math.abs(cx)))/(2*b.band()));
        double w=t*t*t*(t*(t*6-15)+10),other=cx>=0?cx-2*b.boundary():cx+2*b.boundary();
        return raw(cx)+(raw(other)-raw(cx))*w;
    }
}

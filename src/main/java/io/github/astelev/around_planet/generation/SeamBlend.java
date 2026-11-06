package io.github.astelev.around_planet.generation;

/** Immutable block-space chart for a finite generation strip. Pure math, independent of world/chunk access. */
public record SeamBlend(int boundary,int band) {
    public SeamBlend {
        if(boundary<128 || boundary>29_500_000 || boundary%16!=0 || band<16 || band>boundary/2)throw new IllegalArgumentException("Invalid generation blend strip");
    }
    public static SeamBlend of(int boundary,int requestedBand) {
        return new SeamBlend(boundary,Math.min(requestedBand,boundary/2));
    }
    public int canonicalX(int x) {
        // Only generation support neighbours around the seam are remapped; far outside/private plots stay untouched.
        if((long)x < -(long)boundary-band-16 || (long)x > (long)boundary+band+16)return x;
        return x>=boundary?x-2*boundary:x< -boundary?x+2*boundary:x;
    }
    public double weight(int canonicalX) {
        long distance=(long)boundary-Math.abs((long)canonicalX);
        if(distance<0 || distance>=band)return 0;
        double t=(band-distance)/(2.0*band);
        return t*t*t*(t*(t*6-15)+10); // Quintic: C2 through the seam and at the interior edge.
    }
    public int oppositeX(int canonicalX) {return canonicalX>=0?canonicalX-2*boundary:canonicalX+2*boundary;}
}

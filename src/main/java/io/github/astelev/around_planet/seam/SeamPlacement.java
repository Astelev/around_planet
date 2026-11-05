package io.github.astelev.around_planet.seam;

/** Nearest periodic placement of canonical chunk geometry; never changes its cache key. */
public final class SeamPlacement {
    public static int nearest(int sourceX,int cameraChunkX,int circumferenceChunks) {
        if(circumferenceChunks<=0)throw new IllegalArgumentException("Positive circumference required");
        long periods=Math.floorDiv((long)cameraChunkX-sourceX+circumferenceChunks/2,circumferenceChunks);
        return Math.toIntExact(sourceX+periods*circumferenceChunks);
    }
    private SeamPlacement() {}
}

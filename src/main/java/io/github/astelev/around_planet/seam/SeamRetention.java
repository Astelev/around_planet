package io.github.astelev.around_planet.seam;

/** Retained geometry follows its nearest periodic placement, independently of native chunk ownership. */
public final class SeamRetention {
    public static boolean inView(int canonicalX,int z,int cameraX,int cameraZ,int widthChunks,int radius) {
        int image=SeamPlacement.nearest(canonicalX,cameraX,widthChunks);
        return Math.abs((long)image-cameraX)<=radius+1L && Math.abs((long)z-cameraZ)<=radius+1L;
    }
    private SeamRetention(){}
}

package io.github.astelev.around_planet.progressive;
public interface ProgressiveQueueAccess {
    boolean around_planet$previewSupported();
    boolean around_planet$previewAllowed(long pos);
    boolean around_planet$previewNear(long pos,int radius);
}

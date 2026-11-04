package io.github.astelev.around_planet.portal;

/** Ramp earlier instead of requesting the entire opposite view in one burst. */
public final class PortalLoadBudget {
    public static int radius(int viewDistance,int cap,int approach,double distance) {
        int target=Math.max(2,Math.min(viewDistance,cap));
        double progress=Math.max(0.0,Math.min(1.0,1.0-distance/approach));
        return Math.min(target,Math.max(2,(int)Math.ceil(2+(target-2)*progress)));
    }
    public static int destinationChunkX(double planeX,boolean eastPortal) {
        return (int)Math.floor((planeX+(eastPortal?0.5:-0.5))/16.0);
    }
    private PortalLoadBudget() {}
}

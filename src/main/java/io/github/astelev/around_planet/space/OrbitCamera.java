package io.github.astelev.around_planet.space;
/** Orthographic orbit-map camera, independent of Minecraft render state. */
public final class OrbitCamera {
    public record Point(double x,double y,double depth){}
    public double yaw,pitch,zoom=1,panX,panY;public long basisVersion;private double extent=1;
    private SpaceVector right=new SpaceVector(1,0,0),up=new SpaceVector(0,1,0),forward=new SpaceVector(0,0,1);
    public void fit(SpaceState state,double extent){basisVersion++;this.extent=Math.max(1,extent);yaw=pitch=panX=panY=0;zoom=1;
        var r=state.position();if(r.length()<1e-9)return;right=r.scale(1/r.length());var h=r.cross(state.velocity());
        if(h.length()<1e-9)h=right.cross(Math.abs(right.y())<.9?new SpaceVector(0,1,0):new SpaceVector(0,0,1));
        forward=h.scale(1/h.length());up=forward.cross(right);
    }
    public double scale(double viewportSize){return viewportSize*.42*zoom/extent;}
    public Point project(SpaceVector p,double cx,double cy,double viewportSize){
        double x=p.dot(right),y=p.dot(up),z=p.dot(forward),a=x*Math.cos(yaw)+z*Math.sin(yaw),b=-x*Math.sin(yaw)+z*Math.cos(yaw);
        double c=y*Math.cos(pitch)-b*Math.sin(pitch),depth=y*Math.sin(pitch)+b*Math.cos(pitch);double scale=scale(viewportSize);
        return new Point(cx+panX+a*scale,cy+panY-c*scale,depth);
    }
    public SpaceVector fromView(double x,double y,double z){
        double yy=y*Math.cos(pitch)+z*Math.sin(pitch),zz=-y*Math.sin(pitch)+z*Math.cos(pitch);
        double xx=x*Math.cos(yaw)-zz*Math.sin(yaw),z0=x*Math.sin(yaw)+zz*Math.cos(yaw);
        return right.scale(xx).add(up.scale(yy)).add(forward.scale(z0));
    }
    public void rotate(double dx,double dy){yaw=Math.IEEEremainder(yaw+dx*.008,2*Math.PI);pitch=Math.max(-Math.PI/2,Math.min(Math.PI/2,pitch+dy*.008));}
    public void zoom(double wheel){zoom=Math.max(.025,Math.min(200,zoom*Math.pow(1.15,Math.max(-20,Math.min(20,wheel)))));}
    /** Clip huge open trajectories before submitting float GUI vertices. */
    public static double[] clip(double ax,double ay,double bx,double by,double left,double top,double right,double bottom){
        double dx=bx-ax,dy=by-ay,lo=0,hi=1;double[] p={-dx,dx,-dy,dy},q={ax-left,right-ax,ay-top,bottom-ay};
        for(int i=0;i<4;i++){if(Math.abs(p[i])<1e-12){if(q[i]<0)return null;}else{double t=q[i]/p[i];if(p[i]<0)lo=Math.max(lo,t);else hi=Math.min(hi,t);if(lo>hi)return null;}}
        return new double[]{ax+lo*dx,ay+lo*dy,ax+hi*dx,ay+hi*dy};
    }
}

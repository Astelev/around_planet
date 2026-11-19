package io.github.astelev.around_planet.space;
/** Orthographic orbit-map camera, independent of Minecraft render state. */
public final class OrbitCamera {
    public record Point(double x,double y,double depth){}
    public double yaw,pitch,zoom=1;public long basisVersion;private double extent=1;
    public SpaceVector focus=SpaceVector.ZERO;
    private SpaceVector right=new SpaceVector(1,0,0),up=new SpaceVector(0,0,1),forward=new SpaceVector(0,-1,0);
    /** Keep geographic +Z upright: horizontal drag orbits the planet spin axis, even for inclined vessels. */
    public void fit(SpaceState state,double extent){basisVersion++;this.extent=Math.max(1,extent);yaw=0;pitch=.45;focus=SpaceVector.ZERO;zoom=1;
        var equatorial=new SpaceVector(state.position().x(),state.position().y(),0);
        right=equatorial.length()>1e-9?equatorial.scale(1/equatorial.length()):new SpaceVector(1,0,0);
        up=new SpaceVector(0,0,1);forward=right.cross(up);
    }
    public double scale(double viewportSize){return viewportSize*.42*zoom/extent;}
    public Point project(SpaceVector p,double cx,double cy,double viewportSize){
        var v=direction(p.subtract(focus));double scale=scale(viewportSize);
        return new Point(cx+v.x()*scale,cy+v.y()*scale,direction(p).depth());
    }
    /** Unit direction in screen axes (+X right, +Y down, positive depth toward viewer), without pan/zoom. */
    public Point direction(SpaceVector p){
        double x=p.dot(right),y=p.dot(up),z=p.dot(forward),a=x*Math.cos(yaw)+z*Math.sin(yaw),b=-x*Math.sin(yaw)+z*Math.cos(yaw);
        return new Point(a,-(y*Math.cos(pitch)-b*Math.sin(pitch)),y*Math.sin(pitch)+b*Math.cos(pitch));
    }
    public SpaceVector fromView(double x,double y,double z){
        double yy=y*Math.cos(pitch)+z*Math.sin(pitch),zz=-y*Math.sin(pitch)+z*Math.cos(pitch);
        double xx=x*Math.cos(yaw)-zz*Math.sin(yaw),z0=x*Math.sin(yaw)+zz*Math.cos(yaw);
        return right.scale(xx).add(up.scale(yy)).add(forward.scale(z0));
    }
    /** Drag translates the world-space pivot, so the next rotation and zoom use the dragged view center. */
    public void pan(double dx,double dy,double viewportSize){double s=scale(viewportSize);focus=focus.add(fromView(-dx/s,dy/s,0));}
    public OrbitCamera copy(){var c=new OrbitCamera();c.set(this);return c;}
    public void set(OrbitCamera c){yaw=c.yaw;pitch=c.pitch;zoom=c.zoom;focus=c.focus;extent=c.extent;right=c.right;up=c.up;forward=c.forward;basisVersion=c.basisVersion+1;}
    public void rotate(double dx,double dy){yaw=Math.IEEEremainder(yaw+dx*.008,2*Math.PI);pitch=Math.max(-Math.PI/2,Math.min(Math.PI/2,pitch+dy*.008));}
    public void zoom(double wheel){zoom=Math.max(.000001,Math.min(100000000,zoom*Math.pow(1.15,Math.max(-20,Math.min(20,wheel)))));}
    /** Clip huge open trajectories before submitting float GUI vertices. */
    public static double[] clip(double ax,double ay,double bx,double by,double left,double top,double right,double bottom){
        double dx=bx-ax,dy=by-ay,lo=0,hi=1;double[] p={-dx,dx,-dy,dy},q={ax-left,right-ax,ay-top,bottom-ay};
        for(int i=0;i<4;i++){if(Math.abs(p[i])<1e-12){if(q[i]<0)return null;}else{double t=q[i]/p[i];if(p[i]<0)lo=Math.max(lo,t);else hi=Math.min(hi,t);if(lo>hi)return null;}}
        return new double[]{ax+lo*dx,ay+lo*dy,ax+hi*dx,ay+hi*dy};
    }
}

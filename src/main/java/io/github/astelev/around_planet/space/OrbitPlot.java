package io.github.astelev.around_planet.space;
import java.util.*;

/** Spatial conic from the same instantaneous r/v/mu as the server. No time integration or chunk access. */
public record OrbitPlot(List<SpaceVector> points,SpaceVector periapsis,SpaceVector apoapsis,double period,boolean reentry,boolean open) {
    public OrbitPlot {points=List.copyOf(points);}
    public static OrbitPlot create(SpaceState state,double mu,double radius,double returnAltitude,boolean waiting){
        var r=state.position();var v=state.velocity();double distance=r.length(),shell=radius+returnAltitude;
        if(waiting || distance<1e-9)return new OrbitPlot(List.of(r),null,null,0,waiting,false);
        var h=r.cross(v);double h2=h.dot(h);double energy=v.dot(v)/2-(mu>0?mu/distance:0);
        if(mu<=0 || h2<mu*distance*1e-12)return radial(state,mu,radius,shell,energy);
        var eccentric=v.cross(h).scale(1/mu).subtract(r.scale(1/distance));double e=eccentric.length(),p=h2/mu;
        var axis=e>1e-8?eccentric.scale(1/e):r.scale(1/distance);var tangent=h.scale(1/Math.sqrt(h2)).cross(axis);
        double start=Math.atan2(r.dot(tangent),r.dot(axis));boolean open=energy>=0;double end=open?Math.acos(Math.max(-1,Math.min(1,-1/e)))-1e-7:start+2*Math.PI;
        double peri=p/(1+e),a=open?Double.NaN:-mu/(2*energy),apo=open?Double.NaN:2*a-peri;
        double period=open?0:2*Math.PI*Math.sqrt(a*a*a/mu);boolean entry=false;
        if(e>1e-8 && peri<shell){double cosine=(p/shell-1)/e;if(cosine>=-1 && cosine<=1){double hit=-Math.acos(cosine);while(hit<start+1e-9)hit+=2*Math.PI;if(hit<=end){end=hit;entry=true;}}}
        var points=new ArrayList<SpaceVector>();points.add(r);double limit=Math.max(radius*50,distance*10);
        for(int i=1;i<=384;i++){double angle=start+(end-start)*i/384;double denominator=1+e*Math.cos(angle);if(denominator<=0)break;double length=p/denominator;if(length>limit && open)break;
            points.add(axis.scale(Math.cos(angle)*length).add(tangent.scale(Math.sin(angle)*length)));
        }
        // Very narrow hyperbolic arcs must still show a direction even when the first fixed angular step exceeds the display horizon.
        if(points.size()==1 && v.length()>0)points.add(r.add(v.scale(Math.max(radius,distance)/v.length())));
        return new OrbitPlot(points,axis.scale(peri),open?null:axis.scale(-apo),period,entry && points.size()==385,open);
    }
    private static OrbitPlot radial(SpaceState state,double mu,double radius,double shell,double energy){
        var r=state.position();var v=state.velocity();double d=r.length();var unit=r.scale(1/d);var points=new ArrayList<SpaceVector>();points.add(r);
        if(mu==0){
            if(v.length()<1e-10)return new OrbitPlot(points,null,null,0,false,true);
            var end=r.add(v.scale(Math.max(radius*20,d*5)/v.length()));
            // The line can intersect the shell without crossing the planet center.
            var delta=end.subtract(r);double b=2*r.dot(delta),c=r.dot(r)-shell*shell,disc=b*b-4*delta.dot(delta)*c;
            if(disc>=0){double t=(-b-Math.sqrt(disc))/(2*delta.dot(delta));if(t>=0 && t<=1){points.add(r.add(delta.scale(t)));return new OrbitPlot(points,null,null,0,true,true);}}
            points.add(end);return new OrbitPlot(points,null,null,0,false,true);
        }
        if(r.dot(v)<=0){points.add(unit.scale(shell));return new OrbitPlot(points,null,null,0,true,false);}
        if(energy<0){var apo=unit.scale(-mu/energy);points.add(apo);points.add(unit.scale(shell));return new OrbitPlot(points,null,apo,0,true,false);}
        points.add(unit.scale(Math.max(radius*20,d*5)));return new OrbitPlot(points,null,null,0,false,true);
    }
    public double extent(double planetRadius){double size=planetRadius;for(var p:points)size=Math.max(size,p.length());return size;}
}

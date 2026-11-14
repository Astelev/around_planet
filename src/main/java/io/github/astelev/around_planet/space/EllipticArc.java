package io.github.astelev.around_planet.space;
import java.util.*;
/** Geometry and exact time-of-flight share eccentric anomaly; no elapsed-time integration. */
public final class EllipticArc {
    public record Vertex(double seconds,SpaceVector position){}
    public record Arc(List<Vertex> vertices,boolean reentry){public Arc{vertices=List.copyOf(vertices);}}
    private final SpaceState state;private final SpaceVector axis,tangent;private final double a,e,peri,b,start,motion;
    private EllipticArc(SpaceState state,SpaceVector axis,SpaceVector tangent,double a,double e,double peri,double b,double start,double motion){this.state=state;this.axis=axis;this.tangent=tangent;this.a=a;this.e=e;this.peri=peri;this.b=b;this.start=start;this.motion=motion;}
    public static EllipticArc of(SpaceState state,double mu){
        var r=state.position();var v=state.velocity();double distance=r.length();if(mu<=0||distance<=0)return null;
        double alpha=2/distance-v.dot(v)/mu;var h=r.cross(v);double h2=h.dot(h);if(alpha<=0||h2<mu*distance*1e-12)return null;
        double a=1/alpha,p=h2/mu;var eccentric=v.cross(h).scale(1/mu).subtract(r.scale(1/distance));double magnitude=eccentric.length(),e=Math.min(Math.nextDown(1.0),magnitude);
        var axis=magnitude>1e-8?eccentric.scale(1/magnitude):r.scale(1/distance);var tangent=h.scale(1/Math.sqrt(h2)).cross(axis);
        if(magnitude<=1e-8){a=distance;p=distance;e=0;}
        double peri=p/(1+e),b=Math.sqrt(a)*Math.sqrt(p),start=Math.atan2(r.dot(tangent)/b,1+(r.dot(axis)-peri)/a),motion=Math.sqrt(mu/a)/a;
        if(!(motion>0)||!Double.isFinite(2*Math.PI/motion))return null;return new EllipticArc(state,axis,tangent,a,e,peri,b,start,motion);
    }
    public double secondsTo(SpaceVector point){double angle=Math.atan2(point.dot(tangent)/b,1+(point.dot(axis)-peri)/a);while(angle<start-1e-12)angle+=2*Math.PI;return Math.max(0,meanDelta(angle)/motion);}
    public double period(){return 2*Math.PI/motion;}
    public SpaceVector periapsis(){return axis.scale(peri);}
    public SpaceVector apoapsis(){return axis.scale(-(2*a-peri));}
    public Arc full(double shell,int count){return build(start+2*Math.PI,shell,count);}
    public Arc until(double seconds,double shell,int count){if(seconds>=period())return full(shell,count);double lo=start,hi=start+2*Math.PI,target=Math.max(0,seconds)*motion;for(int i=0;i<64;i++){double mid=(lo+hi)/2;if(meanDelta(mid)<target)lo=mid;else hi=mid;}return build((lo+hi)/2,shell,count);}
    private double meanDelta(double end){double d=end-start,mid=(end+start)/2;double deficit=Math.abs(d)<.01?d*d*d*(1.0/24-d*d/1920+d*d*d*d/322560):d-2*Math.sin(d/2);double sin=Math.sin(mid/2);return (1-e)*d+e*(2*d*sin*sin+Math.cos(mid)*deficit);}
    private Arc build(double end,double shell,int count){
        boolean entry=false;if(state.position().length()<=shell+1e-6&&state.position().dot(state.velocity())<0){end=start;entry=true;}
        else if(e>1e-12&&peri<shell){double cosine=(1-shell/a)/e;if(cosine>=-1&&cosine<=1){double hit=-Math.acos(cosine);while(hit<start+1e-10)hit+=2*Math.PI;if(hit<=end){end=hit;entry=true;}}}
        var points=new ArrayList<Vertex>();points.add(new Vertex(0,state.position()));if(end<=start+1e-14)return new Arc(points,entry);
        for(int i=1;i<=count;i++){double angle=start+(end-start)*i/count,s=Math.sin(angle/2);var position=axis.scale(peri-2*a*s*s).add(tangent.scale(b*Math.sin(angle)));double seconds=meanDelta(angle)/motion;if(i==count&&!entry&&Math.abs(end-start-2*Math.PI)<1e-10){position=state.position();seconds=period();}points.add(new Vertex(seconds,position));}
        return new Arc(points,entry);
    }
}

package io.github.astelev.around_planet.space;

/** Equirectangular world chart on a sphere. X is periodic; solid Z borders stop just short of poles. */
public record PlanetGeometry(double halfX,double halfZ) {
    public static final double MAX_LATITUDE=Math.PI/2-1e-6;
    public PlanetGeometry {if(!Double.isFinite(halfX) || !Double.isFinite(halfZ) || halfX<128 || halfZ<128 || halfX>29_500_000 || halfZ>29_500_000)throw new IllegalArgumentException("Planet bounds");}
    public double radius(){return halfX/Math.PI;}
    public double wrapX(double x){return x-Math.floor((x+halfX)/(2*halfX))*(2*halfX);}
    public SpaceFrame frame(double x,double z){
        double lon=wrapX(x)/radius(),lat=Math.max(-1,Math.min(1,z/halfZ))*MAX_LATITUDE;
        double a=Math.sin(lon),b=Math.cos(lon),c=Math.sin(lat),d=Math.cos(lat);
        return new SpaceFrame(new SpaceVector(b,-a,0),new SpaceVector(a*d,b*d,c),new SpaceVector(-a*c,-b*c,d));
    }
    public SpaceVector position(double x,double y,double z){return frame(x,z).up().scale(radius()+y);}
    public SpaceVector surface(SpaceVector cosmic){
        double r=cosmic.length();if(r<1e-9)throw new IllegalArgumentException("Planet center has no surface chart");
        return new SpaceVector(wrapX(Math.atan2(cosmic.x(),cosmic.y())*radius()),r-radius(),
                Math.max(-halfZ,Math.min(halfZ,Math.asin(Math.max(-1,Math.min(1,cosmic.z()/r)))*halfZ/MAX_LATITUDE)));
    }
    /** First inward intersection during a linear step, including a step crossing the entire sphere. */
    public java.util.Optional<SpaceVector> inwardCrossing(SpaceVector a,SpaceVector b,double altitude){
        double r=radius()+altitude;var d=b.subtract(a);double aa=d.dot(d),bb=2*a.dot(d),cc=a.dot(a)-r*r;
        if(aa<1e-20)return java.util.Optional.empty();
        double disc=bb*bb-4*aa*cc;if(disc<=0)return java.util.Optional.empty();
        double t=(-bb-Math.sqrt(disc))/(2*aa);
        if(t<0 || t>1)return java.util.Optional.empty();return java.util.Optional.of(a.add(d.scale(t)));
    }
}

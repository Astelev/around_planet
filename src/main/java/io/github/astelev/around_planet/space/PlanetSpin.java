package io.github.astelev.around_planet.space;
/** Positive longitude is east; one uniform turn per vanilla 24000-tick solar day. */
public record PlanetSpin(double angle,double omega) {
    public static final double PERIOD_SECONDS=1200,OMEGA=2*Math.PI/PERIOD_SECONDS;
    public static PlanetSpin at(double dayTicks,boolean advancing){return new PlanetSpin((dayTicks%24000-6000)*2*Math.PI/24000,advancing?OMEGA:0);}
    public SpaceVector rotate(SpaceVector p){double c=Math.cos(angle),s=Math.sin(angle);return new SpaceVector(c*p.x()+s*p.y(),-s*p.x()+c*p.y(),p.z());}
    public SpaceVector unrotate(SpaceVector p){return new PlanetSpin(-angle,omega).rotate(p);}
    public SpaceFrame frame(PlanetGeometry p,double x,double z){var f=p.frame(x,z);return new SpaceFrame(rotate(f.east()),rotate(f.up()),rotate(f.south()));}
    public SpaceVector position(PlanetGeometry p,double x,double y,double z){return rotate(p.position(x,y,z));}
    public SpaceVector surface(PlanetGeometry p,SpaceVector cosmic){return p.surface(unrotate(cosmic));}
    public SpaceVector velocity(SpaceVector cosmic){return new SpaceVector(omega*cosmic.y(),-omega*cosmic.x(),0);}
    public SpaceState launch(PlanetGeometry p,SpaceVector world,SpaceVector localVelocity){var r=position(p,world.x(),world.y(),world.z());return new SpaceState(r,frame(p,world.x(),world.z()).toCosmic(localVelocity).add(velocity(r)));}
    public SpaceVector landingVelocity(PlanetGeometry p,SpaceVector position,SpaceVector velocity){var w=surface(p,position);return frame(p,w.x(),w.z()).toLocal(velocity.subtract(velocity(position)));}
    public SpaceVector damp(Atmosphere atmosphere,SpaceState state,double radius,double seconds){var wind=velocity(state.position());return atmosphere.damp(new SpaceState(state.position(),state.velocity().subtract(wind)),radius,seconds).add(wind);}
    public SpaceVector handoff(Atmosphere atmosphere,SpaceState state){var wind=velocity(state.position());return atmosphere.handoff(state.velocity().subtract(wind)).add(wind);}
    public static double localTime(double dayTicks,double x,double circumference){double f=dayTicks/24000-.25+x/circumference;return f-Math.floor(f);}
    public static double ticksUntilDawn(long dayTicks,double x,double circumference){double local=Math.floorMod(dayTicks,24000)+x/circumference*24000;double phase=local-Math.floor(local/24000)*24000;return phase==0?0:24000-phase;}
    public static int skyDarken(double time,double rain,double thunder){double light=.5+2*Math.clamp(Math.cos(time*2*Math.PI),-.25,.25);return (int)((1-light*(1-rain*5/16)*(1-thunder*5/16))*11);}
    public static double circularToSpinRatio(double radius,double altitude,double gravity){double distance=radius+altitude;return Math.sqrt(gravity*radius*radius/distance)/(OMEGA*distance);}
}

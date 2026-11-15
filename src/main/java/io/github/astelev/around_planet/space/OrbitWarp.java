package io.github.astelev.around_planet.space;
/** Bounds an instantaneous Kepler coast before interactions requiring real simulation ticks. */
public final class OrbitWarp {
    public static long atmosphere(SpaceTrajectory path,long now,long ticks,double shell){
        if(ticks<=0)return 0;var state=path.at(now);double radius=state.position().length();if(radius<=shell+1e-5&&state.position().dot(state.velocity())<0)return 0;
        var ellipse=EllipticArc.of(state,path.mu());if(ellipse!=null){var arc=ellipse.until(ticks/20.0,shell,1);return arc.reentry()?Math.min(ticks,Math.max(0,(long)Math.floor(arc.vertices().getLast().seconds()*20))):ticks;}
        if(path.mu()==0){var r=state.position();var v=state.velocity();double aa=v.dot(v),bb=2*r.dot(v),cc=r.dot(r)-shell*shell,disc=bb*bb-4*aa*cc;if(aa<1e-20||disc<=0)return ticks;double seconds=(-bb-Math.sqrt(disc))/(2*aa);return seconds<0?ticks:Math.min(ticks,Math.max(0,(long)Math.floor(seconds*20)));}
        // Radial/parabolic/escape cases: each step is bounded by the maximum acceleration
        // outside the shell, so it cannot jump through the whole planet and emerge beyond it.
        double elapsed=0,end=ticks/20.0,g=path.mu()/(shell*shell);for(int i=0;i<512;i++){var s=path.afterSeconds((now-path.epochTick())/20.0+elapsed);double distance=s.position().length()-shell;if(distance<=.001)return Math.min(ticks,Math.max(0,(long)Math.floor(elapsed*20)));double speed=s.velocity().length();double step=.8*2*distance/(speed+Math.sqrt(speed*speed+2*g*distance));if(elapsed+step>=end)return ticks;elapsed+=step;}return Math.min(ticks,Math.max(0,(long)Math.floor(elapsed*20)));
    }
    public static long proximity(SpaceTrajectory a,SpaceTrajectory b,long now,long ticks,double distance,double minimumRadius){
        double elapsed=0,end=ticks/20.0,acceleration=(a.mu()+b.mu())/(minimumRadius*minimumRadius);for(int i=0;i<512;i++){double dt=(now-a.epochTick())/20.0+elapsed;var x=a.afterSeconds(dt);var y=b.afterSeconds((now-b.epochTick())/20.0+elapsed);double gap=x.position().subtract(y.position()).length()-distance;if(gap<=.001)return Math.min(ticks,Math.max(0,(long)Math.floor(elapsed*20)));double speed=x.velocity().subtract(y.velocity()).length();if(speed<1e-12&&acceleration==0)return ticks;double step=.8*2*gap/(speed+Math.sqrt(speed*speed+2*acceleration*gap));if(elapsed+step>=end)return ticks;elapsed+=step;}return Math.min(ticks,Math.max(0,(long)Math.floor(elapsed*20)));
    }
    private OrbitWarp(){}
}

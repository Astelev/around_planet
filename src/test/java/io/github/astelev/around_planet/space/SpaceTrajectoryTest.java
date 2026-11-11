package io.github.astelev.around_planet.space;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SpaceTrajectoryTest {
    private static SpaceVector v(double x,double y,double z){return new SpaceVector(x,y,z);}
    private static void near(SpaceVector expected,SpaceVector actual,double epsilon){assertTrue(expected.subtract(actual).length()<epsilon,()->expected+" != "+actual);}
    @Test void unloadedFreeFlightIsDirectAndDoesNotChangeEpoch() {
        var t=new SpaceTrajectory("sun",200,new SpaceState(v(1,2,3),v(10,-20,30)),0);
        near(v(1+10*86400,2-20*86400,3+30*86400),t.at(200+86400L*20).position(),1e-8);
        assertEquals(200,t.epochTick());assertEquals(new SpaceState(v(1,2,3),v(10,-20,30)),t.at(200));
        near(v(-9,22,-27),t.at(180).position(),1e-12);
    }
    @Test void circularQuarterOrbitAndManyPeriods() {
        double r=100,mu=10000,speed=Math.sqrt(mu/r),period=2*Math.PI*Math.sqrt(r*r*r/mu);
        var initial=new SpaceState(v(r,0,0),v(0,speed,0));
        var quarter=KeplerMotion.at(initial,mu,period/4);
        near(v(0,r,0),quarter.position(),1e-9);near(v(-speed,0,0),quarter.velocity(),1e-10);
        near(v(0,r,0),KeplerMotion.at(initial,mu,period*10000+period/4).position(),1e-7);
        near(v(0,-r,0),KeplerMotion.at(initial,mu,-period/4).position(),1e-9);
    }
    @Test void eccentricApocenterUsesKnownRadiusAndAngularMomentum() {
        double mu=10000,peri=100,apo=300,a=(peri+apo)/2,speed=Math.sqrt(mu*(2/peri-1/a));
        var initial=new SpaceState(v(peri,0,0),v(0,speed,0));
        var after=KeplerMotion.at(initial,mu,Math.PI*Math.sqrt(a*a*a/mu));
        near(v(-apo,0,0),after.position(),1e-8);near(v(0,-speed*peri/apo,0),after.velocity(),1e-9);
    }
    @Test void hyperbolicParabolicAndNearParabolicArcsConserveEnergyAndReverse() {
        double mu=3.986004418e14,r=7e6;
        for(double factor:new double[]{0.8,0.999999999,1,1.000000001,1.5}) {
            var initial=new SpaceState(v(r,0,0),v(0,Math.sqrt(2*mu/r)*factor,0));
            for(double dt:new double[]{-100000,-60,60,100000}) {
                var after=KeplerMotion.at(initial,mu,dt);
                double beforeEnergy=initial.velocity().dot(initial.velocity())/2-mu/r;
                double energy=after.velocity().dot(after.velocity())/2-mu/after.position().length();
                assertEquals(beforeEnergy,energy,1e-5);
                near(initial.position().cross(initial.velocity()),after.position().cross(after.velocity()),20);
                var back=KeplerMotion.at(after,mu,-dt);near(initial.position(),back.position(),0.02);near(initial.velocity(),back.velocity(),1e-5);
            }
        }
    }
    @Test void radialCoastWorksBeforeSurfaceContact() {
        var initial=new SpaceState(v(100,0,0),SpaceVector.ZERO);
        var after=KeplerMotion.at(initial,10000,0.1);
        assertTrue(after.position().x()<100 && after.position().x()>99.9);assertTrue(after.velocity().x()<0);
        near(initial.position(),KeplerMotion.at(after,10000,-0.1).position(),1e-8);
    }
    @Test void directUnloadedQueryAgreesWithManyIntermediateQueries() {
        var initial=new SpaceState(v(7000,100,300),v(-.1,5,2));double mu=398600;
        var direct=KeplerMotion.at(initial,mu,600);
        var stepped=initial;for(int i=0;i<120;i++)stepped=KeplerMotion.at(stepped,mu,5);
        near(direct.position(),stepped.position(),1e-5);near(direct.velocity(),stepped.velocity(),1e-8);
        var random=new Random(5);
        for(int i=0;i<100;i++) {
            var s=new SpaceState(v(1000+random.nextDouble()*1000,300,200),v(random.nextDouble(),random.nextDouble()*10,1));
            var after=KeplerMotion.at(s,100000,120);near(s.position(),KeplerMotion.at(after,100000,-120).position(),1e-6);
        }
    }
    @Test void maneuverRebasesAtItsEventAndPreservesPosition() {
        var initial=new SpaceTrajectory("planet",0,new SpaceState(v(100,0,0),v(0,10,0)),10000);
        var before=initial.at(100);var next=initial.impulse(100,v(1,2,3));
        assertEquals(before.position(),next.initial().position());assertEquals(before.velocity().add(v(1,2,3)),next.initial().velocity());
        assertEquals(100,next.epochTick());assertEquals(0,initial.epochTick());
    }
    @Test void invalidStatesDoNotEnterPersistence() {
        assertThrows(IllegalArgumentException.class,()->v(Double.NaN,0,0));
        assertThrows(IllegalArgumentException.class,()->new SpaceTrajectory("planet",0,new SpaceState(SpaceVector.ZERO,SpaceVector.ZERO),1));
        assertThrows(IllegalArgumentException.class,()->new SpaceTrajectory("planet",0,new SpaceState(v(1,0,0),SpaceVector.ZERO),-1));
    }
}

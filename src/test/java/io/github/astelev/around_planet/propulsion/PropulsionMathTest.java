package io.github.astelev.around_planet.propulsion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PropulsionMathTest {
    @Test void thrustUsesSecondsAndProportionalSignal(){assertEquals(500,PropulsionMath.impulse(10000,15,.05));assertEquals(100,PropulsionMath.impulse(10000,3,.05));assertEquals(0,PropulsionMath.impulse(10000,0,.05));assertEquals(0,PropulsionMath.impulse(Double.NaN,15,.05));assertEquals(0,PropulsionMath.impulse(10000,15,Double.POSITIVE_INFINITY));}
    @Test void substepsPreserveTotalImpulse(){assertEquals(PropulsionMath.impulse(3000,9,.05),5*PropulsionMath.impulse(3000,9,.01),1e-12);}
    @Test void dampingHasNoFreeEnergyAndNeverReversesRotation(){for(int fe:new int[]{0,1,10,100000}){var b=PropulsionMath.brake(100000,2,15,.05,fe);assertTrue(b.factor()>=0&&b.factor()<=1);assertTrue(b.cost()<=fe);assertTrue(100000*(1-b.factor()*b.factor())*PropulsionMath.FE_PER_JOULE<=b.cost()+1e-8);}assertEquals(new PropulsionMath.Brake(1,0),PropulsionMath.brake(100,2,15,.05,0));}
    @Test void dampingIsExponentialAndIndependentOfSubstepCount(){var a=PropulsionMath.brake(1000,2,15,.05,10000);var b=PropulsionMath.brake(1000,2,15,.01,10000);assertEquals(a.factor(),Math.pow(b.factor(),5),1e-12);}
    @Test void stoppedUnpoweredAndInvalidBodiesDoNotConsumeFE(){assertEquals(new PropulsionMath.Brake(1,0),PropulsionMath.brake(0,2,15,.05,100));assertEquals(new PropulsionMath.Brake(1,0),PropulsionMath.brake(100,2,0,.05,100));assertEquals(new PropulsionMath.Brake(1,0),PropulsionMath.brake(Double.NaN,2,15,.05,100));}
}

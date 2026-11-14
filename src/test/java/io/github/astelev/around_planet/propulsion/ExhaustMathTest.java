package io.github.astelev.around_planet.propulsion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExhaustMathTest {
    @Test void unpoweredZeroThrustAndInvalidValuesDoNotRender(){assertFalse(ExhaustMath.jet(10000,0,false,0).visible());assertFalse(ExhaustMath.jet(0,15,false,0).visible());assertFalse(ExhaustMath.jet(Double.NaN,15,false,0).visible());assertFalse(ExhaustMath.jet(10000,15,false,Double.NaN).visible());}
    @Test void flameRespondsToBothSignalAndConfiguredThrust(){var low=ExhaustMath.jet(10000,1,false,0);var full=ExhaustMath.jet(10000,15,false,0);assertTrue(full.length()>low.length());assertTrue(full.alpha()>low.alpha());assertTrue(ExhaustMath.jet(1000,15,false,0).length()<full.length());}
    @Test void maximumEffectsRemainInsideTheirRenderBounds(){for(boolean rsu:new boolean[]{false,true})for(double phase:new double[]{0,Math.PI/2,Math.PI}){var j=ExhaustMath.jet(1e9,100,rsu,phase);assertTrue(j.length()<=(rsu?1.1:3.81));assertTrue(j.alpha()>0&&j.alpha()<=255);assertTrue(j.halfWidth()<(rsu?.1:.25));}}
    @Test void animationIsBoundedAndRsuIsSmallerThanTheMainEngine(){var a=ExhaustMath.jet(1000,15,true,0);var b=ExhaustMath.jet(1000,15,true,Math.PI/2);assertNotEquals(a.length(),b.length());assertTrue(a.length()<ExhaustMath.jet(10000,15,false,0).length());}
}

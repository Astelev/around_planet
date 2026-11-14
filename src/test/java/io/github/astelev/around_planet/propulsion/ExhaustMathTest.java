package io.github.astelev.around_planet.propulsion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExhaustMathTest {
    @Test void unpoweredZeroThrustAndInvalidValuesDoNotRender(){assertFalse(ExhaustMath.jet(10000,0,false,0).visible());assertFalse(ExhaustMath.jet(0,15,false,0).visible());assertFalse(ExhaustMath.jet(Double.NaN,15,false,0).visible());assertFalse(ExhaustMath.jet(10000,15,false,Double.NaN).visible());}
    @Test void activeSectionShrinksWithThrustAndSignal(){for(boolean rsu:new boolean[]{false,true}){var full=ExhaustMath.jet(10000,15,rsu,0);var low=ExhaustMath.jet(10000,1,rsu,0);assertTrue(full.activeLength()>low.activeLength());assertTrue(full.alpha()>low.alpha());assertTrue(ExhaustMath.jet(100,15,rsu,0).activeLength()<full.activeLength());}}
    @Test void expandingGasKeepsItsMinimumLengthEvenAtTinyPositiveThrust(){for(boolean rsu:new boolean[]{false,true})for(double phase:new double[]{0,Math.PI/2,Math.PI}){var j=ExhaustMath.jet(.0001,1,rsu,phase);assertEquals(ExhaustMath.minimumCone(rsu),j.coneLength());assertTrue(j.activeLength()<.001);assertTrue(j.visible());assertTrue(j.coneAlpha()>0);}}
    @Test void maximumEffectsRemainInsideTheirRenderBounds(){for(boolean rsu:new boolean[]{false,true})for(double phase:new double[]{0,Math.PI/2,Math.PI}){var j=ExhaustMath.jet(1e9,100,rsu,phase);assertTrue(j.length()<=(rsu?1.1:3.81));assertTrue(j.alpha()>0&&j.alpha()<=255);assertTrue(j.coneAlpha()>0&&j.coneAlpha()<j.alpha());assertTrue(j.halfWidth()<(rsu?.1:.25));}}
    @Test void smoothAnimationDoesNotChangeMinimumGasLength(){var a=ExhaustMath.jet(1000,15,true,0);var b=ExhaustMath.jet(1000,15,true,Math.PI/2);assertNotEquals(a.activeLength(),b.activeLength());assertEquals(a.coneLength(),b.coneLength());assertTrue(a.length()<ExhaustMath.jet(10000,15,false,0).length());}
}

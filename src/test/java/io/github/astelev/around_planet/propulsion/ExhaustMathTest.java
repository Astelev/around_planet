package io.github.astelev.around_planet.propulsion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExhaustMathTest {
    @Test void unpoweredZeroThrustAndInvalidValuesDoNotRender(){assertFalse(ExhaustMath.jet(10000,0,false,0).visible());assertFalse(ExhaustMath.jet(0,15,false,0).visible());assertFalse(ExhaustMath.jet(Double.NaN,15,false,0).visible());assertFalse(ExhaustMath.jet(10000,15,false,Double.NaN).visible());}
    @Test void activeSectionShrinksWithThrustAndSignal(){for(boolean rsu:new boolean[]{false,true}){var full=ExhaustMath.jet(10000,15,rsu,0);var low=ExhaustMath.jet(10000,1,rsu,0);assertTrue(full.activeLength()>low.activeLength());assertTrue(full.alpha()>low.alpha());assertTrue(ExhaustMath.jet(100,15,rsu,0).activeLength()<full.activeLength());}}
    @Test void expandingGasKeepsItsMinimumLengthEvenAtTinyPositiveThrust(){for(boolean rsu:new boolean[]{false,true})for(double phase:new double[]{0,Math.PI/2,Math.PI}){var j=ExhaustMath.jet(.0001,1,rsu,phase);assertEquals(ExhaustMath.minimumCone(rsu),j.coneLength());assertTrue(j.activeLength()<.001);assertTrue(j.visible());assertTrue(j.coneAlpha()>0);}}
    @Test void maximumEffectsRemainInsideTheirRenderBounds(){for(boolean rsu:new boolean[]{false,true})for(double phase:new double[]{0,Math.PI/2,Math.PI}){var j=ExhaustMath.jet(1e9,100,rsu,phase);assertTrue(j.length()<=(rsu?2.41:12.01));assertTrue(j.alpha()>0&&j.alpha()<=255);assertTrue(j.coneAlpha()>0&&j.coneAlpha()<j.alpha());assertTrue(j.halfWidth()<(rsu?.12:.25));}}
    @Test void smoothAnimationDoesNotChangeMinimumGasLength(){var a=ExhaustMath.jet(1000,15,true,0);var b=ExhaustMath.jet(1000,15,true,Math.PI/2);assertNotEquals(a.activeLength(),b.activeLength());assertEquals(a.coneLength(),b.coneLength());assertTrue(a.length()<ExhaustMath.jet(10000,15,false,0).length());}
    @Test void fullThrustIsSubstantiallyLargerThanThePreviousPlumes(){assertTrue(ExhaustMath.jet(10000,15,false,0).length()>5.8);assertTrue(ExhaustMath.jet(1000,15,true,0).length()>1.2);assertTrue(ExhaustMath.jet(10_000_000,15,false,0).length()>11.8);}

    @Test void engineExpansionHasNoSlopeBreakAtActiveLength(){
        for(double thrust:new double[]{.0001,100,10000,1e7}){
            var jet=ExhaustMath.jet(thrust,15,false,0);float y=jet.activeLength(),h=.0001f;
            assertEquals(jet.halfWidth()*1.5,ExhaustMath.engineRadius(jet,0),1e-6);
            assertEquals((ExhaustMath.engineRadius(jet,y)-ExhaustMath.engineRadius(jet,y-h))/h,
                (ExhaustMath.engineRadius(jet,y+h)-ExhaustMath.engineRadius(jet,y))/h,.004);
            for(int k=0;k<=100;k++){float r=ExhaustMath.engineRadius(jet,jet.length()*k/100);assertTrue(Float.isFinite(r)&&r>0&&r<2);}
        }
    }

    @Test void rsuReachesMaximumExhaustAtFiveHundredNewtons(){assertEquals(ExhaustMath.jet(500,15,true,0),ExhaustMath.jet(1e7,15,true,0));assertTrue(ExhaustMath.jet(250,15,true,0).activeLength()<ExhaustMath.jet(500,15,true,0).activeLength());}

}

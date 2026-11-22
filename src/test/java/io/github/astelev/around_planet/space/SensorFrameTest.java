package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SensorFrameTest {
    private static final SpaceVector[] AXES={new SpaceVector(1,0,0),new SpaceVector(-1,0,0),new SpaceVector(0,1,0),new SpaceVector(0,-1,0),new SpaceVector(0,0,1),new SpaceVector(0,0,-1)};
    @Test void allSixMountsUseTheModelTopAsForward(){for(var normal:AXES){var f=SensorFrame.of(normal);assertEquals(new SpaceVector(0,0,1),f.instrument(normal));assertEquals(normal,f.model(new SpaceVector(0,1,0)));assertEquals(0,f.forward().dot(f.right()));assertEquals(1,f.up().length());assertEquals(0,f.up().subtract(f.right().cross(f.forward())).length(),1e-12);}}
    @Test void outputFollowsNoseDeviationOppositeTargetOnEveryMount(){for(var normal:AXES){var f=SensorFrame.of(normal);for(var side:new SpaceVector[]{f.right(),f.right().scale(-1),f.up(),f.up().scale(-1)}){var target=f.forward().subtract(side);assertEquals(15,f.power(target,side,45));assertEquals(0,f.power(target,side.scale(-1),45));assertEquals(0,f.power(target,f.forward(),45));assertEquals(0,f.power(target,f.forward().scale(-1),45));}}}
    @Test void alignedSensorHasNoSignalsAndDeadbandIsStable(){for(var normal:AXES){var f=SensorFrame.of(normal);for(var side:AXES)assertEquals(0,f.power(normal,side,45));assertEquals(0,f.power(normal.add(f.right().scale(.001)),f.right().scale(-1),45));}}
    @Test void allTwentyFourRollFramesKeepTheNoseAndControlAxesConsistent(){for(var forward:AXES){var base=SensorFrame.of(forward);for(int roll=0;roll<4;roll++){var f=base.rolled(roll);assertEquals(forward,f.forward());assertEquals(0,f.right().dot(f.up()),1e-12);assertEquals(0,f.up().subtract(f.right().cross(f.forward())).length(),1e-12);assertEquals(0,base.up().add(base.rolled(2).up()).length(),1e-12);for(var side:new SpaceVector[]{f.right(),f.up()})assertEquals(15,f.power(f.forward().subtract(side),side,45));assertEquals(0,f.model(new SpaceVector(.2,.7,.4)).subtract(f.right().scale(.2).add(f.forward().scale(.7)).add(f.up().scale(.4))).length(),1e-12);}}}

}
